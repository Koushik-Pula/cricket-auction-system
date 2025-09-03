package com.auction.model;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.auction.db.MongoDBUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class AuctionManager {
    private final List<Player> players;

    public AuctionManager() {
        this.players = new ArrayList<>();
        loadPlayersFromDB();
    }

    private void loadPlayersFromDB() {
        MongoDatabase db = MongoDBUtil.getDatabase();
        MongoCollection<Document> playersCollection = db.getCollection("players");

        players.clear();
        for (Document doc : playersCollection.find(Filters.eq("auctioned", 0))) {
            Player p = new Player();
            p.setName(doc.getString("name"));
            p.setRole(doc.getString("role"));
            p.setBasePrice(doc.getInteger("basePrice"));
            p.setAuctioned(doc.getInteger("auctioned"));
            p.setBoughtBy(doc.getString("boughtBy"));
            p.setPrice(doc.getInteger("price") != null ? doc.getInteger("price") : 0);
            players.add(p);
        }
    }

    public List<Player> getPlayers() {
        return players;
    }
}
