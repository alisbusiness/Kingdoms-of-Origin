package com.example.kingdoms.treasury;

import net.minecraft.util.Formatting;

public enum ReserveHealth {
    FULLY_BACKED("Fully Backed", 1.00, 3, -3, Formatting.GREEN),
    STABLE("Stable", 0.75, 1, -1, Formatting.AQUA),
    STRAINED("Strained", 0.50, -2, 4, Formatting.YELLOW),
    DEBASED("Debased", 0.25, -5, 9, Formatting.RED),
    INSOLVENT("Insolvent", 0.00, -10, 16, Formatting.DARK_RED);

    private final String label;
    private final double threshold;
    private final int legitimacyDelta;
    private final int unrestDelta;
    private final Formatting color;

    ReserveHealth(String label, double threshold, int legitimacyDelta, int unrestDelta, Formatting color) {
        this.label = label;
        this.threshold = threshold;
        this.legitimacyDelta = legitimacyDelta;
        this.unrestDelta = unrestDelta;
        this.color = color;
    }

    public static ReserveHealth fromRatio(double ratio) {
        if (ratio >= FULLY_BACKED.threshold) return FULLY_BACKED;
        if (ratio >= STABLE.threshold) return STABLE;
        if (ratio >= STRAINED.threshold) return STRAINED;
        if (ratio >= DEBASED.threshold) return DEBASED;
        return INSOLVENT;
    }

    public String label() { return label; }
    public int legitimacyDelta() { return legitimacyDelta; }
    public int unrestDelta() { return unrestDelta; }
    public Formatting color() { return color; }
}
