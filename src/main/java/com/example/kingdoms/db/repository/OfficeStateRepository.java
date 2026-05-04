package com.example.kingdoms.db.repository;

import com.example.kingdoms.db.model.OfficeState;

import java.sql.*;
import java.util.Optional;

public final class OfficeStateRepository {

    private final Connection conn;

    public OfficeStateRepository(Connection conn) {
        this.conn = conn;
    }

    public Optional<OfficeState> findByOfficeId(String officeId) throws SQLException {
        String sql = "SELECT office_id, holder_uuid, holder_origin_before_office, active_king_origin_id, " +
                     "term_started_at, term_ends_at, phase FROM office_state WHERE office_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, officeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public void save(OfficeState state) throws SQLException {
        String sql = "INSERT INTO office_state (office_id, holder_uuid, holder_origin_before_office, " +
                     "active_king_origin_id, term_started_at, term_ends_at, phase) VALUES (?,?,?,?,?,?,?) " +
                     "ON CONFLICT(office_id) DO UPDATE SET " +
                     "holder_uuid=excluded.holder_uuid, " +
                     "holder_origin_before_office=excluded.holder_origin_before_office, " +
                     "active_king_origin_id=excluded.active_king_origin_id, " +
                     "term_started_at=excluded.term_started_at, " +
                     "term_ends_at=excluded.term_ends_at, " +
                     "phase=excluded.phase";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, state.getOfficeId());
            ps.setString(2, state.getHolderUuid());
            ps.setString(3, state.getHolderOriginBeforeOffice());
            ps.setString(4, state.getActiveKingOriginId());
            ps.setLong(5, state.getTermStartedAt());
            ps.setLong(6, state.getTermEndsAt());
            ps.setString(7, state.getPhase());
            ps.executeUpdate();
        }
    }

    public void delete(String officeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM office_state WHERE office_id = ?")) {
            ps.setString(1, officeId);
            ps.executeUpdate();
        }
    }

    private static OfficeState map(ResultSet rs) throws SQLException {
        return new OfficeState(
            rs.getString("office_id"),
            rs.getString("holder_uuid"),
            rs.getString("holder_origin_before_office"),
            rs.getString("active_king_origin_id"),
            rs.getLong("term_started_at"),
            rs.getLong("term_ends_at"),
            rs.getString("phase")
        );
    }
}
