package com.auction.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.Document;

import com.auction.db.MongoDBUtil;
import com.auction.model.Player;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public class AuctionServer {
    private static final int PORT = 12345;
    private static final int MAX_CONCURRENT_TEAMS = 10;
    private static final List<Player> players = new ArrayList<>();
    private static final Map<String, ClientHandler> teams = new ConcurrentHashMap<>();
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private static volatile Player currentPlayer = null;
    private static volatile String highestBidder = null;
    private static volatile int highestBid = 0;
    private static volatile boolean auctionActive = false;

    public static void main(String[] args) throws IOException {
        MongoDBUtil.connect();
        loadPlayers();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Auction server started on port " + PORT);
        System.out.println("Maximum concurrent teams allowed: " + MAX_CONCURRENT_TEAMS);

        pool.execute(() -> {
            try {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    pool.execute(handler);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        runAuction();
    }

    private static void loadPlayers() {
        MongoDatabase db = MongoDBUtil.getDatabase();
        MongoCollection<Document> playersCollection = db.getCollection("players");

        players.clear();
        for (Document doc : playersCollection.find(Filters.eq("auctioned", 0))) {
            Player p = new Player();
            p.setName(doc.getString("name"));
            p.setRole(doc.getString("role"));
            p.setBasePrice(doc.getInteger("basePrice"));
            players.add(p);
        }

        if (players.isEmpty()) {
            broadcast("No players available yet. Auction will begin once players are added.");
        }
    }

    private static void runAuction() {
        MongoDatabase db = MongoDBUtil.getDatabase();
        MongoCollection<Document> teamsCollection = db.getCollection("teams");

        for (Player player : players) {
            currentPlayer = player;
            highestBidder = null;
            highestBid = player.getBasePrice();

            broadcast("\n--- NEXT PLAYER: " + player.getName() + " (" + player.getRole() + ") ---");
            broadcast("Base Price: " + player.getBasePrice());
            broadcast("Type 'yes' to participate");

            waitForAllYes(teamsCollection);

            boolean anyTeamCanBid = false;
            for (Map.Entry<String, ClientHandler> entry : teams.entrySet()) {
                Document teamDoc = teamsCollection.find(Filters.eq("teamName", entry.getKey())).first();
                if (teamDoc != null && teamDoc.getInteger("budget") >= currentPlayer.getBasePrice()) {
                    anyTeamCanBid = true;
                    break;
                }
            }
            if (!anyTeamCanBid) {
                broadcast("No team can afford " + currentPlayer.getName() + ". Skipping this player.");
                continue;
            }

            auctionActive = true;
            broadcast("BIDDING STARTED! Current bid: " + currentPlayer.getBasePrice());
            startBiddingTimer();

            if (highestBidder != null) {
                broadcast("SOLD to " + highestBidder + " for " + highestBid);

                Document winnerDoc = teamsCollection.find(Filters.eq("teamName", highestBidder)).first();
                if (winnerDoc != null) {
                    int newBudget = winnerDoc.getInteger("budget") - highestBid;
                    teamsCollection.updateOne(Filters.eq("teamName", highestBidder), Updates.set("budget", newBudget));

                    List<Document> playerList = (List<Document>) winnerDoc.get("players");
                    if (playerList == null) playerList = new ArrayList<>();
                    Document playerDoc = new Document("name", currentPlayer.getName())
                            .append("role", currentPlayer.getRole())
                            .append("price", highestBid);
                    playerList.add(playerDoc);
                    teamsCollection.updateOne(Filters.eq("teamName", highestBidder), Updates.set("players", playerList));

                    updatePlayerInDB(currentPlayer, highestBidder, highestBid);
                    
                    // Notify winning team
                    if (teams.containsKey(highestBidder)) {
                        teams.get(highestBidder).send("ACQUIRED: " + currentPlayer.getName() + 
                            " (" + currentPlayer.getRole() + ") for " + highestBid);
                    }
                }
            } else {
                broadcast("UNSOLD - No bids received");
            }
        }
        broadcast("\nAuction completed. Thank you!");
        System.exit(0);
    }

    private static void updatePlayerInDB(Player player, String teamName, int price) {
        MongoCollection<Document> playersCollection = MongoDBUtil.getDatabase().getCollection("players");
        playersCollection.updateOne(
            Filters.eq("name", player.getName()),
            Updates.combine(
                Updates.set("auctioned", 1),
                Updates.set("boughtBy", teamName),
                Updates.set("price", price)
            )
        );
    }

    private static void waitForAllYes(MongoCollection<Document> teamsCollection) {
        Set<String> readyTeams = new HashSet<>();
        while (true) {
            synchronized (teams) {
                readyTeams.clear();
                for (Map.Entry<String, ClientHandler> entry : teams.entrySet()) {
                    Document teamDoc = teamsCollection.find(Filters.eq("teamName", entry.getKey())).first();
                    if (teamDoc != null) {
                        int budget = teamDoc.getInteger("budget");
                        if (budget >= currentPlayer.getBasePrice()) {
                            if (entry.getValue().isReady()) readyTeams.add(entry.getKey());
                        } else {
                            entry.getValue().send("Cannot participate for " + currentPlayer.getName() + " (budget too low)");
                        }
                    }
                }
            }
            if (readyTeams.size() == teams.size() && !teams.isEmpty()) break;
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        for (ClientHandler handler : teams.values()) {
            handler.setReady(false);
        }
    }

    private static void startBiddingTimer() {
        long endTime = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < endTime) {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        auctionActive = false;
    }

    private static void broadcast(String msg) {
        for (ClientHandler handler : teams.values()) {
            handler.send(msg);
        }
        System.out.println(msg);
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String teamName;
        private boolean ready = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                MongoDatabase db = MongoDBUtil.getDatabase();
                MongoCollection<Document> teamsCollection = db.getCollection("teams");

                synchronized (teams) {
                    if (teams.size() >= MAX_CONCURRENT_TEAMS) {
                        out.println("Auction session full. Maximum " + MAX_CONCURRENT_TEAMS + " teams allowed.");
                        socket.close();
                        return;
                    }
                }

                out.println("Enter your team name:");
                teamName = in.readLine();

                Document existingTeam = teamsCollection.find(Filters.eq("teamName", teamName)).first();
                
                if (existingTeam == null) {
                    out.println("New team detected! Enter owner's name:");
                    String owner = in.readLine();
                    out.println("Enter city:");
                    String city = in.readLine();
                    out.println("Enter budget:");
                    int budget = Integer.parseInt(in.readLine());

                    Document teamDoc = new Document("teamName", teamName)
                            .append("ownerName", owner)
                            .append("city", city)
                            .append("budget", budget)
                            .append("players", new ArrayList<>());
                    teamsCollection.insertOne(teamDoc);

                    out.println("Registration complete! Welcome, " + teamName);
                } else {
                    out.println("Welcome back, " + teamName + "!");
                    out.println("Your budget: " + existingTeam.getInteger("budget"));
                }

                synchronized (teams) {
                    teams.put(teamName, this);
                }

                out.println("Connected! Auction will begin shortly...");
                out.println("Commands: 'yes' to participate, 'bid <amount>' to bid");

                String msg;
                while ((msg = in.readLine()) != null) {
                    handleMessage(msg.trim(), teamsCollection);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + (teamName != null ? teamName : "Unknown"));
            } catch (NumberFormatException e) {
                out.println("Invalid number format. Please enter valid numbers for budget.");
            } finally {
                if (teamName != null) {
                    synchronized (teams) {
                        teams.remove(teamName);
                    }
                    System.out.println("Team disconnected: " + teamName);
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleMessage(String msg, MongoCollection<Document> teamsCollection) {
            if (msg.equalsIgnoreCase("yes")) {
                ready = true;
                send("Ready for this auction.");
            } else if (msg.startsWith("bid")) {
                if (!auctionActive || currentPlayer == null) {
                    send("No active auction right now.");
                    return;
                }
                try {
                    int bidAmount = Integer.parseInt(msg.split(" ")[1]);

                    Document teamDoc = teamsCollection.find(Filters.eq("teamName", teamName)).first();
                    if (teamDoc == null) {
                        send("Team data not found. Please reconnect.");
                        return;
                    }

                    int budget = teamDoc.getInteger("budget");

                    if (bidAmount > budget) {
                        send("Bid exceeds your remaining budget (" + budget + ")");
                        return;
                    }

                    if (bidAmount > highestBid) {
                        highestBid = bidAmount;
                        highestBidder = teamName;
                        broadcast("BID: " + teamName + " bids " + bidAmount + " for " + currentPlayer.getName());
                    } else {
                        send("Bid must be higher than current highest (" + highestBid + ")");
                    }
                } catch (Exception e) {
                    send("Invalid bid format. Use: bid <amount>");
                }
            } else {
                send("Unknown command.");
            }
        }

        public void send(String msg) {
            out.println(msg);
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean val) {
            ready = val;
        }
    }
}