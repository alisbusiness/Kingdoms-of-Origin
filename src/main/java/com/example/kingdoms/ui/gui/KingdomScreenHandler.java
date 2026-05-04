package com.example.kingdoms.ui.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.BiConsumer;

/**
 * A GUI screen handler backed by a SimpleInventory with an arbitrary click callback.
 * Renders as a standard chest (the client needs no custom code).
 * All slot interactions within the container are routed to the provided callback;
 * item movement in/out of the container is blocked.
 */
public final class KingdomScreenHandler extends GenericContainerScreenHandler {

    private final BiConsumer<Integer, PlayerEntity> clickHandler;

    public KingdomScreenHandler(
        int syncId,
        PlayerInventory playerInventory,
        Inventory inventory,
        int rows,
        BiConsumer<Integer, PlayerEntity> clickHandler
    ) {
        super(typeForRows(rows), syncId, playerInventory, inventory, rows);
        this.clickHandler = clickHandler;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < getInventory().size()) {
            // Route button clicks to the callback; ignore drags / drops.
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                clickHandler.accept(slotIndex, player);
            }
            return; // Block all item movement out of container
        }
        // Let the player interact with their own inventory normally.
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY; // Prevent shift-click transfers
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    private static ScreenHandlerType<?> typeForRows(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> ScreenHandlerType.GENERIC_9X3;
        };
    }
}
