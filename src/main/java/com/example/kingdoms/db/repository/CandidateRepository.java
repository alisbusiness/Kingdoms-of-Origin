package com.example.kingdoms.db.repository;

import com.example.kingdoms.db.model.Candidate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CandidateRepository {

    private final Connection conn;

    public CandidateRepository(Connection conn) {
        this.conn = conn;
    }

    public List<Candidate> findByElectionId(long electionId) throws SQLException {
        String sql = "SELECT id, election_id, player_uuid, slogan, promised_perks, created_at " +
                     "FROM candidates WHERE election_id = ? ORDER BY id";
        List<Candidate> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        }
        return results;
    }

    public Optional<Candidate> findByElectionAndPlayer(long electionId, String playerUuid) throws SQLException {
        String sql = "SELECT id, election_id, player_uuid, slogan, promised_perks, created_at " +
                     "FROM candidates WHERE election_id = ? AND player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, electionId);
            ps.setString(2, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public long insert(Candidate candidate) throws SQLException {
        String sql = "INSERT INTO candidates (election_id, player_uuid, slogan, promised_perks, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, candidate.getElectionId());
            ps.setString(2, candidate.getPlayerUuid());
            ps.setString(3, candidate.getSlogan());
            ps.setString(4, candidate.getPromisedPerks());
            ps.setLong(5, candidate.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Insert returned no generated key");
    }

    public void updatePromises(long id, String promisedPerks) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE candidates SET promised_perks = ? WHERE id = ?")) {
            ps.setString(1, promisedPerks);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM candidates WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static Candidate map(ResultSet rs) throws SQLException {
        return new Candidate(
            rs.getLong("id"),
            rs.getLong("election_id"),
            rs.getString("player_uuid"),
            rs.getString("slogan"),
            rs.getString("promised_perks"),
            rs.getLong("created_at")
        );
    }
}
