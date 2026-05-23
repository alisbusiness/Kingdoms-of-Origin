package com.example.kingdoms.db.model;

public final class Law {
    private final long id;
    private final int position;
    private final String text;
    private final String createdByUuid;
    private final long createdAt;

    public Law(long id, int position, String text, String createdByUuid, long createdAt) {
        this.id = id;
        this.position = position;
        this.text = text;
        this.createdByUuid = createdByUuid;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }

    public String getCreatedByUuid() {
        return createdByUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
