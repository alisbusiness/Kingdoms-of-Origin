package com.example.kingdoms.origin;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.db.model.History;
import com.example.kingdoms.db.model.OfficeState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.UUID;

public final class OriginTransferService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OriginTransferService.class);
    private static final long MS_PER_DAY = 86_400_000L;

    private final OriginAdapter adapter;
    private final PersistenceService persistence;
    private final ConfigLoader config;

    // Set after the server has started; required for player lookups and server-thread dispatch.
    private MinecraftServer server;

    public OriginTransferService(OriginAdapter adapter, PersistenceService persistence, ConfigLoader config) {
        this.adapter     = adapter;
        this.persistence = persistence;
        this.config      = config;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // -------------------------------------------------------------------------
    // Origin application
    // -------------------------------------------------------------------------

    /**
     * Applies the king origin/layer to {@code player} according to {@code config.originMode()}.
     * In "replace" mode the player's current origin is stored in {@code office_state} for later restore.
     * Must be called on the server main thread.
     */
    public void grantKingStatus(ServerPlayerEntity player) throws SQLException {
        if (adapter == null) return;

        String mode     = config.originMode().type();
        String officeId = config.office().id();
        String uuid     = player.getUuidAsString();

        if ("layer".equals(mode)) {
            adapter.assignOriginLayer(player,
                    config.originMode().kingLayerId(),
                    config.originMode().kingOriginId());
        } else {
            // "replace": snapshot current origin so it can be restored on revoke
            String previous = adapter.getCurrentOrigin(player);
            OfficeState state = loadOrCreate(officeId);
            state.setHolderOriginBeforeOffice(previous);
            state.setActiveKingOriginId(config.originMode().kingOriginId());
            persistence.officeStates().save(state);
            adapter.assignOrigin(player, config.originMode().kingOriginId());
        }

        if (config.debug().logOriginTransfers()) {
            LOGGER.info("King status granted to {} (mode={})", uuid, mode);
        }
    }

    /**
     * Removes the king origin/layer from {@code player}. In "replace" mode the origin stored at
     * grant time is restored. Must be called on the server main thread.
     */
    public void revokeKingStatus(ServerPlayerEntity player) throws SQLException {
        if (adapter == null) return;

        String mode     = config.originMode().type();
        String officeId = config.office().id();
        String uuid     = player.getUuidAsString();

        if ("layer".equals(mode)) {
            adapter.clearOriginLayer(player, config.originMode().kingLayerId());
        } else {
            OfficeState state = persistence.officeStates().findByOfficeId(officeId).orElse(null);
            String previous   = state != null ? state.getHolderOriginBeforeOffice() : null;
            if (previous != null) {
                adapter.assignOrigin(player, previous);
            } else {
                LOGGER.warn("No stored previous origin for {} — skipping restore in replace mode", uuid);
            }
            if (state != null) {
                state.setHolderOriginBeforeOffice(null);
                state.setActiveKingOriginId(null);
                persistence.officeStates().save(state);
            }
        }

        if (config.debug().logOriginTransfers()) {
            LOGGER.info("King status revoked from {} (mode={})", uuid, mode);
        }
    }

    // -------------------------------------------------------------------------
    // Office transfer orchestration
    // -------------------------------------------------------------------------

    /**
     * Full handoff: revoke old holder → grant new holder → give Orb of Origin to old holder
     * (if configured) → update {@code office_state} → append to history.
     *
     * <p>DB writes happen on the calling thread; all Minecraft API calls (origin assignment,
     * item giving) are dispatched to the server main thread via {@code server.execute()}.
     */
    public void transferOffice(UUID fromUUID, UUID toUUID) throws SQLException {
        if (server == null) {
            LOGGER.error("Server reference not set; cannot dispatch office transfer {}->{}", fromUUID, toUUID);
            return;
        }
        String officeId  = config.office().id();
        long   now       = System.currentTimeMillis();
        long   termEndsAt = now + (long) config.office().termDays() * MS_PER_DAY;

        OfficeState state = loadOrCreate(officeId);
        state.setHolderUuid(toUUID.toString());
        state.setActiveKingOriginId(config.originMode().kingOriginId());
        state.setTermStartedAt(now);
        state.setTermEndsAt(termEndsAt);
        persistence.officeStates().save(state);

        persistence.history().insert(new History(0, "office_transfer",
                fromUUID.toString(), toUUID.toString(),
                "{\"office\":\"" + officeId + "\",\"from\":\"" + fromUUID + "\",\"to\":\"" + toUUID + "\"}",
                now));

        if (config.debug().logOriginTransfers()) {
            LOGGER.info("Office {} transferring {} → {}", officeId, fromUUID, toUUID);
        }

        server.execute(() -> {
            ServerPlayerEntity oldHolder = server.getPlayerManager().getPlayer(fromUUID);
            ServerPlayerEntity newHolder = server.getPlayerManager().getPlayer(toUUID);

            if (oldHolder != null) {
                try {
                    revokeKingStatus(oldHolder);
                } catch (SQLException e) {
                    LOGGER.error("Failed to revoke king status from {}", fromUUID, e);
                }
                if (config.transition().giveOrbOnTransfer() && adapter != null) {
                    adapter.giveOrbOfOrigin(oldHolder, 1);
                }
            } else {
                LOGGER.warn("Old holder {} is offline; origin revoke deferred until login", fromUUID);
            }

            if (newHolder != null) {
                try {
                    grantKingStatus(newHolder);
                } catch (SQLException e) {
                    LOGGER.error("Failed to grant king status to {}", toUUID, e);
                }
            } else {
                LOGGER.warn("New holder {} is offline; origin grant deferred until login", toUUID);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Startup validation
    // -------------------------------------------------------------------------

    /**
     * Checks that the current officeholder actually has the configured king origin applied.
     * Re-applies and logs a warning if a mismatch is detected.
     * Called during server startup for the active officeholder.
     * Must be called on the server main thread.
     */
    public void validateSync(ServerPlayerEntity player) {
        if (adapter == null) return;

        String mode     = config.originMode().type();
        String expected = config.originMode().kingOriginId();
        String uuid     = player.getUuidAsString();

        String actual = "layer".equals(mode)
                ? adapter.getOriginOnLayer(player, config.originMode().kingLayerId())
                : adapter.getCurrentOrigin(player);

        if (!expected.equals(actual)) {
            String location = "layer".equals(mode) ? config.originMode().kingLayerId() : "main layer";
            LOGGER.warn("Origin sync mismatch for {} — expected '{}' on {}, found '{}'; re-applying",
                    uuid, expected, location, actual);
            if ("layer".equals(mode)) {
                adapter.assignOriginLayer(player, config.originMode().kingLayerId(), expected);
            } else {
                adapter.assignOrigin(player, expected);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /** Gives one Orb of Origin to the player if the Origins adapter is present. */
    public void giveOriginOrb(ServerPlayerEntity player) {
        if (adapter == null) {
            LOGGER.warn("giveOriginOrb called but Origins adapter is not loaded; skipping");
            return;
        }
        adapter.giveOrbOfOrigin(player, 1);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OfficeState loadOrCreate(String officeId) throws SQLException {
        return persistence.officeStates().findByOfficeId(officeId)
                .orElseGet(() -> new OfficeState(officeId, null, null, null, 0L, 0L, null, null));
    }
}
