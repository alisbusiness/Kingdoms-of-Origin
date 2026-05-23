package com.example.kingdoms.treasury;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.origin.OfficeService;
import com.example.kingdoms.ui.AnnouncementService;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.sql.SQLException;

public final class TreasuryService {
    public static final String CURRENCY_NAME = "Crowns";

    private final PersistenceService persistence;
    private final ConfigLoader config;
    private final OfficeService officeService;
    private AnnouncementService announcementService;
    private MinecraftServer server;
    private int auditTicks;

    public TreasuryService(PersistenceService persistence, ConfigLoader config, OfficeService officeService) {
        this.persistence = persistence;
        this.config = config;
        this.officeService = officeService;
    }

    public void setAnnouncementService(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public synchronized TreasuryState state() throws SQLException {
        return persistence.treasury().get(config.office().id());
    }

    public synchronized int balance(String playerUuid) throws SQLException {
        return persistence.treasury().balance(playerUuid);
    }

    public String currencyName() {
        return config.treasury().currencyName();
    }

    public synchronized void depositReserves(ServerPlayerEntity actor, int diamonds, int blocks) throws SQLException {
        if (diamonds < 0 || blocks < 0) return;
        int removedDiamonds = removeItems(actor, Items.DIAMOND, diamonds);
        int removedBlocks = removeItems(actor, Items.DIAMOND_BLOCK, blocks);
        if (removedDiamonds == 0 && removedBlocks == 0) {
            actor.sendMessage(Text.literal("No diamonds or diamond blocks were deposited.").formatted(Formatting.RED), false);
            return;
        }
        TreasuryState s = state();
        save(copy(s, s.rawDiamonds() + removedDiamonds, s.diamondBlocks() + removedBlocks, s.currencySupply(),
            s.taxesCollected(), s.publicSpending(), s.treasuryWithdrawals(), s.emergencyMinting(),
            s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(), s.emergencyLevyRate(),
            clamp(s.legitimacy() + 2), clampHeat(s.corruptionHeat() - 1), clamp(s.unrest() - 4),
            s.revoltActive(), s.revoltStartedAt(), s.captureProgress(), s.transitionFreezeUntil()));
        persistence.treasury().ledger(config.office().id(), actor.getUuidAsString(), "reserve_deposit",
            removedDiamonds + removedBlocks * 9, true, "Diamond reserves deposited");
        actor.sendMessage(Text.literal("Deposited " + removedDiamonds + " diamonds and " + removedBlocks + " diamond blocks.").formatted(Formatting.GREEN), false);
        auditAndMaybeRevolt("Reserve recovery");
    }

    public synchronized void setTax(ServerPlayerEntity actor, String channel, int rate) throws SQLException {
        requireKing(actor);
        TreasuryState s = state();
        int cap = switch (channel) {
            case "xp" -> config.treasury().xpTaxCap();
            case "trade" -> config.treasury().tradeTaxCap();
            case "resource" -> config.treasury().resourceTaxCap();
            case "levy" -> config.treasury().levyTaxCap();
            default -> throw new IllegalArgumentException("Unknown tax channel: " + channel);
        };
        int next = Math.max(0, Math.min(cap, rate));
        int old = switch (channel) {
            case "xp" -> s.xpTaxRate();
            case "trade" -> s.tradeTaxRate();
            case "resource" -> s.resourceTitheRate();
            case "levy" -> s.emergencyLevyRate();
            default -> 0;
        };
        int hike = Math.max(0, next - old);
        save(copy(s, s.rawDiamonds(), s.diamondBlocks(), s.currencySupply(), s.taxesCollected(),
            s.publicSpending(), s.treasuryWithdrawals(), s.emergencyMinting(),
            channel.equals("xp") ? next : s.xpTaxRate(),
            channel.equals("trade") ? next : s.tradeTaxRate(),
            channel.equals("resource") ? next : s.resourceTitheRate(),
            channel.equals("levy") ? next : s.emergencyLevyRate(),
            clamp(s.legitimacy() - hike / 4), clampHeat(s.corruptionHeat() + (channel.equals("levy") ? hike / 3 : hike / 6)),
            clamp(s.unrest() + hike / 2 + (next > 20 ? 4 : 0)), s.revoltActive(), s.revoltStartedAt(), s.captureProgress(), s.transitionFreezeUntil()));
        persistence.treasury().ledger(config.office().id(), actor.getUuidAsString(), "tax_policy", next, true, channel + " tax set to " + next + "%");
        broadcast(Text.literal("Tax policy changed: " + channel + " tax is now " + next + "%.").formatted(next > 20 ? Formatting.RED : Formatting.GOLD));
        auditAndMaybeRevolt("Tax hike");
    }

    public synchronized void mint(ServerPlayerEntity actor, int amount, boolean emergency) throws SQLException {
        requireKing(actor);
        TreasuryState s = state();
        if (s.revoltActive()) throw new IllegalStateException("Minting is suspended during active revolt.");
        if (System.currentTimeMillis() < s.transitionFreezeUntil()) throw new IllegalStateException("Treasury controls are frozen during transition.");
        int minted = Math.max(1, Math.min(amount, config.treasury().mintMax()));
        int unsupported = Math.max(0, s.currencySupply() + minted - s.reserveValue());
        save(copy(s, s.rawDiamonds(), s.diamondBlocks(), s.currencySupply() + minted, s.taxesCollected(),
            s.publicSpending(), s.treasuryWithdrawals(), emergency ? s.emergencyMinting() + minted : s.emergencyMinting(),
            s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(), s.emergencyLevyRate(),
            clamp(s.legitimacy() - unsupported / 20 - (emergency ? 2 : 0)),
            clampHeat(s.corruptionHeat() + unsupported / 15 + (emergency ? 5 : 1)),
            clamp(s.unrest() + unsupported / 12 + (emergency ? 5 : 1)),
            s.revoltActive(), s.revoltStartedAt(), s.captureProgress(), s.transitionFreezeUntil()));
        persistence.treasury().adjustBalance(actor.getUuidAsString(), minted);
        persistence.treasury().ledger(config.office().id(), actor.getUuidAsString(), emergency ? "emergency_mint" : "mint", minted, true, "New Crowns issued");
        broadcast(Text.literal("The Crown minted " + minted + " " + currencyName() + ". Reserve backing is now " + ratioText(state()) + ".").formatted(unsupported > 0 ? Formatting.RED : Formatting.GOLD));
        auditAndMaybeRevolt("Currency issuance");
    }

    public synchronized void redeem(ServerPlayerEntity actor, int amount) throws SQLException {
        int wanted = Math.max(1, amount);
        int bal = balance(actor.getUuidAsString());
        if (bal < wanted) {
            actor.sendMessage(Text.literal("You only hold " + bal + " " + currencyName() + ".").formatted(Formatting.RED), false);
            return;
        }
        TreasuryState s = state();
        int redeemable = Math.min(wanted, s.reserveValue());
        if (redeemable <= 0) {
            save(copy(s, s.rawDiamonds(), s.diamondBlocks(), s.currencySupply(), s.taxesCollected(), s.publicSpending(),
                s.treasuryWithdrawals(), s.emergencyMinting(), s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(),
                s.emergencyLevyRate(), clamp(s.legitimacy() - 12), clampHeat(s.corruptionHeat() + 8), clamp(s.unrest() + 18),
                s.revoltActive(), s.revoltStartedAt(), s.captureProgress(), s.transitionFreezeUntil()));
            broadcast(Text.literal("Redemption has failed: the treasury cannot pay diamonds for Crowns.").formatted(Formatting.DARK_RED));
            auditAndMaybeRevolt("Failed redemption");
            return;
        }
        int blocksToPay = Math.min(s.diamondBlocks(), redeemable / 9);
        int diamondsToPay = redeemable - blocksToPay * 9;
        if (diamondsToPay > s.rawDiamonds()) {
            int needed = diamondsToPay - s.rawDiamonds();
            int extraBlocks = Math.min(s.diamondBlocks() - blocksToPay, (needed + 8) / 9);
            blocksToPay += extraBlocks;
            diamondsToPay = Math.max(0, redeemable - blocksToPay * 9);
        }
        actor.giveItemStack(new net.minecraft.item.ItemStack(Items.DIAMOND_BLOCK, blocksToPay));
        actor.giveItemStack(new net.minecraft.item.ItemStack(Items.DIAMOND, diamondsToPay));
        persistence.treasury().adjustBalance(actor.getUuidAsString(), -redeemable);
        save(copy(s, s.rawDiamonds() - diamondsToPay, s.diamondBlocks() - blocksToPay, Math.max(0, s.currencySupply() - redeemable),
            s.taxesCollected(), s.publicSpending(), s.treasuryWithdrawals(), s.emergencyMinting(), s.xpTaxRate(), s.tradeTaxRate(),
            s.resourceTitheRate(), s.emergencyLevyRate(), s.legitimacy(), s.corruptionHeat(), wanted > redeemable ? clamp(s.unrest() + 10) : s.unrest(),
            s.revoltActive(), s.revoltStartedAt(), s.captureProgress(), s.transitionFreezeUntil()));
        persistence.treasury().ledger(config.office().id(), actor.getUuidAsString(), "redemption", redeemable, true, "Crowns redeemed for diamond reserves");
        if (wanted > redeemable) broadcast(Text.literal("Partial redemption: reserves ran short before all Crowns were honored.").formatted(Formatting.RED));
        auditAndMaybeRevolt("Redemption pressure");
    }

    public synchronized void spend(ServerPlayerEntity actor, SpendingCategory category, int amount) throws SQLException {
        requireKing(actor);
        TreasuryState s = state();
        int cost = Math.max(1, Math.min(amount, config.treasury().spendMax()));
        if (s.currencySupply() < cost && category.publicSpending()) throw new IllegalStateException("Not enough issued Crowns for this spending.");
        int nextSupply = category.publicSpending() ? Math.max(0, s.currencySupply() - cost) : s.currencySupply();
        if (category == SpendingCategory.SIPHON || category == SpendingCategory.PALACE) persistence.treasury().adjustBalance(actor.getUuidAsString(), cost);
        save(copy(s, s.rawDiamonds(), s.diamondBlocks(), nextSupply, s.taxesCollected(),
            s.publicSpending() + (category.publicSpending() ? cost : 0),
            s.treasuryWithdrawals() + (!category.publicSpending() ? cost : 0),
            s.emergencyMinting(), s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(), s.emergencyLevyRate(),
            clamp(s.legitimacy() + category.legitimacyDelta()), clampHeat(s.corruptionHeat() + category.heatDelta()),
            clamp(s.unrest() + category.unrestDelta()), s.revoltActive(), s.revoltStartedAt(), s.captureProgress(), s.transitionFreezeUntil()));
        persistence.treasury().ledger(config.office().id(), actor.getUuidAsString(), "spend_" + category.name().toLowerCase(), cost, category.publicSpending(), category.label());
        broadcast(Text.literal(category.publicSpending() ? "Treasury funded " + category.label() + " for " + cost + " Crowns." : "Rumors spread of " + category.label() + " spending.").formatted(category.publicSpending() ? Formatting.GREEN : Formatting.DARK_RED));
        auditAndMaybeRevolt("Treasury spending");
    }

    public synchronized void collectTax(ServerPlayerEntity subject, String channel, int taxableAmount) {
        if (taxableAmount <= 0 || subject.getUuidAsString().equals(officeService.getRuler())) return;
        try {
            TreasuryState s = state();
            int rate = switch (channel) {
                case "xp" -> s.xpTaxRate();
                case "resource" -> s.resourceTitheRate();
                case "trade" -> s.tradeTaxRate();
                case "levy" -> s.emergencyLevyRate();
                default -> 0;
            };
            if (rate <= 0) return;
            int collected = Math.max(1, taxableAmount * rate / 100);
            int reserveDiamonds = channel.equals("resource") ? collected : 0;
            int crownTaxes = channel.equals("resource") ? 0 : collected;
            save(copy(s, s.rawDiamonds() + reserveDiamonds, s.diamondBlocks(), s.currencySupply() + crownTaxes,
                s.taxesCollected() + collected, s.publicSpending(), s.treasuryWithdrawals(), s.emergencyMinting(),
                s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(), s.emergencyLevyRate(), s.legitimacy(),
                s.corruptionHeat(), clamp(s.unrest() + (rate > 20 ? 1 : 0)), s.revoltActive(), s.revoltStartedAt(),
                s.captureProgress(), s.transitionFreezeUntil()));
            persistence.treasury().ledger(config.office().id(), subject.getUuidAsString(), channel + "_tax", collected, true, "Collected by tax policy");
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Failed to collect {} tax from {}", channel, subject.getUuidAsString(), e);
        }
    }

    public synchronized void joinRevolt(ServerPlayerEntity player, String side) throws SQLException {
        TreasuryState s = state();
        if (!s.revoltActive()) {
            player.sendMessage(Text.literal("There is no active revolt window.").formatted(Formatting.RED), false);
            return;
        }
        String normalized = side.equalsIgnoreCase("loyalist") || side.equalsIgnoreCase("crown") ? "LOYALIST" : "REBEL";
        persistence.treasury().joinSide(player.getUuidAsString(), normalized);
        broadcast(Text.literal(player.getName().getString() + " declared for the " + (normalized.equals("REBEL") ? "revolt." : "crown.")).formatted(normalized.equals("REBEL") ? Formatting.RED : Formatting.GOLD));
    }

    public synchronized void onServerMinute(MinecraftServer server) {
        this.server = server;
        try {
            if (++auditTicks % 5 == 0) auditAndMaybeRevolt("Periodic audit");
            tickCapture(server);
        } catch (Exception e) {
            KingdomsPlugin.LOGGER.error("Treasury tick failed", e);
        }
    }

    public synchronized void freezeTransition() throws SQLException {
        TreasuryState s = state();
        save(copy(s, s.rawDiamonds(), s.diamondBlocks(), s.currencySupply(), s.taxesCollected(), s.publicSpending(),
            s.treasuryWithdrawals(), s.emergencyMinting(), s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(),
            s.emergencyLevyRate(), clamp(s.legitimacy() + 5), clampHeat(s.corruptionHeat() - 15), clamp(Math.min(s.unrest(), 45)),
            false, 0L, 0, System.currentTimeMillis() + (long) config.treasury().transitionFreezeMinutes() * 60L * 1000L));
        persistence.treasury().clearParticipants();
    }

    private void tickCapture(MinecraftServer server) throws SQLException, ElectionException {
        TreasuryState s = state();
        if (!s.revoltActive()) return;
        int rebelsJoined = persistence.treasury().sideCount("REBEL");
        int rebels = 0;
        int loyalists = 0;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            double radius = config.revolt().captureRadius();
            if (p.squaredDistanceTo(config.revolt().capitalX(), config.revolt().capitalY(), config.revolt().capitalZ()) > radius * radius) continue;
            String side = persistence.treasury().side(p.getUuidAsString());
            if ("REBEL".equals(side)) rebels++;
            if ("LOYALIST".equals(side) || p.getUuidAsString().equals(officeService.getRuler())) loyalists++;
        }
        int progress = s.captureProgress();
        if (rebelsJoined >= minimumRebels(server) && rebels > loyalists && rebels > 0) progress = Math.min(config.revolt().captureRequired(), progress + 8);
        else if (loyalists >= rebels && progress > 0) progress = Math.max(0, progress - 5);
        if (progress != s.captureProgress()) {
            save(copy(s, s.rawDiamonds(), s.diamondBlocks(), s.currencySupply(), s.taxesCollected(), s.publicSpending(),
                s.treasuryWithdrawals(), s.emergencyMinting(), s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(), s.emergencyLevyRate(),
                s.legitimacy(), s.corruptionHeat(), s.unrest(), true, s.revoltStartedAt(), progress, s.transitionFreezeUntil()));
            broadcast(Text.literal("Capital capture: " + progress + "% (" + rebels + " rebels, " + loyalists + " loyalists at the capital).").formatted(Formatting.RED));
        }
        if (progress >= config.revolt().captureRequired()) {
            String old = officeService.getRuler();
            officeService.removeRuler(OfficeService.RemovalReason.FORCED);
            freezeTransition();
            broadcast(Text.literal("The capital has fallen. The King has been overthrown by popular revolt.").formatted(Formatting.DARK_RED));
            persistence.history().insert(new com.example.kingdoms.db.model.History(0, "revolt_success", old, null, "{\"capital\":\"" + capitalText() + "\"}", System.currentTimeMillis()));
            if (config.office().electionEnabled()) KingdomsPlugin.getInstance().startElectionAndSchedule(config.office().id());
        }
    }

