package com.example.kingdoms.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TrustRepository {
    private final Connection conn;

    public TrustRepository(Connection conn) {
        this.conn = conn;
    }

    public int getScore(String rulerUuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT trust_score FROM ruler_trust WHERE ruler_uuid = ?")) {
            ps.setString(1, rulerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("trust_score") : 50;
            }
        }
    }

    public void adjust(String rulerUuid, int delta) throws SQLException {
        int score = Math.max(0, Math.min(100, getScore(rulerUuid) + delta));
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO ruler_trust (ruler_uuid, trust_score, updated_at) VALUES (?,?,?) " +
                "ON CONFLICT(ruler_uuid) DO UPDATE SET trust_score=excluded.trust_score, updated_at=excluded.updated_at")) {
            ps.setString(1, rulerUuid);
            ps.setInt(2, score);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void addHistory(String rulerUuid, long electionId, String promised, String enacted, boolean honored) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO promise_history (ruler_uuid, election_id, promised_perk, enacted_perks, honored, created_at) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, rulerUuid);
            ps.setLong(2, electionId);
            ps.setString(3, promised);
            ps.setString(4, enacted);
            ps.setInt(5, honored ? 1 : 0);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public String recentHistory(String rulerUuid, int limit) throws SQLException {
        StringBuilder out = new StringBuilder();
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT promised_perk, honored, created_at FROM promise_history WHERE ruler_uuid = ? ORDER BY id DESC LIMIT ?")) {
            ps.setString(1, rulerUuid);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!out.isEmpty()) out.append("\n");
                    out.append(rs.getInt("honored") == 1 ? "Honored " : "Broke ")
                        .append(rs.getString("promised_perk"));
                }
            }
        }
        return out.toString();
    }
}
