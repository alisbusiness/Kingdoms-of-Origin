package com.example.kingdoms.treasury;

public final class TreasuryState {
    private final String officeId;
    private final int rawDiamonds;
    private final int diamondBlocks;
    private final int currencySupply;
    private final int taxesCollected;
    private final int publicSpending;
    private final int treasuryWithdrawals;
    private final int emergencyMinting;
    private final int xpTaxRate;
    private final int tradeTaxRate;
    private final int resourceTitheRate;
    private final int emergencyLevyRate;
    private final int legitimacy;
    private final int corruptionHeat;
    private final int unrest;
    private final boolean revoltActive;
    private final long revoltStartedAt;
    private final int captureProgress;
    private final long transitionFreezeUntil;
    private final long updatedAt;

    public TreasuryState(String officeId, int rawDiamonds, int diamondBlocks, int currencySupply,
                         int taxesCollected, int publicSpending, int treasuryWithdrawals,
                         int emergencyMinting, int xpTaxRate, int tradeTaxRate,
                         int resourceTitheRate, int emergencyLevyRate, int legitimacy,
                         int corruptionHeat, int unrest, boolean revoltActive,
                         long revoltStartedAt, int captureProgress, long transitionFreezeUntil,
                         long updatedAt) {
        this.officeId = officeId;
        this.rawDiamonds = rawDiamonds;
        this.diamondBlocks = diamondBlocks;
        this.currencySupply = currencySupply;
        this.taxesCollected = taxesCollected;
        this.publicSpending = publicSpending;
        this.treasuryWithdrawals = treasuryWithdrawals;
        this.emergencyMinting = emergencyMinting;
        this.xpTaxRate = xpTaxRate;
        this.tradeTaxRate = tradeTaxRate;
        this.resourceTitheRate = resourceTitheRate;
        this.emergencyLevyRate = emergencyLevyRate;
        this.legitimacy = legitimacy;
        this.corruptionHeat = corruptionHeat;
        this.unrest = unrest;
        this.revoltActive = revoltActive;
        this.revoltStartedAt = revoltStartedAt;
        this.captureProgress = captureProgress;
        this.transitionFreezeUntil = transitionFreezeUntil;
        this.updatedAt = updatedAt;
    }

    public static TreasuryState defaults(String officeId) {
        return new TreasuryState(officeId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 70, 0, 0, false, 0L, 0, 0L, System.currentTimeMillis());
    }

    public int reserveValue() { return rawDiamonds + diamondBlocks * 9; }
    public double reserveRatio() { return currencySupply <= 0 ? 1.0 : reserveValue() / (double) currencySupply; }
    public ReserveHealth reserveHealth() { return ReserveHealth.fromRatio(reserveRatio()); }
    public StabilityBand unrestBand() { return StabilityBand.fromUnrest(unrest); }

    public String officeId() { return officeId; }
    public int rawDiamonds() { return rawDiamonds; }
    public int diamondBlocks() { return diamondBlocks; }
    public int currencySupply() { return currencySupply; }
    public int taxesCollected() { return taxesCollected; }
    public int publicSpending() { return publicSpending; }
    public int treasuryWithdrawals() { return treasuryWithdrawals; }
    public int emergencyMinting() { return emergencyMinting; }
    public int xpTaxRate() { return xpTaxRate; }
    public int tradeTaxRate() { return tradeTaxRate; }
    public int resourceTitheRate() { return resourceTitheRate; }
    public int emergencyLevyRate() { return emergencyLevyRate; }
    public int legitimacy() { return legitimacy; }
    public int corruptionHeat() { return corruptionHeat; }
    public int unrest() { return unrest; }
    public boolean revoltActive() { return revoltActive; }
    public long revoltStartedAt() { return revoltStartedAt; }
    public int captureProgress() { return captureProgress; }
    public long transitionFreezeUntil() { return transitionFreezeUntil; }
    public long updatedAt() { return updatedAt; }
}
