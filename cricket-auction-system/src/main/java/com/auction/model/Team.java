package com.auction.model;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private String teamName;
    private String ownerName;
    private String city;
    private int budget;
    private List<Player> players; // players won in auction

    public Team() {
        players = new ArrayList<>();
    }

    public Team(String teamName, String ownerName, String city, int budget) {
        this.teamName = teamName;
        this.ownerName = ownerName;
        this.city = city;
        this.budget = budget;
        this.players = new ArrayList<>();
    }

    // getters and setters
    public String getTeamName() { return teamName; }
    public String getOwnerName() { return ownerName; }
    public String getCity() { return city; }
    public int getBudget() { return budget; }
    public List<Player> getPlayers() { return players; }

    public void setTeamName(String teamName) { this.teamName = teamName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setCity(String city) { this.city = city; }
    public void setBudget(int budget) { this.budget = budget; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public void addPlayer(Player player) {
        players.add(player);
    }
}
