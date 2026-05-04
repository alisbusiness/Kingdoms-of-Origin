package com.example.kingdoms.db.repository;

import com.example.kingdoms.db.model.Election;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ElectionRepository {

    private final Connection conn;

    public ElectionRepository(Connection conn) {
        this.conn = conn;
    }

    public Optional<Election> findById(long id) throws SQLException {
        String sql = "SELECT id, office_id, status, nomination_opens_at, voting_opens_at, " +
                     "voting_closes_at, winner_uuid, created_at FROM elections WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<Election> findByOfficeId(String officeId) throws SQLException {
        String sql = "SELECT id, office_id, status, nomination_opens_at, voting_opens_at, " +
                     "voting_closes_at, winner_uuid, created_at FROM elections WHERE office_id = ? ORDER BY id DESC";
        List<Election> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, officeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        }
        return results;
    }

    public Optional<Election> findCurrentByOfficeId(String officeId) throws SQLException {
        String sql = "SELECT id, office_id, status, nomination_opens_at, voting_opens_at, " +
                     "voting_closes_at, winner_uuid, created_at FROM elections " +
                     "WHERE office_id = ? AND status NOT IN ('COMPLETE','CANCELLED') ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, officeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public long insert(Election election) throws SQLException {
        String sql = "INSERT INTO elections (office_id, status, nomination_opens_at, voting_opens_at, " +
                     "voting_closes_at, winner_uuid, created_at) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, election.getOfficeId());
            ps.setString(2, election.getStatus());
            ps.setLong(3, election.getNominationOpensAt());
            ps.setLong(4, election.getVotingOpensAt());
            ps.setLong(5, election.getVotingClosesAt());
            ps.setString(6, election.getWinnerUuid());
            ps.setLong(7, election.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Insert returned no generated key");
    }

    public void update(Election election) throws SQLException {
        String sql = "UPDATE elections SET office_id=?, status=?, nomination_opens_at=?, " +
                     "voting_opens_at=?, voting_closes_at=?, winner_uuid=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, election.getOfficeId());
            ps.setString(2, election.getStatus());
            ps.setLong(3, election.getNominationOpensAt());
            ps.setLong(4, election.getVotingOpensAt());
            ps.setLong(5, election.getVotingClosesAt());
            ps.setString(6, election.getWinnerUuid());
            ps.setLong(7, election.getId());
            ps.executeUpdate();
        }
    }

    private static Election map(ResultSet rs) throws SQLException {
        return new Election(
            rs.getLong("id"),
            rs.getString("office_id"),
            rs.getString("status"),
            rs.getLong("nomination_opens_at"),
            rs.getLong("voting_opens_at"),
            rs.getLong("voting_closes_at"),
            rs.getString("winner_uuid"),
            rs.getLong("created_at")
        );
    }
}
