package com.example.kingdoms.ui;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.config.ConfigLoader;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Sends server-wide election and ruler announcements via chat and optional boss bar.
 * Which channels are used is controlled by the ui section in config.yml.
 */
public final class AnnouncementService {

    private final ConfigLoader config;
    private MinecraftServer server;

    private ServerBossBar electionBossBar;

    public AnnouncementService(ConfigLoader config) {
        this.config = config;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ------------------------------------------------------------------
    // Public broadcast methods
    // ------------------------------------------------------------------

    public void broadcastElectionStart(String officeName) {
        Text msg = Messages.announceElectionStart(officeName);
        broadcastChat(msg);

        if (config.ui().useBossbarDuringElection()) {
            showElectionBossBar(Text.literal("Election in Progress — Nominations Open")
                .formatted(net.minecraft.util.Formatting.GOLD));
        }
    }

    public void broadcastVotingOpen(List<String> candidateNames) {
        broadcastChat(Messages.announceVotingOpen(candidateNames));

        if (electionBossBar != null) {
            electionBossBar.setName(Text.literal("Election — VOTING OPEN")
                .formatted(net.minecraft.util.Formatting.GREEN));
            electionBossBar.setColor(BossBar.Color.GREEN);
        }
    }

    public void broadcastWinner(String winnerName) {
        broadcastChat(Messages.announceWinner(winnerName));
        dismissElectionBossBar();
    }

    public void broadcastRulerChange(String oldRuler, String newRuler) {
        broadcastChat(Messages.announceRulerChange(oldRuler, newRuler));
    }

    public void broadcastSpeech(String rulerName, String message) {
        broadcastChat(Messages.announceRoyalDecree(rulerName, message));
    }

    // ------------------------------------------------------------------
    // Boss bar lifecycle
    // ------------------------------------------------------------------

    private void showElectionBossBar(Text title) {
        if (server == null) return;
        dismissElectionBossBar();
        electionBossBar = new ServerBossBar(title, BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
        server.getPlayerManager().getPlayerList().forEach(electionBossBar::addPlayer);
    }

    public void addPlayerToBossBar(net.minecraft.server.network.ServerPlayerEntity player) {
        if (electionBossBar != null) electionBossBar.addPlayer(player);
    }

    public void dismissElectionBossBar() {
        if (electionBossBar != null) {
            electionBossBar.clearPlayers();
            electionBossBar = null;
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void broadcastChat(Text message) {
        if (!config.ui().sendChatBroadcasts()) return;
        if (server == null) {
            KingdomsPlugin.LOGGER.warn("AnnouncementService.broadcastChat called before server was set");
            return;
        }
        server.getPlayerManager().broadcast(message, false);
    }
}
