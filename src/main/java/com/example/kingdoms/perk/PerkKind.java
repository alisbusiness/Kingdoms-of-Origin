package com.example.kingdoms.perk;

public enum PerkKind {
    MINOR(2, "Minor"),
    MODERATE(4, "Moderate"),
    STRONG(6, "Strong"),
    DEBUFF(-3, "Debuff"),
    CORRUPTION(0, "Corruption");

    private final int cost;
    private final String label;

    PerkKind(int cost, String label) {
        this.cost = cost;
        this.label = label;
    }

    public int cost() {
        return cost;
    }

    public String label() {
        return label;
    }
}
