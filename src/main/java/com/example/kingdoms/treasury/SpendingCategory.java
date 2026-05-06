package com.example.kingdoms.treasury;

public enum SpendingCategory {
    PUBLIC_WORKS(true, 4, -6, -2, "Public works"),
    MILITARY(true, 1, -2, 1, "Military funding"),
    RELIEF(true, 5, -10, -3, "Food and welfare relief"),
    INFRASTRUCTURE(true, 3, -5, -1, "Infrastructure"),
    FESTIVAL(true, 2, -8, 0, "Public festival"),
    STABILIZATION(true, 6, -12, -4, "Emergency stabilization"),
    PALACE(false, -4, 5, 8, "Royal household"),
    SIPHON(false, -8, 12, 18, "Private extraction");

    private final boolean publicSpending;
    private final int legitimacyDelta;
    private final int unrestDelta;
    private final int heatDelta;
    private final String label;

    SpendingCategory(boolean publicSpending, int legitimacyDelta, int unrestDelta, int heatDelta, String label) {
        this.publicSpending = publicSpending;
        this.legitimacyDelta = legitimacyDelta;
        this.unrestDelta = unrestDelta;
        this.heatDelta = heatDelta;
        this.label = label;
    }

    public boolean publicSpending() { return publicSpending; }
    public int legitimacyDelta() { return legitimacyDelta; }
    public int unrestDelta() { return unrestDelta; }
    public int heatDelta() { return heatDelta; }
    public String label() { return label; }
}