    private void auditAndMaybeRevolt(String reason) throws SQLException {
        TreasuryState s = state();
        ReserveHealth health = s.reserveHealth();
        int legitimacy = clamp(s.legitimacy() + health.legitimacyDelta() / 2 - s.corruptionHeat() / 35);
        int unrest = clamp(s.unrest() + health.unrestDelta() / 2 + (s.legitimacy() < 35 ? 3 : 0) + (s.corruptionHeat() > 70 ? 4 : 0));
        boolean opens = !s.revoltActive() && unrest >= config.revolt().threshold();
        save(copy(s, s.rawDiamonds(), s.diamondBlocks(), s.currencySupply(), s.taxesCollected(), s.publicSpending(),
            s.treasuryWithdrawals(), s.emergencyMinting(), s.xpTaxRate(), s.tradeTaxRate(), s.resourceTitheRate(), s.emergencyLevyRate(),
            legitimacy, s.corruptionHeat(), unrest, s.revoltActive() || opens, opens ? System.currentTimeMillis() : s.revoltStartedAt(),
            s.captureProgress(), s.transitionFreezeUntil()));
        if (opens) {
            broadcast(Text.literal("REVOLT WINDOW OPEN: unrest has reached " + unrest + "/100. Seize the capital at " + capitalText() + " to overthrow the King.").formatted(Formatting.DARK_RED));
            if (announcementService != null) announcementService.showRevoltBossBar(Text.literal("Revolt Window - Capture the Capital").formatted(Formatting.DARK_RED));
        } else if (health == ReserveHealth.DEBASED || health == ReserveHealth.INSOLVENT) {
            broadcast(Text.literal("Treasury audit: " + health.label() + ". " + reason + " has shaken confidence.").formatted(health.color()));
        }
    }

