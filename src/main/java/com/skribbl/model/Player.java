package com.skribbl.model;

public class Player {
    private int id;
    private String name;
    private int roomId;
    private int score;
    private boolean isDrawer;
    private boolean hasGuessed;

    public Player() {}

    public Player(int id, String name, int roomId, int score, boolean isDrawer, boolean hasGuessed) {
        this.id = id;
        this.name = name;
        this.roomId = roomId;
        this.score = score;
        this.isDrawer = isDrawer;
        this.hasGuessed = hasGuessed;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public boolean isDrawer() { return isDrawer; }
    public void setDrawer(boolean drawer) { isDrawer = drawer; }

    public boolean isHasGuessed() { return hasGuessed; }
    public void setHasGuessed(boolean hasGuessed) { this.hasGuessed = hasGuessed; }
}
