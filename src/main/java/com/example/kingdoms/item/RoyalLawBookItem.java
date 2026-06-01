package com.example.kingdoms.item;

import com.example.kingdoms.db.model.Law;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class RoyalLawBookItem {
    private static final String LAW_BOOK_MARKER = "KingdomRoyalLawBook";
    private static final String LAW_VERSION = "KingdomLawVersion";
    private static final int LAWS_PER_PAGE = 8;

    private RoyalLawBookItem() {}

    public static ItemStack createStack(long version, List<Law> laws) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        writeBookNbt(stack, version, laws);
        return stack;
    }

    private static void writeBookNbt(ItemStack stack, long version, List<Law> laws) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(LAW_BOOK_MARKER, true);
        nbt.putLong(LAW_VERSION, version);
        nbt.putString("title", "Royal Law Book");
        nbt.putString("author", "The Crown");
        nbt.putInt("generation", 0);
        nbt.putBoolean("resolved", true);
        nbt.put("pages", pages(laws));
    }

    private static NbtList pages(List<Law> laws) {
        NbtList pages = new NbtList();
        if (laws.isEmpty()) {
            pages.add(NbtString.of(Text.Serializer.toJson(Text.literal("Royal Law Book\n\nNo laws have been written yet."))));
            return pages;
        }

        List<String> current = new ArrayList<>();
        for (Law law : laws) {
            current.add(law.getPosition() + ". " + law.getText());
            if (current.size() >= LAWS_PER_PAGE) {
                pages.add(page(current));
                current.clear();
            }
        }
        if (!current.isEmpty()) pages.add(page(current));
        return pages;
    }

    private static NbtString page(List<String> lines) {
        return NbtString.of(Text.Serializer.toJson(Text.literal("Royal Law Book\n\n" + String.join("\n\n", lines))));
    }
}
