package com.example.kingdoms.origin;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.db.model.Player;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerStateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStateService.class);

    private final OriginAdapter adapter;
    private final PersistenceService persistence;
    private final ConfigLoader config;

    public PlayerStateService(OriginAdapter adapter, PersistenceService persistence, ConfigLoader config) {
        this.adapter     = adapter;
        this.persistence = persistence;
        this.config      = config;
    }

    /**
     * Called from the consolidated JOIN listener in {@code EventListeners}.
     * Upserts the player record and assigns a random origin on first join.
     */
    public void handleJoin(ServerPlayerEntity player) {
        String uuid     = player.getUuidAsString();
        String username = player.getName().getString();
        long   now      = System.currentTimeMillis();

        try {
            Player record = persistence.players().findByUuid(uuid).orElseGet(() -> {
                Player p = new Player();
                p.setUuid(uuid);
                p.setUsername(username);
                p.setCreatedAt(now);
                return p;
            });

            record.setUsername(username);
            record.setUpdatedAt(now);

            if (!record.isFirstJoinOriginAssigned()
                    && adapter != null
                    && config.randomOrigin().enabled()
                    && config.randomOrigin().assignOnFirstJoin()) {

                String chosen = pickRandomOrigin();
                if (chosen != null) {
                    try {
                        adapter.assignOrigin(player, chosen);
                        record.setCurrentOriginId(chosen);
                        record.setFirstJoinOriginAssigned(true);
                        LOGGER.info("Assigned random origin '{}' to new player {}", chosen, username);
                    } catch (Exception e) {
                        LOGGER.error("Failed to assign random origin '{}' to {}", chosen, uuid, e);
                    }
                }
            }

            persistence.players().save(record);
        } catch (SQLException e) {
            LOGGER.error("Failed to process player join for {}", uuid, e);
        }
    }

    private String pickRandomOrigin() {
        List<String> candidates = new ArrayList<>(config.randomOrigin().allowedOrigins());
        candidates.removeAll(config.randomOrigin().excludedOrigins());
        candidates.remove(config.originMode().kingOriginId());
        if (candidates.isEmpty()) {
            LOGGER.warn("No eligible origins remain after exclusions; skipping random assignment");
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
