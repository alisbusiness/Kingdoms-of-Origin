package com.example.kingdoms.db.repository;

import com.example.kingdoms.db.model.Vote;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VoteRepository {

    private final Connection conn;

    public VoteRepository(Connection conn) {
        this.conn = conn;
    }

    public List<Vote> findByElectionId(long electionId) throws SQLException {
        String sql = "SELECT id, election_id, voter_uuid, candidate_uuid, created_at " +
                     "FROM votes WHERE election_id = ?";
        List<Vote> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        }
        return results;
    }

    public Optional<Vote> findByElectionAndVoter(long electionId, String voterUuid) throws SQLException {
        String sql = "SELECT id, election_id, voter_uuid, candidate_uuid, created_at " +
                     "FROM votes WHERE election_id = ? AND voter_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, electionId);
            ps.setString(2, voterUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public long insert(Vote vote) throws SQLException {
        String sql = "INSERT INTO votes (election_id, voter_uuid, candidate_uuid, created_at) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, vote.getElectionId());
            ps.setString(2, vote.getVoterUuid());
            ps.setString(3, vote.getCandidateUuid());
            ps.setLong(4, vote.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Insert returned no generated key");
    }

    public void deleteByElectionId(long electionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM votes WHERE election_id = ?")) {
            ps.setLong(1, electionId);
            ps.executeUpdate();
        }
    }

    private static Vote map(ResultSet rs) throws SQLException {
        return new Vote(
            rs.getLong("id"),
            rs.getLong("election_id"),
            rs.getString("voter_uuid"),
            rs.getString("candidate_uuid"),
            rs.getLong("created_at")
        );
    }
}
