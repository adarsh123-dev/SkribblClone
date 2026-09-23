package com.skribbl.model;

public class Room {
    private int id;
    private String roomCode;
    private String currentWord;
    private int currentDrawerId;
    private int roundNumber;
    private String status; // WAITING, PLAYING, ROUND_END

    public Room() {}

    public Room(int id, String roomCode, String currentWord, int currentDrawerId,
                int roundNumber, String status) {
        this.id = id;
        this.roomCode = roomCode;
        this.currentWord = currentWord;
        this.currentDrawerId = currentDrawerId;
        this.roundNumber = roundNumber;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getCurrentWord() { return currentWord; }
    public void setCurrentWord(String currentWord) { this.currentWord = currentWord; }

    public int getCurrentDrawerId() { return currentDrawerId; }
    public void setCurrentDrawerId(int currentDrawerId) { this.currentDrawerId = currentDrawerId; }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
