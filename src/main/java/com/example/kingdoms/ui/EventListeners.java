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

    public static final String[] ALL_PERKS = {
        "acrobat", "apple_picker", "aqua_affinity", "arachnid_bane", "blessed_realm",
        "bountiful_harvest_i", "butchers_blade", "censorship", "creeper_resistance", "curfew",
        "current_rider", "deep_dark_resistance", "deep_sea_diver", "diamond_luck", "disarmed_populace",
        "dolphins_grace", "early_bird", "enchanters_wisdom", "enderman_slayer", "fishers_diet",
        "forced_labor_i", "forced_labor_ii", "fragile_armor", "frail_subjects", "green_thumb",
        "guardian_slayer", "heavy_taxes_i", "heavy_taxes_ii", "hero_of_the_realm_i", "hero_of_the_realm_ii",
        "honey_lover", "iron_skin", "lava_immunity", "lumberjack", "master_angler_i",
        "miners_haste_i", "miners_haste_ii", "miners_haste_iii", "mountaineer", "mushroom_forager",
        "night_owl", "obsidian_breaker", "ocean_treasure", "oppression_i", "oppression_ii",
        "ore_doubling", "royal_guard_i", "royal_guard_ii", "royal_vitality_i", "royal_vitality_ii",
        "seas_bounty_i", "shepherd", "slowed_masses", "spelunkers_glow", "squids_ink",
        "starvation_diet_i", "starvation_diet_ii", "stout_heart", "swift_messenger", "swift_striker",
        "tractor", "unbreaking_tools", "undead_slayer", "unlucky_realm", "vegans_grace"
    };

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

            // 3.5 Interim King logic: if no ruler and this is the first player ever to join
            if (rulerUuid == null) {
                try {
                    if (plugin.getPersistence().players().findAll().size() <= 1) {
                        officeService.assignRuler(player.getUuid());
                        player.sendMessage(net.minecraft.text.Text.literal("You are the interim King! Run /kingdom start-election to begin elections.").formatted(net.minecraft.util.Formatting.GOLD), false);
                    }
                } catch (SQLException e) {
                    LOGGER.error("Failed to assign interim king", e);
                }
            }

            // 4. Clean up all old perks
            for (String perk : ALL_PERKS) {
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), 
                    "power revoke " + player.getName().getString() + " kingdoms_of_origin:perks/" + perk);
            }

            // 5. Grant active perks
            String activePerks = officeService.getActivePerks();
            if (activePerks != null && !activePerks.isBlank()) {
                for (String perk : activePerks.split("[,\\s]+")) {
                    if (perk.isBlank()) continue;
                    String powerId = perk.contains(":") ? perk : "kingdoms_of_origin:perks/" + perk;
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), 
                        "power grant " + player.getName().getString() + " " + powerId);
                }
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
