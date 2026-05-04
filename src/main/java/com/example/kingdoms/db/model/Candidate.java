package com.example.kingdoms.db.model;

public final class Candidate {
    private long id;
    private long electionId;
    private String playerUuid;
    private String slogan;
    private long createdAt;

    public Candidate() {}

    public Candidate(long id, long electionId, String playerUuid, String slogan, long createdAt) {
        this.id = id;
        this.electionId = electionId;
        this.playerUuid = playerUuid;
        this.slogan = slogan;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getElectionId() { return electionId; }
    public void setElectionId(long electionId) { this.electionId = electionId; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
