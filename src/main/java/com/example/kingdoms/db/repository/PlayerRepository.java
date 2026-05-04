package com.example.kingdoms.db.repository;

import com.example.kingdoms.db.model.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlayerRepository {

    private final Connection conn;

    public PlayerRepository(Connection conn) {
        this.conn = conn;
    }

    public Optional<Player> findByUuid(String uuid) throws SQLException {
        String sql = "SELECT uuid, username, current_origin_id, previous_origin_id, " +
                     "first_join_origin_assigned, created_at, updated_at FROM players WHERE uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<Player> findAll() throws SQLException {
        String sql = "SELECT uuid, username, current_origin_id, previous_origin_id, " +
                     "first_join_origin_assigned, created_at, updated_at FROM players";
        List<Player> players = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) players.add(map(rs));
        }
        return players;
    }

    public void save(Player player) throws SQLException {
        String sql = "INSERT INTO players (uuid, username, current_origin_id, previous_origin_id, " +
                     "first_join_origin_assigned, created_at, updated_at) VALUES (?,?,?,?,?,?,?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET " +
                     "username=excluded.username, current_origin_id=excluded.current_origin_id, " +
                     "previous_origin_id=excluded.previous_origin_id, " +
                     "first_join_origin_assigned=excluded.first_join_origin_assigned, " +
                     "updated_at=excluded.updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUuid());
            ps.setString(2, player.getUsername());
            ps.setString(3, player.getCurrentOriginId());
            ps.setString(4, player.getPreviousOriginId());
            ps.setInt(5, player.isFirstJoinOriginAssigned() ? 1 : 0);
            ps.setLong(6, player.getCreatedAt());
            ps.setLong(7, player.getUpdatedAt());
            ps.executeUpdate();
        }
    }

    public void delete(String uuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }

    private static Player map(ResultSet rs) throws SQLException {
        return new Player(
            rs.getString("uuid"),
            rs.getString("username"),
            rs.getString("current_origin_id"),
            rs.getString("previous_origin_id"),
            rs.getInt("first_join_origin_assigned") == 1,
            rs.getLong("created_at"),
            rs.getLong("updated_at")
        );
    }
}
