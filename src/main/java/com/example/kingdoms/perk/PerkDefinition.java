package com.example.kingdoms.perk;

import net.minecraft.item.Item;

public record PerkDefinition(
    String id,
    String name,
    PerkCategory category,
    PerkKind kind,
    Item icon,
    String description,
    boolean wartime
) {
    public int cost() {
        return kind.cost();
    }
}
