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
    private static final List<Player> players = new ArrayList<>();
    private static final Map<String, ClientHandler> teams = new ConcurrentHashMap<>();
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final int MAX_TEAMS = 5;

    private static volatile Player currentPlayer = null;
    private static volatile String highestBidder = null;
    private static volatile int highestBid = 0;
    private static volatile boolean auctionActive = false;

    public static void main(String[] args) throws IOException {
        MongoDBUtil.connect();
        loadPlayers();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[SERVER] Auction server started on port " + PORT);

        // Accept clients
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

        // Start auction flow
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
            broadcast("[SERVER] No players available yet. Auction will begin once players are added.");
        }
    }

    private static void runAuction() {
        MongoDatabase db = MongoDBUtil.getDatabase();
        MongoCollection<Document> teamsCollection = db.getCollection("teams");

        for (Player player : players) {
            currentPlayer = player;
            highestBidder = null;
            highestBid = player.getBasePrice();

            broadcast("\n[AUCTION] Next player: " + player.getName() + " (" + player.getRole() + ") - Base: " + player.getBasePrice());
            broadcast("[AUCTION] Type 'yes' to participate in this auction.");

            waitForAllYes(teamsCollection);

            // Skip if no team can afford this player
            boolean anyTeamCanBid = false;
            for (Map.Entry<String, ClientHandler> entry : teams.entrySet()) {
                Document teamDoc = teamsCollection.find(Filters.eq("teamName", entry.getKey())).first();
                if (teamDoc.getInteger("budget") >= currentPlayer.getBasePrice()) {
                    anyTeamCanBid = true;
                    break;
                }
            }
            if (!anyTeamCanBid) {
                broadcast("[AUCTION] No team can afford " + currentPlayer.getName() + ". Skipping this player.");
                continue;
            }

            auctionActive = true;
            broadcast("[AUCTION] Bidding started! Base price: " + currentPlayer.getBasePrice());
            startBiddingTimer();

            if (highestBidder != null) {
                broadcast("[RESULT] " + currentPlayer.getName() + " SOLD to " + highestBidder + " for " + highestBid);

                // Update winner's budget in DB
                Document winnerDoc = teamsCollection.find(Filters.eq("teamName", highestBidder)).first();
                int newBudget = winnerDoc.getInteger("budget") - highestBid;
                teamsCollection.updateOne(Filters.eq("teamName", highestBidder), Updates.set("budget", newBudget));

                // Add player to winner's list
                List<Document> playerList = (List<Document>) winnerDoc.get("players");
                if (playerList == null) playerList = new ArrayList<>();
                Document playerDoc = new Document("name", currentPlayer.getName())
                        .append("role", currentPlayer.getRole())
                        .append("price", highestBid);
                playerList.add(playerDoc);
                teamsCollection.updateOne(Filters.eq("teamName", highestBidder), Updates.set("players", playerList));

                // Update player in players collection
                updatePlayerInDB(currentPlayer, highestBidder, highestBid);
            } else {
                broadcast("[RESULT] " + currentPlayer.getName() + " remains UNSOLD.");
            }
        }
        broadcast("\n[AUCTION] All players auctioned. Thank you!");
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
                    int budget = teamDoc.getInteger("budget");

                    if (budget >= currentPlayer.getBasePrice()) {
                        if (entry.getValue().isReady()) readyTeams.add(entry.getKey());
                    } else {
                        entry.getValue().send("[SERVER] You cannot participate for " + currentPlayer.getName() + " (budget too low)");
                    }
                }
            }
            if (readyTeams.size() == teams.size() && !teams.isEmpty()) break;
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        // Reset readiness
        for (ClientHandler handler : teams.values()) {
            handler.setReady(false);
        }
    }

    private static void startBiddingTimer() {
        long endTime = System.currentTimeMillis() + 30000; // 30 sec
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

    // ------------------ ClientHandler ------------------
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

                out.println("[SERVER] Enter your team name:");
                teamName = in.readLine();

                // Limit total number of teams
                long currentTeams = teamsCollection.countDocuments();
                if (currentTeams >= MAX_TEAMS) {
                    out.println("[SERVER] Registration full. Max " + MAX_TEAMS + " teams allowed.");
                    socket.close();
                    return;
                }

                // Check if team exists in DB
                Document existingTeam = teamsCollection.find(Filters.eq("teamName", teamName)).first();
                if (existingTeam == null) {
                    out.println("[SERVER] New team! Enter owner's name:");
                    String owner = in.readLine();
                    out.println("[SERVER] Enter city:");
                    String city = in.readLine();
                    out.println("[SERVER] Enter budget:");
                    int budget = Integer.parseInt(in.readLine());

                    Document teamDoc = new Document("teamName", teamName)
                            .append("ownerName", owner)
                            .append("city", city)
                            .append("budget", budget)
                            .append("players", new ArrayList<>());
                    teamsCollection.insertOne(teamDoc);

                    out.println("[SERVER] Registration complete! Welcome " + teamName);
                } else {
                    out.println("[SERVER] Welcome back " + teamName + "!");
                }

                // Add to current session teams map
                synchronized (teams) {
                    teams.put(teamName, this);
                }

                out.println("[SERVER] Type 'yes' to join auction, 'bid <amount>' to bid.");

                String msg;
                while ((msg = in.readLine()) != null) {
                    handleMessage(msg.trim(), teamsCollection);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (teamName != null) {
                    teams.remove(teamName);
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleMessage(String msg, MongoCollection<Document> teamsCollection) {
            if (msg.equalsIgnoreCase("yes")) {
                ready = true;
                send("[SERVER] You are ready for this auction.");
            } else if (msg.startsWith("bid")) {
                if (!auctionActive || currentPlayer == null) {
                    send("[SERVER] No active auction right now.");
                    return;
                }
                try {
                    int bidAmount = Integer.parseInt(msg.split(" ")[1]);

                    Document teamDoc = teamsCollection.find(Filters.eq("teamName", teamName)).first();
                    int budget = teamDoc.getInteger("budget");

                    if (bidAmount > budget) {
                        send("[SERVER] Your bid exceeds your remaining budget (" + budget + ")");
                        return;
                    }

                    if (bidAmount > highestBid) {
                        highestBid = bidAmount;
                        highestBidder = teamName;
                        broadcast("[BID] " + teamName + " bids " + bidAmount + " for " + currentPlayer.getName());
                    } else {
                        send("[SERVER] Your bid must be higher than current highest (" + highestBid + ")");
                    }
                } catch (Exception e) {
                    send("[SERVER] Invalid bid format. Use: bid <amount>");
                }
            } else {
                send("[SERVER] Unknown command.");
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
