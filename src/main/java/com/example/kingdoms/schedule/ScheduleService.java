package com.example.kingdoms.schedule;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.model.Election;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.election.ElectionPhase;
import com.example.kingdoms.election.ElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Schedules election phase transitions using a Java ScheduledExecutorService.
 *
 * A single-threaded executor is used rather than Fabric's tick scheduler because:
 *  1. Phase deadlines span days — tick counting would require large numbers and is
 *     fragile across restarts.
 *  2. Phase transitions are pure DB writes with no immediate Minecraft-side effects;
 *     they do not need to run on the server thread.
 *  3. A single-threaded executor serializes all scheduled tasks naturally.
 *
 * Any downstream Minecraft actions triggered by winner callbacks must dispatch
 * back to the server thread themselves.
 */
public final class ScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleService.class);

    private final ElectionService electionService;
    private final PersistenceService persistence;
    private final ConfigLoader config;
    private final Clock clock;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kingdoms-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final Map<Long, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();

    public ScheduleService(ElectionService electionService, PersistenceService persistence,
                           ConfigLoader config, Clock clock) {
        this.electionService = electionService;
        this.persistence = persistence;
        this.config = config;
        this.clock = clock;
    }

    // -------------------------------------------------------------------------
    // Scheduling
    // -------------------------------------------------------------------------

    /**
     * Schedules the next phase transition for the given election based on its
     * current phase and stored timestamps. Safe to call multiple times — cancels
     * any previously pending future for the same election ID.
     */
    public void scheduleNextTransition(Election election) {
        ElectionPhase phase = ElectionPhase.fromString(election.getStatus());
        long deadline = ElectionService.phaseDeadlineEpochMs(election, phase, config.office());
        if (deadline == Long.MAX_VALUE) return; // COMPLETE or IDLE — nothing to schedule

        long nowMs  = clock.instant().toEpochMilli();
        long delayMs = Math.max(0L, deadline - nowMs);
        long electionId = election.getId();

        cancelPending(electionId);
        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                Election updated = electionService.advancePhase(electionId);
                scheduleNextTransition(updated);
            } catch (ElectionException | SQLException e) {
                LOGGER.error("Phase transition failed for election {}: {}", electionId, e.getMessage(), e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);

        pending.put(electionId, future);
        LOGGER.info("Scheduled transition out of {} for election {} in {}s",
            phase, electionId, delayMs / 1000);
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    public void cancelElection(long electionId) throws SQLException {
        cancelPending(electionId);
        persistence.elections().findById(electionId).ifPresent(election -> {
            try {
                election.setStatus("CANCELLED");
                persistence.elections().update(election);
                LOGGER.info("Election {} cancelled", electionId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void forceAdvancePhase(long electionId) {
        cancelPending(electionId);
        executor.submit(() -> {
            try {
                Election updated = electionService.advancePhase(electionId);
                scheduleNextTransition(updated);
                LOGGER.info("Force-advanced election {} to phase {}", electionId, updated.getStatus());
            } catch (ElectionException | SQLException e) {
                LOGGER.error("Force advance failed for election {}: {}", electionId, e.getMessage(), e);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        LOGGER.info("ScheduleService executor shut down");
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void cancelPending(long electionId) {
        ScheduledFuture<?> existing = pending.remove(electionId);
        if (existing != null) existing.cancel(false);
    }
}