    private int minimumRebels(MinecraftServer server) {
        int online = server.getPlayerManager().getPlayerList().size();
        return Math.max(1, Math.min(3, (online + 1) / 2));
    }

    public String capitalText() {
        return Math.round(config.revolt().capitalX()) + " " + Math.round(config.revolt().capitalY()) + " " + Math.round(config.revolt().capitalZ());
    }

    private void requireKing(ServerPlayerEntity actor) {
        if (!actor.getUuidAsString().equals(officeService.getRuler())) throw new IllegalStateException("Only the King may use this power.");
    }

    private void broadcast(Text text) {
        if (server != null) server.getPlayerManager().broadcast(text, false);
    }

    private void save(TreasuryState state) throws SQLException {
        persistence.treasury().save(state);
    }

    private int removeItems(ServerPlayerEntity player, net.minecraft.item.Item item, int count) {
        int left = count;
        for (int i = 0; i < player.getInventory().size() && left > 0; i++) {
            var stack = player.getInventory().getStack(i);
            if (!stack.isOf(item)) continue;
            int take = Math.min(left, stack.getCount());
            stack.decrement(take);
            left -= take;
        }
        return count - left;
    }

    private static String ratioText(TreasuryState s) {
        return Math.round(s.reserveRatio() * 100.0) + "%";
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
    private static int clampHeat(int v) { return Math.max(0, Math.min(100, v)); }

    private static TreasuryState copy(TreasuryState s, int raw, int blocks, int supply, int taxes, int publicSpend,
                                      int withdrawals, int emergencyMint, int xp, int trade, int resource, int levy,
                                      int legitimacy, int heat, int unrest, boolean revolt, long revoltAt,
                                      int capture, long freezeUntil) {
        return new TreasuryState(s.officeId(), raw, blocks, supply, taxes, publicSpend, withdrawals, emergencyMint,
            xp, trade, resource, levy, legitimacy, heat, unrest, revolt, revoltAt, capture, freezeUntil, System.currentTimeMillis());
    }
}
