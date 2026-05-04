package com.example.kingdoms.ui;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Permission node names and predicate helpers.
 * Uses OP-level fallback since fabric-permissions-api is not a dependency.
 * OP level 2 = admin, level 0 = any player.
 */
public final class Permissions {

    private Permissions() {}

    // Node names (for documentation / future permissions-api integration)
    public static final String PLAYER        = "kingdom.player";
    public static final String VOTE          = "kingdom.vote";
    public static final String RUN           = "kingdom.run";
    public static final String STATUS        = "kingdom.status";

    public static final String ADMIN         = "kingdom.admin";
    public static final String ADMIN_ELECTION = "kingdom.admin.election";
    public static final String ADMIN_TRANSFER = "kingdom.admin.transfer";
    public static final String ADMIN_ORIGIN  = "kingdom.admin.origin";
    public static final String RULER_PANEL   = "kingdom.ruler.panel";

    // ------------------------------------------------------------------
    // Predicates for Brigadier .requires()
    // ------------------------------------------------------------------

    /** Any player (not command blocks / console). */
    public static boolean isPlayer(ServerCommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity;
    }

    /** OP level 2 or higher. */
    public static boolean isAdmin(ServerCommandSource source) {
        return source.hasPermissionLevel(2);
    }
}
