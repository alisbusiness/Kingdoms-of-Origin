package com.example.kingdoms.ui;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.origin.OfficeService;
import com.example.kingdoms.origin.PlayerStateService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Registers Fabric event callbacks for player join, server tick, chat interception,
 * and a death hook stub.
 */
public final class EventListeners {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventListeners.class);

    private final KingdomsPlugin plugin;
    private final OfficeService officeService;
    private final AnnouncementService announcementService;
    private final PlayerStateService playerStateService;

    private final Set<UUID> pendingSpeech =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private int tickCounter = 0;
    private static final int TICKS_PER_MINUTE = 20 * 60;

    public EventListeners(
        KingdomsPlugin plugin,
        OfficeService officeService,
        AnnouncementService announcementService,
        PlayerStateService playerStateService
    ) {
        this.plugin              = plugin;
        this.officeService       = officeService;
        this.announcementService = announcementService;
        this.playerStateService  = playerStateService;
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    public void register() {
        registerJoinListener();
        registerTickListener();
        registerChatListener();
        registerDeathHookStub();
    }

    // ------------------------------------------------------------------
    // Speech prompt API (called by GuiService)
    // ------------------------------------------------------------------

    public void setPendingSpeech(UUID playerUuid) {
        pendingSpeech.add(playerUuid);
    }

    public void clearPendingSpeech(UUID playerUuid) {
        pendingSpeech.remove(playerUuid);
    }

    // ------------------------------------------------------------------
    // Join listener (consolidated — replaces duplicate in PlayerStateService)
    // ------------------------------------------------------------------

    private void registerJoinListener() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // 1. First-join random origin assignment and player record upsert
            playerStateService.handleJoin(player);

            // 2. Give the boss bar to the newly joined player if an election is running
            announcementService.addPlayerToBossBar(player);

            // 3. Validate king origin sync for the ruler on login
            String rulerUuid = officeService.getRuler();
            if (rulerUuid != null && rulerUuid.equals(player.getUuidAsString())) {
                plugin.getTransferService().validateSync(player);
            }
        });
    }

    // ------------------------------------------------------------------
    // Tick listener — term expiration
    // ------------------------------------------------------------------

    private void registerTickListener() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < TICKS_PER_MINUTE) return;
            tickCounter = 0;
            checkTermExpiration(server);
        });
    }

    private void checkTermExpiration(MinecraftServer server) {
        long   now       = System.currentTimeMillis();
        long   termEnd   = officeService.getTermEnd();
        String rulerUuid = officeService.getRuler();

        if (rulerUuid == null || termEnd == 0) return;
        if (now < termEnd) return;

        LOGGER.info("Term expired for ruler {}; removing from office.", rulerUuid);
        try {
            String rulerName = resolvePlayerName(server, rulerUuid);
            officeService.removeRuler(OfficeService.RemovalReason.TERM_END);
            announcementService.broadcastRulerChange(rulerName, null);

            // Start a new election if elections are enabled
            if (plugin.getConfig().office().electionEnabled()) {
                plugin.startElectionAndSchedule(plugin.getConfig().office().id());
            }
        } catch (ElectionException e) {
            LOGGER.error("Failed to start new election after term expiration: {}", e.getMessage(), e);
        } catch (SQLException e) {
            LOGGER.error("Failed to handle term expiration for ruler {}", rulerUuid, e);
        }
    }

    // ------------------------------------------------------------------
    // Chat listener — royal speech interception
    // ------------------------------------------------------------------

    private void registerChatListener() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            UUID senderUuid = sender.getUuid();
            if (!pendingSpeech.remove(senderUuid)) return;

            String rulerName = sender.getName().getString();
            String text      = message.getContent().getString();
            announcementService.broadcastSpeech(rulerName, text);
        });
    }

    // ------------------------------------------------------------------
    // Death hook stub
    // ------------------------------------------------------------------

    private void registerDeathHookStub() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (plugin.getConfig().debug().logGuiActions()) {
                    LOGGER.debug("Player {} died (source: {})", player.getUuidAsString(), source.getName());
                }
            }
            return true;
        });
    }

    // ------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------

    private String resolvePlayerName(MinecraftServer server, String uuid) {
        try {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
            if (online != null) return online.getName().getString();
            return plugin.getPersistence().players().findByUuid(uuid)
                .map(p -> p.getUsername() != null ? p.getUsername() : uuid)
                .orElse(uuid);
        } catch (Exception e) {
            return uuid;
        }
    }
}
