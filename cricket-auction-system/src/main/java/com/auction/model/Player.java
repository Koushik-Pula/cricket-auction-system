package com.auction.model;

public class Player {
    private String name;
    private String role;
    private int basePrice;
    private int auctioned; // 0 = not auctioned, 1 = auctioned
    private String boughtBy; // team name who bought the player
    private int price; // final sold price

    public Player() {
        this.auctioned = 0;
        this.boughtBy = null;
        this.price = 0;
    }

    public Player(String name, String role, int basePrice) {
        this.name = name;
        this.role = role;
        this.basePrice = basePrice;
        this.auctioned = 0;
        this.boughtBy = null;
        this.price = 0;
    }

    // getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getBasePrice() { return basePrice; }
    public void setBasePrice(int basePrice) { this.basePrice = basePrice; }

    public int getAuctioned() { return auctioned; }
    public void setAuctioned(int auctioned) { this.auctioned = auctioned; }

    public String getBoughtBy() { return boughtBy; }
    public void setBoughtBy(String boughtBy) { this.boughtBy = boughtBy; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    @Override
    public String toString() {
        return name + " (" + role + ") - Base: " + basePrice;
    }
}
