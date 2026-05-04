package com.example.kingdoms.db.repository;

import com.example.kingdoms.db.model.History;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class HistoryRepository {

    private final Connection conn;

    public HistoryRepository(Connection conn) {
        this.conn = conn;
    }

    public List<History> findByActorUuid(String actorUuid) throws SQLException {
        String sql = "SELECT id, event_type, actor_uuid, target_uuid, payload_json, created_at " +
                     "FROM history WHERE actor_uuid = ? ORDER BY id DESC";
        List<History> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actorUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        }
        return results;
    }

    public List<History> findByEventType(String eventType) throws SQLException {
        String sql = "SELECT id, event_type, actor_uuid, target_uuid, payload_json, created_at " +
                     "FROM history WHERE event_type = ? ORDER BY id DESC";
        List<History> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        }
        return results;
    }

    public List<History> findRecent(int limit) throws SQLException {
        String sql = "SELECT id, event_type, actor_uuid, target_uuid, payload_json, created_at " +
                     "FROM history ORDER BY id DESC LIMIT ?";
        List<History> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        }
        return results;
    }

    public long insert(History history) throws SQLException {
        String sql = "INSERT INTO history (event_type, actor_uuid, target_uuid, payload_json, created_at) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, history.getEventType());
            ps.setString(2, history.getActorUuid());
            ps.setString(3, history.getTargetUuid());
            ps.setString(4, history.getPayloadJson());
            ps.setLong(5, history.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Insert returned no generated key");
    }

    private static History map(ResultSet rs) throws SQLException {
        return new History(
            rs.getLong("id"),
            rs.getString("event_type"),
            rs.getString("actor_uuid"),
            rs.getString("target_uuid"),
            rs.getString("payload_json"),
            rs.getLong("created_at")
        );
    }
}
