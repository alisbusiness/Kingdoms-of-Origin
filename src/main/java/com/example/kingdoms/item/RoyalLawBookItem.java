package com.example.kingdoms.item;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.db.model.Law;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class RoyalLawBookItem extends WrittenBookItem {
    private static final String LAW_BOOK_MARKER = "KingdomRoyalLawBook";
    private static final String LAW_VERSION = "KingdomLawVersion";
    private static final int LAWS_PER_PAGE = 8;

    public RoyalLawBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            syncStack(stack);
            if (player.isSneaking() && KingdomsPlugin.getInstance().getLawService().isKing(serverPlayer)) {
                KingdomsPlugin.getInstance().getEventListeners().setPendingLawRewrite(serverPlayer.getUuid());
                serverPlayer.sendMessage(Text.literal("Write the royal law book in chat. Separate laws with |. Type cancel to stop.").formatted(Formatting.GOLD), false);
                return TypedActionResult.success(stack);
            }
        }
        return super.use(world, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof ServerPlayerEntity) {
            syncStack(stack);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("The kingdom's shared law book.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("King: sneak-use to rewrite.").formatted(Formatting.DARK_GRAY));
    }

    public ItemStack createStack() {
        ItemStack stack = new ItemStack(this);
        syncStack(stack);
        return stack;
    }

    public void syncStack(ItemStack stack) {
        try {
            long version = KingdomsPlugin.getInstance().getLawService().latestUpdatedAt();
            NbtCompound nbt = stack.getOrCreateNbt();
            if (nbt.getBoolean(LAW_BOOK_MARKER) && nbt.getLong(LAW_VERSION) == version && nbt.contains(PAGES_KEY)) {
                return;
            }
            writeBookNbt(stack, version, KingdomsPlugin.getInstance().getLawService().laws());
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Could not sync royal law book", e);
        }
    }

    private static void writeBookNbt(ItemStack stack, long version, List<Law> laws) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(LAW_BOOK_MARKER, true);
        nbt.putLong(LAW_VERSION, version);
        nbt.putString(TITLE_KEY, "Royal Law Book");
        nbt.putString(AUTHOR_KEY, "The Crown");
        nbt.putInt(GENERATION_KEY, 0);
        nbt.putBoolean(RESOLVED_KEY, true);
        nbt.put(PAGES_KEY, pages(laws));
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
