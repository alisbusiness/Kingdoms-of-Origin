package com.example.kingdoms.item;

import com.example.kingdoms.KingdomsPlugin;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final RoyalLawBookItem ROYAL_LAW_BOOK = new RoyalLawBookItem(
        new Item.Settings().maxCount(1).rarity(Rarity.RARE)
    );

    private ModItems() {}

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(KingdomsPlugin.MOD_ID, "royal_law_book"), ROYAL_LAW_BOOK);
    }

    public static boolean isRoyalLawBook(ItemStack stack) {
        return stack.isOf(ROYAL_LAW_BOOK);
    }
}
