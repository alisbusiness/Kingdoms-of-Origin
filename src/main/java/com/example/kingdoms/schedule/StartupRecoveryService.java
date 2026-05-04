package com.example.kingdoms.schedule;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.db.model.Election;
import com.example.kingdoms.db.model.OfficeState;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.election.ElectionPhase;
import com.example.kingdoms.election.ElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;

public final class StartupRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupRecoveryService.class);

    private final PersistenceService persistence;
    private final ElectionService electionService;
    private final ScheduleService scheduleService;
    private final ConfigLoader config;
    private final Clock clock;

    public StartupRecoveryService(PersistenceService persistence, ElectionService electionService,
                                   ScheduleService scheduleService, ConfigLoader config, Clock clock) {
        this.persistence     = persistence;
        this.electionService = electionService;
        this.scheduleService = scheduleService;
        this.config          = config;
        this.clock           = clock;
    }

    public void recover() throws SQLException {
        String officeId = config.office().id();
        LOGGER.info("[Recovery] Starting recovery for office '{}'", officeId);

        recoverOfficeHolder(officeId);
        recoverElection(officeId);

        LOGGER.info("[Recovery] Recovery complete for office '{}'", officeId);
    }

    // -------------------------------------------------------------------------
    // Office holder
    // -------------------------------------------------------------------------

    private void recoverOfficeHolder(String officeId) throws SQLException {
        Optional<OfficeState> stateOpt = persistence.officeStates().findByOfficeId(officeId);
        if (stateOpt.isEmpty() || stateOpt.get().getHolderUuid() == null) {
            LOGGER.info("[Recovery] No current officeholder for '{}'", officeId);
            return;
        }

        OfficeState state = stateOpt.get();
        LOGGER.info("[Recovery] Office '{}' held by {} — origin re-application will be verified " +
                    "on world load (OfficeService not yet implemented)", officeId, state.getHolderUuid());
        // Origin re-application is scheduled via OfficeService once the server world is ready.
        // ScheduleService.executor is a daemon thread and cannot interact with Minecraft APIs directly.
    }

    // -------------------------------------------------------------------------
    // Election
    // -------------------------------------------------------------------------

    private void recoverElection(String officeId) throws SQLException {
        Optional<Election> electionOpt = persistence.elections().findCurrentByOfficeId(officeId);
        if (electionOpt.isEmpty()) {
            LOGGER.info("[Recovery] No active election for office '{}'", officeId);
            return;
        }

        Election election = electionOpt.get();
        ElectionPhase phase = ElectionPhase.fromString(election.getStatus());
        LOGGER.info("[Recovery] Found election {} in phase {} for office '{}'",
            election.getId(), phase, officeId);

        // Advance through any phases whose deadlines have already passed
        election = advancePastDeadlines(election);

        // If still ongoing, reschedule the next transition
        ElectionPhase recovered = ElectionPhase.fromString(election.getStatus());
        if (recovered != ElectionPhase.COMPLETE && recovered != ElectionPhase.IDLE) {
            scheduleService.scheduleNextTransition(election);
            LOGGER.info("[Recovery] Rescheduled transition for election {} (now in phase {})",
                election.getId(), recovered);
        } else {
            LOGGER.info("[Recovery] Election {} is already {}", election.getId(), recovered);
        }
    }

    private Election advancePastDeadlines(Election election) throws SQLException {
        long nowMs = clock.instant().toEpochMilli();

        while (true) {
            ElectionPhase phase = ElectionPhase.fromString(election.getStatus());
            long deadline = ElectionService.phaseDeadlineEpochMs(election, phase, config.office());
            if (deadline == Long.MAX_VALUE || nowMs < deadline) break;

            LOGGER.info("[Recovery] Deadline for phase {} has passed on election {} — advancing immediately",
                phase, election.getId());
            try {
                election = electionService.advancePhase(election.getId());
            } catch (ElectionException e) {
                LOGGER.warn("[Recovery] Could not advance election {}: {}", election.getId(), e.getMessage());
                break;
            }
        }
        return election;
    }
}
