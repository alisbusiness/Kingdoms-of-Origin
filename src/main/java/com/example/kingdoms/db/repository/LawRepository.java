package com.example.kingdoms.db.repository;

import com.example.kingdoms.db.model.Law;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class LawRepository {
    private final Connection conn;

    public LawRepository(Connection conn) {
        this.conn = conn;
    }

    public List<Law> findAll() throws SQLException {
        List<Law> laws = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT id, position, text, created_by_uuid, created_at FROM kingdom_laws ORDER BY position ASC, id ASC"
        )) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) laws.add(read(rs));
            }
        }
        return laws;
    }

    public long add(String text, String createdByUuid) throws SQLException {
        int nextPosition = nextPosition();
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO kingdom_laws (position, text, created_by_uuid, created_at, updated_at) VALUES (?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        )) {
            long now = System.currentTimeMillis();
            ps.setInt(1, nextPosition);
            ps.setString(2, text);
            ps.setString(3, createdByUuid);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Insert returned no generated key");
    }

    public boolean removeByPosition(int position) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM kingdom_laws WHERE position = ?")) {
            ps.setInt(1, position);
            int changed = ps.executeUpdate();
            if (changed > 0) renumber();
            return changed > 0;
        }
    }

    public void replaceAll(List<String> laws, String createdByUuid) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement delete = conn.prepareStatement("DELETE FROM kingdom_laws")) {
                delete.executeUpdate();
            }
            try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO kingdom_laws (position, text, created_by_uuid, created_at, updated_at) VALUES (?,?,?,?,?)"
            )) {
                long now = System.currentTimeMillis();
                for (int i = 0; i < laws.size(); i++) {
                    insert.setInt(1, i + 1);
                    insert.setString(2, laws.get(i));
                    insert.setString(3, createdByUuid);
                    insert.setLong(4, now);
                    insert.setLong(5, now);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    public long latestUpdatedAt() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(updated_at), 0) FROM kingdom_laws")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private int nextPosition() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(position), 0) + 1 FROM kingdom_laws")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private void renumber() throws SQLException {
        List<Law> laws = findAll();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE kingdom_laws SET position = ?, updated_at = ? WHERE id = ?")) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < laws.size(); i++) {
                ps.setInt(1, i + 1);
                ps.setLong(2, now);
                ps.setLong(3, laws.get(i).getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static Law read(ResultSet rs) throws SQLException {
        return new Law(
            rs.getLong("id"),
            rs.getInt("position"),
            rs.getString("text"),
            rs.getString("created_by_uuid"),
            rs.getLong("created_at")
        );
    }
}
