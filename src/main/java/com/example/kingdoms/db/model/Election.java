package com.example.kingdoms.db.model;

public final class Election {
    private long id;
    private String officeId;
    private String status;
    private long nominationOpensAt;
    private long votingOpensAt;
    private long votingClosesAt;
    private String winnerUuid;
    private long createdAt;

    public Election() {}

    public Election(long id, String officeId, String status, long nominationOpensAt,
                    long votingOpensAt, long votingClosesAt, String winnerUuid, long createdAt) {
        this.id = id;
        this.officeId = officeId;
        this.status = status;
        this.nominationOpensAt = nominationOpensAt;
        this.votingOpensAt = votingOpensAt;
        this.votingClosesAt = votingClosesAt;
        this.winnerUuid = winnerUuid;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getOfficeId() { return officeId; }
    public void setOfficeId(String officeId) { this.officeId = officeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getNominationOpensAt() { return nominationOpensAt; }
    public void setNominationOpensAt(long nominationOpensAt) { this.nominationOpensAt = nominationOpensAt; }

    public long getVotingOpensAt() { return votingOpensAt; }
    public void setVotingOpensAt(long votingOpensAt) { this.votingOpensAt = votingOpensAt; }

    public long getVotingClosesAt() { return votingClosesAt; }
    public void setVotingClosesAt(long votingClosesAt) { this.votingClosesAt = votingClosesAt; }

    public String getWinnerUuid() { return winnerUuid; }
    public void setWinnerUuid(String winnerUuid) { this.winnerUuid = winnerUuid; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
