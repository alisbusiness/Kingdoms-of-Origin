package com.example.kingdoms.db;

import com.example.kingdoms.db.model.Candidate;
import com.example.kingdoms.db.model.Election;
import com.example.kingdoms.db.model.History;
import com.example.kingdoms.db.model.OfficeState;
import com.example.kingdoms.db.model.Player;
import com.example.kingdoms.db.model.Vote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PersistenceSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void opensSchemaAndExercisesRepositories() throws SQLException {
        PersistenceService persistence = new PersistenceService(tempDir);
        persistence.open();
        try {
            long now = 1_700_000_000_000L;

            Player player = new Player("player-1", "Ali", "origins:human", null, true, now, now);
            persistence.players().save(player);
            assertEquals("Ali", persistence.players().findByUuid("player-1").orElseThrow().getUsername());
            assertEquals(1, persistence.players().findAll().size());

            OfficeState officeState = new OfficeState("king", "player-1", "origins:human",
                "kingdoms_of_origin:king", now, now + 1, "NOMINATION", "iron_mandate");
            persistence.officeStates().save(officeState);
            assertEquals("player-1", persistence.officeStates().findByOfficeId("king").orElseThrow().getHolderUuid());

            Election election = new Election(0, "king", "NOMINATION", now, now + 10, now + 20, null, now);
            long electionId = persistence.elections().insert(election);
            assertTrue(electionId > 0);
            assertTrue(persistence.elections().findCurrentByOfficeId("king").isPresent());

            Candidate candidate = new Candidate(0, electionId, "player-2", "A clean slogan", "iron_mandate", now);
            long candidateId = persistence.candidates().insert(candidate);
            persistence.candidates().updatePromises(candidateId, "stone_covenant");
            assertEquals("stone_covenant",
                persistence.candidates().findByElectionAndPlayer(electionId, "player-2").orElseThrow().getPromisedPerks());

            Vote vote = new Vote(0, electionId, "voter-1", "player-2", now);
            long voteId = persistence.votes().insert(vote);
            assertTrue(voteId > 0);
            assertEquals("player-2",
                persistence.votes().findByElectionAndVoter(electionId, "voter-1").orElseThrow().getCandidateUuid());

            long historyId = persistence.history().insert(new History(0, "SMOKE", "player-1", "player-2", "{}", now));
            assertTrue(historyId > 0);
            assertEquals(1, persistence.history().findByActorUuid("player-1").size());
            assertEquals(1, persistence.history().findByEventType("SMOKE").size());
            assertEquals(1, persistence.history().findRecent(5).size());

            assertEquals(50, persistence.trust().getScore("player-1"));
            persistence.trust().adjust("player-1", 60);
            assertEquals(100, persistence.trust().getScore("player-1"));
            persistence.trust().addHistory("player-1", electionId, "iron_mandate", "iron_mandate", true);
            assertTrue(persistence.trust().recentHistory("player-1", 5).contains("Honored iron_mandate"));

            assertEquals(70, persistence.treasury().get("king").legitimacy());
            persistence.treasury().adjustBalance("player-1", 12);
            persistence.treasury().adjustBalance("player-1", -20);
            assertEquals(0, persistence.treasury().balance("player-1"));
            persistence.treasury().joinSide("player-1", "revolt");
            assertEquals("revolt", persistence.treasury().side("player-1"));
            assertEquals(1, persistence.treasury().sideCount("revolt"));
            persistence.treasury().ledger("king", "player-1", "MINT", 3, true, "smoke");
            assertEquals(List.of("MINT 3 - smoke"), persistence.treasury().recentPublicLedger("king", 5));
            persistence.treasury().clearParticipants();
            assertEquals(0, persistence.treasury().sideCount("revolt"));

            long firstLawId = persistence.laws().add("The first law", "player-1");
            assertTrue(firstLawId > 0);
            persistence.laws().add("The second law", "player-1");
            assertEquals(2, persistence.laws().findAll().size());
            assertTrue(persistence.laws().removeByPosition(1));
            assertEquals(1, persistence.laws().findAll().get(0).getPosition());
            persistence.laws().replaceAll(List.of("A", "B", "C"), "player-1");
            assertEquals(3, persistence.laws().findAll().size());
            assertTrue(persistence.laws().latestUpdatedAt() > 0);

            election.setId(electionId);
            election.setStatus("COMPLETE");
            election.setWinnerUuid("player-2");
            persistence.elections().update(election);
            assertEquals("player-2",
                persistence.elections().findMostRecentCompletedByOfficeId("king").orElseThrow().getWinnerUuid());

            persistence.votes().deleteByElectionId(electionId);
            assertTrue(persistence.votes().findByElectionId(electionId).isEmpty());
            persistence.candidates().delete(candidateId);
            assertTrue(persistence.candidates().findByElectionId(electionId).isEmpty());
            persistence.officeStates().delete("king");
            assertFalse(persistence.officeStates().findByOfficeId("king").isPresent());
            persistence.players().delete("player-1");
            assertFalse(persistence.players().findByUuid("player-1").isPresent());

            assertNotNull(tempDir.resolve("kingdoms.db"));
        } finally {
            persistence.close();
        }
    }
}
