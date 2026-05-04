package com.example.kingdoms.db.model;

public final class OfficeState {
    private String officeId;
    private String holderUuid;
    private String holderOriginBeforeOffice;
    private String activeKingOriginId;
    private long termStartedAt;
    private long termEndsAt;
    private String phase;
    private String activePerks;

    public OfficeState() {}

    public OfficeState(String officeId, String holderUuid, String holderOriginBeforeOffice,
                       String activeKingOriginId, long termStartedAt, long termEndsAt, String phase, String activePerks) {
        this.officeId = officeId;
        this.holderUuid = holderUuid;
        this.holderOriginBeforeOffice = holderOriginBeforeOffice;
        this.activeKingOriginId = activeKingOriginId;
        this.termStartedAt = termStartedAt;
        this.termEndsAt = termEndsAt;
        this.phase = phase;
        this.activePerks = activePerks;
    }

    public String getOfficeId() { return officeId; }
    public void setOfficeId(String officeId) { this.officeId = officeId; }

    public String getHolderUuid() { return holderUuid; }
    public void setHolderUuid(String holderUuid) { this.holderUuid = holderUuid; }

    public String getHolderOriginBeforeOffice() { return holderOriginBeforeOffice; }
    public void setHolderOriginBeforeOffice(String holderOriginBeforeOffice) { this.holderOriginBeforeOffice = holderOriginBeforeOffice; }

    public String getActiveKingOriginId() { return activeKingOriginId; }
    public void setActiveKingOriginId(String activeKingOriginId) { this.activeKingOriginId = activeKingOriginId; }

    public long getTermStartedAt() { return termStartedAt; }
    public void setTermStartedAt(long termStartedAt) { this.termStartedAt = termStartedAt; }

    public long getTermEndsAt() { return termEndsAt; }
    public void setTermEndsAt(long termEndsAt) { this.termEndsAt = termEndsAt; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getActivePerks() { return activePerks; }
    public void setActivePerks(String activePerks) { this.activePerks = activePerks; }
}
