package com.example.kingdoms.election;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ElectionServiceSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void runsHappyPathElectionFlow() throws Exception {
        PersistenceService persistence = new PersistenceService(tempDir);
        persistence.open();
        try {
            ConfigLoader config = ConfigLoader.load(Path.of("src/main/resources/config.yml"));
            Clock clock = Clock.fixed(Instant.parse("2026-05-23T00:00:00Z"), ZoneOffset.UTC);
            ElectionService service = new ElectionService(persistence, config, clock);
            AtomicReference<ElectionResult> winner = new AtomicReference<>();
            service.addWinnerListener(winner::set);

            var election = service.startElection("king");
            assertEquals(ElectionPhase.NOMINATION.name(), election.getStatus());
            assertTrue(service.getCurrentElection("king").isPresent());

            var candidate = service.registerCandidate(election.getId(), "candidate-1", " Vote\nfor\tme ");
            assertEquals("Vote for me", candidate.getSlogan());
            assertThrows(ElectionException.class,
                () -> service.registerCandidate(election.getId(), "candidate-1", "duplicate"));

            service.advancePhase(election.getId());
            service.advancePhase(election.getId());
            service.castVote(election.getId(), "voter-1", "candidate-1", 60);
            assertThrows(ElectionException.class,
                () -> service.castVote(election.getId(), "voter-1", "candidate-1", 60));

            var completed = service.advancePhase(election.getId());

            assertEquals(ElectionPhase.COMPLETE.name(), completed.getStatus());
            assertEquals("candidate-1", completed.getWinnerUuid());
            assertEquals("candidate-1", winner.get().winnerUuid());
        } finally {
            persistence.close();
        }
    }
}
