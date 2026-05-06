package com.example.kingdoms.db.repository;

import com.example.kingdoms.treasury.TreasuryState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class TreasuryRepository {
    private final Connection conn;

    public TreasuryRepository(Connection conn) {
        this.conn = conn;
    }

    public TreasuryState get(String officeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM treasury_state WHERE office_id = ?")) {
            ps.setString(1, officeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return read(rs);
            }
        }
        TreasuryState state = TreasuryState.defaults(officeId);
        save(state);
        return state;
    }

    public void save(TreasuryState s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO treasury_state (
              office_id, raw_diamonds, diamond_blocks, currency_supply, taxes_collected,
              public_spending, treasury_withdrawals, emergency_minting, xp_tax_rate,
              trade_tax_rate, resource_tithe_rate, emergency_levy_rate, legitimacy,
              corruption_heat, unrest, revolt_active, revolt_started_at, capture_progress,
              transition_freeze_until, updated_at
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(office_id) DO UPDATE SET
              raw_diamonds=excluded.raw_diamonds,
              diamond_blocks=excluded.diamond_blocks,
              currency_supply=excluded.currency_supply,
              taxes_collected=excluded.taxes_collected,
              public_spending=excluded.public_spending,
              treasury_withdrawals=excluded.treasury_withdrawals,
              emergency_minting=excluded.emergency_minting,
              xp_tax_rate=excluded.xp_tax_rate,
              trade_tax_rate=excluded.trade_tax_rate,
              resource_tithe_rate=excluded.resource_tithe_rate,
              emergency_levy_rate=excluded.emergency_levy_rate,
              legitimacy=excluded.legitimacy,
              corruption_heat=excluded.corruption_heat,
              unrest=excluded.unrest,
              revolt_active=excluded.revolt_active,
              revolt_started_at=excluded.revolt_started_at,
              capture_progress=excluded.capture_progress,
              transition_freeze_until=excluded.transition_freeze_until,
              updated_at=excluded.updated_at
            """)) {
            ps.setString(1, s.officeId());
            ps.setInt(2, s.rawDiamonds());
            ps.setInt(3, s.diamondBlocks());
            ps.setInt(4, s.currencySupply());
            ps.setInt(5, s.taxesCollected());
            ps.setInt(6, s.publicSpending());
            ps.setInt(7, s.treasuryWithdrawals());
            ps.setInt(8, s.emergencyMinting());
            ps.setInt(9, s.xpTaxRate());
            ps.setInt(10, s.tradeTaxRate());
            ps.setInt(11, s.resourceTitheRate());
            ps.setInt(12, s.emergencyLevyRate());
            ps.setInt(13, s.legitimacy());
            ps.setInt(14, s.corruptionHeat());
            ps.setInt(15, s.unrest());
            ps.setInt(16, s.revoltActive() ? 1 : 0);
            ps.setLong(17, s.revoltStartedAt());
            ps.setInt(18, s.captureProgress());
            ps.setLong(19, s.transitionFreezeUntil());
            ps.setLong(20, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public int balance(String playerUuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM currency_balances WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("balance") : 0;
            }
        }
    }

    public void adjustBalance(String playerUuid, int delta) throws SQLException {
        int next = Math.max(0, balance(playerUuid) + delta);
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO currency_balances (player_uuid, balance, updated_at) VALUES (?,?,?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET balance=excluded.balance, updated_at=excluded.updated_at")) {
            ps.setString(1, playerUuid);
            ps.setInt(2, next);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void joinSide(String playerUuid, String side) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO revolt_participants (player_uuid, side, joined_at) VALUES (?,?,?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET side=excluded.side, joined_at=excluded.joined_at")) {
            ps.setString(1, playerUuid);
            ps.setString(2, side);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public String side(String playerUuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT side FROM revolt_participants WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("side") : null;
            }
        }
    }

    public int sideCount(String side) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM revolt_participants WHERE side = ?")) {
            ps.setString(1, side);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void clearParticipants() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM revolt_participants")) {
            ps.executeUpdate();
        }
    }

    public void ledger(String officeId, String actorUuid, String type, int amount, boolean isPublic, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO treasury_ledger (office_id, actor_uuid, type, amount, public, note, created_at) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, officeId);
            ps.setString(2, actorUuid);
            ps.setString(3, type);
            ps.setInt(4, amount);
            ps.setInt(5, isPublic ? 1 : 0);
            ps.setString(6, note);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public List<String> recentPublicLedger(String officeId, int limit) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT type, amount, note FROM treasury_ledger WHERE office_id = ? AND public = 1 ORDER BY id DESC LIMIT ?")) {
            ps.setString(1, officeId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(rs.getString("type") + " " + rs.getInt("amount") + " - " + rs.getString("note"));
            }
        }
        return rows;
    }

    private TreasuryState read(ResultSet rs) throws SQLException {
        return new TreasuryState(
            rs.getString("office_id"),
            rs.getInt("raw_diamonds"),
            rs.getInt("diamond_blocks"),
            rs.getInt("currency_supply"),
            rs.getInt("taxes_collected"),
            rs.getInt("public_spending"),
            rs.getInt("treasury_withdrawals"),
            rs.getInt("emergency_minting"),
            rs.getInt("xp_tax_rate"),
            rs.getInt("trade_tax_rate"),
            rs.getInt("resource_tithe_rate"),
            rs.getInt("emergency_levy_rate"),
            rs.getInt("legitimacy"),
            rs.getInt("corruption_heat"),
            rs.getInt("unrest"),
            rs.getInt("revolt_active") == 1,
            rs.getLong("revolt_started_at"),
            rs.getInt("capture_progress"),
            rs.getLong("transition_freeze_until"),
            rs.getLong("updated_at")
        );
    }
}
