package com.example.kingdoms.election;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.db.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ElectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectionService.class);
    private static final long MS_PER_DAY = 86_400_000L;

    private final PersistenceService persistence;
    private final ConfigLoader config;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Consumer<ElectionResult>> winnerListeners = new ArrayList<>();
    /** Fired on every phase transition: (election, newPhase). Runs on the scheduler thread. */
    private final List<BiConsumer<Election, ElectionPhase>> phaseListeners = new ArrayList<>();

    public ElectionService(PersistenceService persistence, ConfigLoader config, Clock clock) {
        this.persistence = persistence;
        this.config = config;
        this.clock = clock;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public Election startElection(String officeId) throws SQLException, ElectionException {
        lock.lock();
        try {
            Optional<Election> existing = persistence.elections().findCurrentByOfficeId(officeId);
            if (existing.isPresent()) {
                throw new ElectionException("An active election already exists for office: " + officeId);
            }

            ConfigLoader.OfficeConfig officeCfg = config.office();
            long now = clock.instant().toEpochMilli();
            long votingOpensAt  = now + (officeCfg.nominationDays() + officeCfg.campaignDays()) * MS_PER_DAY;
            long votingClosesAt = votingOpensAt + officeCfg.votingDays() * MS_PER_DAY;

            Election election = new Election();
            election.setOfficeId(officeId);
            election.setStatus(ElectionPhase.NOMINATION.name());
            election.setNominationOpensAt(now);
            election.setVotingOpensAt(votingOpensAt);
            election.setVotingClosesAt(votingClosesAt);
            election.setCreatedAt(now);

            long id = persistence.elections().insert(election);
            election.setId(id);

            setOfficePhase(officeId, ElectionPhase.NOMINATION.name());

            LOGGER.info("Election {} started for office {} [nom {}d / campaign {}d / voting {}d]",
                id, officeId, officeCfg.nominationDays(), officeCfg.campaignDays(), officeCfg.votingDays());
            return election;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Advances the election to its next phase. If the resulting phase is
     * RESOLVING, winner calculation and the COMPLETE transition happen
     * synchronously within the same call. Returns the election in its final
     * state after the advance.
     */
    public Election advancePhase(long electionId) throws SQLException, ElectionException {
        lock.lock();
        try {
            Election election = requireElection(electionId);
            ElectionPhase current = ElectionPhase.fromString(election.getStatus());

            ElectionPhase next = switch (current) {
                case NOMINATION -> ElectionPhase.CAMPAIGN;
                case CAMPAIGN   -> ElectionPhase.VOTING;
                case VOTING     -> ElectionPhase.RESOLVING;
                case RESOLVING  -> ElectionPhase.COMPLETE;
                default -> throw new ElectionException("Cannot advance from phase " + current);
            };

            election.setStatus(next.name());
            persistence.elections().update(election);
            setOfficePhase(election.getOfficeId(), next.name());
            LOGGER.info("Election {} phase: {} → {}", electionId, current, next);

            Election electionSnapshot = election;
            ElectionPhase nextSnapshot = next;
            phaseListeners.forEach(l -> l.accept(electionSnapshot, nextSnapshot));

            if (next == ElectionPhase.RESOLVING) {
                election = resolveElection(election);
            }

            return election;
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Candidate registration
    // -------------------------------------------------------------------------

    public Candidate registerCandidate(long electionId, String playerUuid, String slogan)
            throws SQLException, ElectionException {
        lock.lock();
        try {
            Election election = requireElection(electionId);
            ElectionPhase phase = ElectionPhase.fromString(election.getStatus());

            if (phase != ElectionPhase.NOMINATION && phase != ElectionPhase.CAMPAIGN) {
                throw new ElectionException("Nominations are closed (current phase: " + phase + ")");
            }

            if (persistence.candidates().findByElectionAndPlayer(electionId, playerUuid).isPresent()) {
                throw new ElectionException("Player " + playerUuid + " is already a candidate");
            }

            persistence.officeStates().findByOfficeId(election.getOfficeId()).ifPresent(state -> {
                if (playerUuid.equals(state.getHolderUuid())) {
                    try {
                        throw new ElectionException("The current officeholder cannot run as a candidate");
                    } catch (ElectionException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            long now = clock.instant().toEpochMilli();
            Candidate candidate = new Candidate(0, electionId, playerUuid, slogan, now);
            long id = persistence.candidates().insert(candidate);
            candidate.setId(id);

            LOGGER.info("Candidate registered: {} in election {}", playerUuid, electionId);
            return candidate;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ElectionException ee) throw ee;
            throw e;
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Voting
    // -------------------------------------------------------------------------

    /**
     * @param playerPlaytimeMinutes total playtime of the voter — callers obtain
     *                              this from Minecraft APIs and pass it in, keeping
     *                              this service free of Minecraft dependencies.
     */
    public Vote castVote(long electionId, String voterUuid, String candidateUuid, int playerPlaytimeMinutes)
            throws SQLException, ElectionException {
        lock.lock();
        try {
            Election election = requireElection(electionId);

            if (ElectionPhase.fromString(election.getStatus()) != ElectionPhase.VOTING) {
                throw new ElectionException("Voting is not currently active");
            }

            int minPlaytime = config.voting().minimumPlaytimeMinutes();
            if (playerPlaytimeMinutes < minPlaytime) {
                throw new ElectionException(
                    "Minimum playtime not met (" + playerPlaytimeMinutes + "/" + minPlaytime + " min)");
            }

            if (persistence.votes().findByElectionAndVoter(electionId, voterUuid).isPresent()) {
                throw new ElectionException("Player " + voterUuid + " has already voted");
            }

            if (persistence.candidates().findByElectionAndPlayer(electionId, candidateUuid).isEmpty()) {
                throw new ElectionException("No such candidate " + candidateUuid + " in election " + electionId);
            }

            long now = clock.instant().toEpochMilli();
            Vote vote = new Vote(0, electionId, voterUuid, candidateUuid, now);
            long id = persistence.votes().insert(vote);
            vote.setId(id);

            LOGGER.info("Vote recorded: {} → {} (election {})", voterUuid, candidateUuid, electionId);
            return vote;
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Optional<Election> getCurrentElection(String officeId) throws SQLException {
        return persistence.elections().findCurrentByOfficeId(officeId);
    }

    public List<Candidate> getCandidates(long electionId) throws SQLException {
        return persistence.candidates().findByElectionId(electionId);
    }

    // -------------------------------------------------------------------------
    // Winner callbacks
    // -------------------------------------------------------------------------

    public void addWinnerListener(Consumer<ElectionResult> listener) {
        winnerListeners.add(listener);
    }

    public void addPhaseListener(BiConsumer<Election, ElectionPhase> listener) {
        phaseListeners.add(listener);
    }

    // -------------------------------------------------------------------------
    // Public timing helper (used by ScheduleService / recovery)
    // -------------------------------------------------------------------------

    public static long phaseDeadlineEpochMs(Election e, ElectionPhase phase, ConfigLoader.OfficeConfig cfg) {
        return switch (phase) {
            case NOMINATION -> e.getNominationOpensAt() + cfg.nominationDays() * MS_PER_DAY;
            case CAMPAIGN   -> e.getVotingOpensAt();
            case VOTING     -> e.getVotingClosesAt();
            default         -> Long.MAX_VALUE;
        };
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private Election resolveElection(Election election) throws SQLException {
        String winnerUuid = calculateWinner(election.getId());
        election.setWinnerUuid(winnerUuid);
        election.setStatus(ElectionPhase.COMPLETE.name());
        persistence.elections().update(election);
        setOfficePhase(election.getOfficeId(), ElectionPhase.COMPLETE.name());

        LOGGER.info("Election {} resolved — winner: {}", election.getId(),
            winnerUuid != null ? winnerUuid : "<no candidates>");

        ElectionResult result = new ElectionResult(election.getId(), election.getOfficeId(), winnerUuid);
        winnerListeners.forEach(l -> l.accept(result));
        return election;
    }

    private String calculateWinner(long electionId) throws SQLException {
        List<Candidate> candidates = persistence.candidates().findByElectionId(electionId);
        if (candidates.isEmpty()) return null;

        List<Vote> votes = persistence.votes().findByElectionId(electionId);

        Map<String, Long> tally = candidates.stream()
            .collect(Collectors.toMap(Candidate::getPlayerUuid, c -> 0L));
        votes.forEach(v -> tally.merge(v.getCandidateUuid(), 1L, Long::sum));

        long max = tally.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<String> leaders = tally.entrySet().stream()
            .filter(e -> e.getValue() == max)
            .map(Map.Entry::getKey)
            .toList();

        return leaders.size() == 1
            ? leaders.get(0)
            : leaders.get(ThreadLocalRandom.current().nextInt(leaders.size()));
    }

    private Election requireElection(long electionId) throws SQLException, ElectionException {
        return persistence.elections().findById(electionId)
            .orElseThrow(() -> new ElectionException("Election not found: " + electionId));
    }

    private void setOfficePhase(String officeId, String phase) throws SQLException {
        OfficeState state = persistence.officeStates().findByOfficeId(officeId)
            .orElseGet(() -> new OfficeState(officeId, null, null, null, 0L, 0L, null));
        state.setPhase(phase);
        persistence.officeStates().save(state);
    }
}
