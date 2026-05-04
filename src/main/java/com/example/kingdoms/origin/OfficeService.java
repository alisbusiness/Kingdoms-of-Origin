package com.example.kingdoms.origin;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.db.model.History;
import com.example.kingdoms.db.model.OfficeState;
import com.example.kingdoms.election.ElectionResult;
import com.example.kingdoms.election.ElectionService;
import com.example.kingdoms.map.MapIntegrationService;
import com.example.kingdoms.ui.AnnouncementService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Clock;
import java.util.UUID;

public final class OfficeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfficeService.class);
    private static final long MS_PER_DAY = 86_400_000L;

    public enum RemovalReason { ABDICATION, FORCED, TERM_END }

    private final PersistenceService persistence;
    private final ConfigLoader config;
    private final ElectionService electionService;
    private final OriginTransferService transferService;
    private final Clock clock;

    private OfficeState cachedState;
    private MinecraftServer server;
    private MapIntegrationService mapService;
    private AnnouncementService announcementService;

    public OfficeService(PersistenceService persistence, ConfigLoader config,
                         ElectionService electionService, OriginTransferService transferService,
                         Clock clock) {
        this.persistence      = persistence;
        this.config           = config;
        this.electionService  = electionService;
        this.transferService  = transferService;
        this.clock            = clock;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void init() throws SQLException {
        cachedState = persistence.officeStates()
                .findByOfficeId(config.office().id())
                .orElse(null);
        electionService.addWinnerListener(this::onElectionResult);
        LOGGER.info("OfficeService initialized; current holder: {}",
                cachedState != null ? cachedState.getHolderUuid() : "<none>");
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
        transferService.setServer(server);
    }

    public void setMapService(MapIntegrationService mapService) {
        this.mapService = mapService;
    }

    public void setAnnouncementService(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    public void onServerStarted() {
        if (cachedState == null || cachedState.getHolderUuid() == null) return;
        ServerPlayerEntity holder = server.getPlayerManager()
                .getPlayer(UUID.fromString(cachedState.getHolderUuid()));
        if (holder != null) {
            transferService.validateSync(holder);
        }
    }

    // -------------------------------------------------------------------------
    // Ruler management
    // -------------------------------------------------------------------------

    /**
     * Appoints a player as ruler. If a different ruler currently holds office,
     * routes through {@link OriginTransferService#transferOffice} so the old
     * holder is properly revoked and receives an orb per config.
     */
    public void assignRuler(UUID playerUUID) throws SQLException {
        String currentHolder = cachedState != null ? cachedState.getHolderUuid() : null;

        if (currentHolder != null && !currentHolder.equals(playerUUID.toString())) {
            // Transfer from current holder to new player
            transferService.transferOffice(UUID.fromString(currentHolder), playerUUID);
            cachedState = persistence.officeStates()
                    .findByOfficeId(config.office().id()).orElse(null);
            updateMap();
            return;
        }

        // Vacant office — direct appointment
        String officeId   = config.office().id();
        long   now        = clock.instant().toEpochMilli();
        long   termEndsAt = now + (long) config.office().termDays() * MS_PER_DAY;

        OfficeState state = persistence.officeStates().findByOfficeId(officeId)
                .orElseGet(() -> new OfficeState(officeId, null, null, null, 0L, 0L, null));
        state.setHolderUuid(playerUUID.toString());
        state.setActiveKingOriginId(config.originMode().kingOriginId());
        state.setTermStartedAt(now);
        state.setTermEndsAt(termEndsAt);
        persistence.officeStates().save(state);
        cachedState = state;

        persistence.history().insert(new History(0, "office_appointed",
                null, playerUUID.toString(),
                "{\"office\":\"" + officeId + "\"}",
                now));

        if (server != null) {
            server.execute(() -> {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUUID);
                if (player != null) {
                    try {
                        transferService.grantKingStatus(player);
                    } catch (SQLException e) {
                        LOGGER.error("Failed to grant king status to {}", playerUUID, e);
                    }
                } else {
                    LOGGER.warn("Player {} is offline; king origin will be applied on next login", playerUUID);
                }
            });
        }

        updateMap();
        LOGGER.info("Ruler assigned: {} (term ends {})", playerUUID, termEndsAt);
    }

    /**
     * Removes the current ruler. Revokes king status and gives Orb of Origin
     * according to the reason and config flags.
     */
    public void removeRuler(RemovalReason reason) throws SQLException {
        if (cachedState == null || cachedState.getHolderUuid() == null) return;

        UUID   holderUUID = UUID.fromString(cachedState.getHolderUuid());
        String officeId   = config.office().id();
        long   now        = clock.instant().toEpochMilli();

        boolean giveOrb = switch (reason) {
            case ABDICATION -> config.transition().giveOrbOnAbdication();
            case FORCED     -> config.transition().giveOrbOnForcedRemoval();
            case TERM_END   -> false; // orb is not granted when term naturally expires per spec
        };

        if (server != null) {
            boolean finalGiveOrb = giveOrb;
            server.execute(() -> {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(holderUUID);
                if (player != null) {
                    try {
                        transferService.revokeKingStatus(player);
                    } catch (SQLException e) {
                        LOGGER.error("Failed to revoke king status from {}", holderUUID, e);
                    }
                    if (finalGiveOrb) {
                        transferService.giveOriginOrb(player);
                    }
                } else {
                    LOGGER.warn("Outgoing ruler {} is offline; origin revoke deferred to next login",
                            holderUUID);
                }
            });
        }

        OfficeState state = persistence.officeStates().findByOfficeId(officeId)
                .orElseGet(() -> new OfficeState(officeId, null, null, null, 0L, 0L, null));
        state.setHolderUuid(null);
        state.setActiveKingOriginId(null);
        state.setHolderOriginBeforeOffice(null);
        persistence.officeStates().save(state);
        cachedState = state;

        persistence.history().insert(new History(0, "office_removed",
                holderUUID.toString(), null,
                "{\"office\":\"" + officeId + "\",\"reason\":\"" + reason.name() + "\"}",
                now));

        if (mapService != null) mapService.onRulerChanged("None");
        LOGGER.info("Ruler removed ({}); office {} is now vacant", reason, officeId);
    }

    /** Convenience overload for existing call sites that don't specify a reason. */
    public void removeRuler() throws SQLException {
        removeRuler(RemovalReason.FORCED);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public String getRuler() {
        return cachedState != null ? cachedState.getHolderUuid() : null;
    }

    public long getTermEnd() {
        return cachedState != null ? cachedState.getTermEndsAt() : 0L;
    }

    public String getPhase() {
        return cachedState != null ? cachedState.getPhase() : null;
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void updateMap() {
        if (mapService == null) return;
        String uuid = cachedState != null ? cachedState.getHolderUuid() : null;
        if (uuid == null) { mapService.onRulerChanged("None"); return; }
        mapService.onRulerChanged(resolveDisplayName(UUID.fromString(uuid)));
    }

    private String resolveDisplayName(UUID uuid) {
        if (server != null) {
            var player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) return player.getName().getString();
        }
        try {
            return persistence.players().findByUuid(uuid.toString())
                .map(p -> p.getUsername() != null ? p.getUsername() : uuid.toString())
                .orElse(uuid.toString());
        } catch (java.sql.SQLException e) {
            return uuid.toString();
        }
    }

    private void onElectionResult(ElectionResult result) {
        if (!result.officeId().equals(config.office().id())) return;

        String winnerUuid = result.winnerUuid();
        if (winnerUuid == null) {
            LOGGER.info("Election {} for office {} ended with no candidates; office unchanged",
                    result.electionId(), result.officeId());
            return;
        }

        UUID   winner        = UUID.fromString(winnerUuid);
        String currentHolder = cachedState != null ? cachedState.getHolderUuid() : null;
        String winnerName    = resolveDisplayName(winner);
        String oldName       = currentHolder != null ? resolveDisplayName(UUID.fromString(currentHolder)) : null;

        try {
            if (currentHolder != null) {
                transferService.transferOffice(UUID.fromString(currentHolder), winner);
            } else {
                assignRuler(winner);
            }
            cachedState = persistence.officeStates()
                    .findByOfficeId(config.office().id()).orElse(null);

            if (announcementService != null) {
                announcementService.broadcastWinner(winnerName);
                announcementService.broadcastRulerChange(oldName, winnerName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process election result {} for office {}",
                    result.electionId(), result.officeId(), e);
        }
    }
}
