package com.example.kingdoms.db.model;

public final class Vote {
    private long id;
    private long electionId;
    private String voterUuid;
    private String candidateUuid;
    private long createdAt;

    public Vote() {}

    public Vote(long id, long electionId, String voterUuid, String candidateUuid, long createdAt) {
        this.id = id;
        this.electionId = electionId;
        this.voterUuid = voterUuid;
        this.candidateUuid = candidateUuid;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getElectionId() { return electionId; }
    public void setElectionId(long electionId) { this.electionId = electionId; }

    public String getVoterUuid() { return voterUuid; }
    public void setVoterUuid(String voterUuid) { this.voterUuid = voterUuid; }

    public String getCandidateUuid() { return candidateUuid; }
    public void setCandidateUuid(String candidateUuid) { this.candidateUuid = candidateUuid; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
