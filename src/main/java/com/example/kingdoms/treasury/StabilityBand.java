package com.example.kingdoms.treasury;

import net.minecraft.util.Formatting;

public enum StabilityBand {
    CALM("Calm", Formatting.GREEN),
    UNEASY("Uneasy", Formatting.YELLOW),
    UNREST("Unrest", Formatting.GOLD),
    CRISIS("Crisis", Formatting.RED),
    REVOLT_WINDOW("Revolt Window", Formatting.DARK_RED);

    private final String label;
    private final Formatting color;

    StabilityBand(String label, Formatting color) {
        this.label = label;
        this.color = color;
    }

    public static StabilityBand fromUnrest(int unrest) {
        if (unrest >= 80) return REVOLT_WINDOW;
        if (unrest >= 60) return CRISIS;
        if (unrest >= 40) return UNREST;
        if (unrest >= 20) return UNEASY;
        return CALM;
    }

    public String label() { return label; }
    public Formatting color() { return color; }
}
