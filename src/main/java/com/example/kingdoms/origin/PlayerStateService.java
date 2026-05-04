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

            if (!record.isFirstJoinOriginAssigned()) {
                record.setFirstJoinOriginAssigned(true);
            }

            persistence.players().save(record);
        } catch (SQLException e) {
            LOGGER.error("Failed to process player join for {}", uuid, e);
        }
    }

}
