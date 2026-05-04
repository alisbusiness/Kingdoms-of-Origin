package com.example.kingdoms.db.model;

public final class Player {
    private String uuid;
    private String username;
    private String currentOriginId;
    private String previousOriginId;
    private boolean firstJoinOriginAssigned;
    private long createdAt;
    private long updatedAt;

    public Player() {}

    public Player(String uuid, String username, String currentOriginId, String previousOriginId,
                  boolean firstJoinOriginAssigned, long createdAt, long updatedAt) {
        this.uuid = uuid;
        this.username = username;
        this.currentOriginId = currentOriginId;
        this.previousOriginId = previousOriginId;
        this.firstJoinOriginAssigned = firstJoinOriginAssigned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCurrentOriginId() { return currentOriginId; }
    public void setCurrentOriginId(String currentOriginId) { this.currentOriginId = currentOriginId; }

    public String getPreviousOriginId() { return previousOriginId; }
    public void setPreviousOriginId(String previousOriginId) { this.previousOriginId = previousOriginId; }

    public boolean isFirstJoinOriginAssigned() { return firstJoinOriginAssigned; }
    public void setFirstJoinOriginAssigned(boolean firstJoinOriginAssigned) { this.firstJoinOriginAssigned = firstJoinOriginAssigned; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
