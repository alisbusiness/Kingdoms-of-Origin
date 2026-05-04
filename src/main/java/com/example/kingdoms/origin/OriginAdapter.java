package com.example.kingdoms.origin;

import net.minecraft.server.network.ServerPlayerEntity;

public interface OriginAdapter {

    /** Returns the origin ID on the primary Origins layer (e.g., "server:avian"), or null if unset. */
    String getCurrentOrigin(ServerPlayerEntity player);

    /** Returns the origin ID assigned to a specific layer, or null if none is set. */
    String getOriginOnLayer(ServerPlayerEntity player, String layerId);

    /** Assigns an origin on the default Origins layer, replacing any previously assigned origin. */
    void assignOrigin(ServerPlayerEntity player, String originId);

    /** Assigns an origin on a specific named layer without affecting other layers. */
    void assignOriginLayer(ServerPlayerEntity player, String layerId, String originId);

    /** Resets a specific layer to the empty state, effectively removing any origin on that layer. */
    void clearOriginLayer(ServerPlayerEntity player, String layerId);

    /** Gives the player the Origins "Orb of Origin" item looked up from the item registry. */
    void giveOrbOfOrigin(ServerPlayerEntity player, int amount);
}
