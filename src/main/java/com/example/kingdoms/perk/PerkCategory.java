package com.example.kingdoms.perk;

public enum PerkCategory {
    LABOR("Labor"),
    MILITARY("Military"),
    ECONOMIC("Economic"),
    SOCIAL("Social"),
    CORRUPTION("Corruption");

    private final String displayName;

    PerkCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
