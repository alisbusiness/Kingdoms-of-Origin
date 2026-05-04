package com.example.kingdoms.db.model;

public final class History {
    private long id;
    private String eventType;
    private String actorUuid;
    private String targetUuid;
    private String payloadJson;
    private long createdAt;

    public History() {}

    public History(long id, String eventType, String actorUuid, String targetUuid,
                   String payloadJson, long createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.actorUuid = actorUuid;
        this.targetUuid = targetUuid;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getActorUuid() { return actorUuid; }
    public void setActorUuid(String actorUuid) { this.actorUuid = actorUuid; }

    public String getTargetUuid() { return targetUuid; }
    public void setTargetUuid(String targetUuid) { this.targetUuid = targetUuid; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
