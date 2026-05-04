package com.example.kingdoms.ui;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Centralised display strings. All user-visible text lives here.
 * Uses Minecraft's native Text API with explicit colour formatting.
 */
public final class Messages {

    private Messages() {}

    // ------------------------------------------------------------------
    // Shared UI prefix
    // ------------------------------------------------------------------

    private static final String PREFIX_RAW  = "[Kingdom] ";
    private static final Formatting COLOR_PREFIX = Formatting.GOLD;
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    public static MutableText prefix() {
        return Text.literal(PREFIX_RAW).formatted(COLOR_PREFIX);
    }

    public static MutableText prefixed(MutableText body) {
        return prefix().append(body);
    }

    // ------------------------------------------------------------------
    // Status
    // ------------------------------------------------------------------

    public static MutableText statusHeader() {
        return Text.literal("--- Kingdom Status ---").formatted(Formatting.GOLD, Formatting.BOLD);
    }

    public static MutableText rulerLine(String rulerName) {
        return Text.literal("Ruler: ").formatted(Formatting.YELLOW)
            .append(Text.literal(rulerName).formatted(Formatting.WHITE));
    }

    public static MutableText noRuler() {
        return Text.literal("Ruler: ").formatted(Formatting.YELLOW)
            .append(Text.literal("None").formatted(Formatting.GRAY));
    }

    public static MutableText termEndLine(long epochMs) {
        String date = DATE_FMT.format(Instant.ofEpochMilli(epochMs));
        return Text.literal("Term ends: ").formatted(Formatting.YELLOW)
            .append(Text.literal(date).formatted(Formatting.WHITE));
    }

    public static MutableText phaseLine(String phase) {
        return Text.literal("Phase: ").formatted(Formatting.YELLOW)
            .append(Text.literal(phase == null ? "IDLE" : phase).formatted(Formatting.AQUA));
    }

    // ------------------------------------------------------------------
    // Candidate list
    // ------------------------------------------------------------------

    public static MutableText candidatesHeader(int count) {
        return Text.literal("=== Candidates (" + count + ") ===").formatted(Formatting.GOLD, Formatting.BOLD);
    }

    public static MutableText candidateLine(int index, String name, String slogan) {
        MutableText line = Text.literal((index + 1) + ". ").formatted(Formatting.YELLOW)
            .append(Text.literal(name).formatted(Formatting.WHITE));
        if (slogan != null && !slogan.isBlank()) {
            line.append(Text.literal(" — " + slogan).formatted(Formatting.GRAY));
        }
        return line;
    }

    public static MutableText noCandidates() {
        return prefixed(Text.literal("No candidates have registered yet.").formatted(Formatting.GRAY));
    }

    // ------------------------------------------------------------------
    // Vote feedback
    // ------------------------------------------------------------------

    public static MutableText voteRecorded(String candidateName) {
        return prefixed(
            Text.literal("Vote cast for ").formatted(Formatting.GREEN)
                .append(Text.literal(candidateName).formatted(Formatting.WHITE))
                .append(Text.literal(".").formatted(Formatting.GREEN))
        );
    }

    public static MutableText alreadyVoted() {
        return prefixed(Text.literal("You have already voted in this election.").formatted(Formatting.RED));
    }

    public static MutableText votingNotOpen() {
        return prefixed(Text.literal("Voting is not currently open.").formatted(Formatting.RED));
    }

    public static MutableText notEnoughPlaytime(int have, int need) {
        return prefixed(
            Text.literal("You need at least " + need + " minutes of playtime to vote (you have " + have + ").").formatted(Formatting.RED)
        );
    }

    // ------------------------------------------------------------------
    // Candidacy
    // ------------------------------------------------------------------

    public static MutableText registeredAsCandidate(String slogan) {
        MutableText msg = prefixed(Text.literal("You are now a candidate!").formatted(Formatting.GREEN));
        if (slogan != null && !slogan.isBlank()) {
            msg.append(Text.literal(" Slogan: " + slogan).formatted(Formatting.GRAY));
        }
        return msg;
    }

    public static MutableText alreadyCandidate() {
        return prefixed(Text.literal("You are already registered as a candidate.").formatted(Formatting.RED));
    }

    public static MutableText nominationsNotOpen() {
        return prefixed(Text.literal("Nominations are not currently open.").formatted(Formatting.RED));
    }

    public static MutableText holderCannotRun() {
        return prefixed(Text.literal("The current officeholder cannot run as a candidate.").formatted(Formatting.RED));
    }

    public static MutableText noActiveElection() {
        return prefixed(Text.literal("There is no active election right now.").formatted(Formatting.GRAY));
    }

    // ------------------------------------------------------------------
    // Admin feedback
    // ------------------------------------------------------------------

    public static MutableText electionStarted() {
        return prefixed(Text.literal("Election started.").formatted(Formatting.GREEN));
    }

    public static MutableText electionEnded() {
        return prefixed(Text.literal("Election ended and resolved.").formatted(Formatting.GREEN));
    }

    public static MutableText rulerSet(String name) {
        return prefixed(
            Text.literal("Ruler set to ").formatted(Formatting.GREEN)
                .append(Text.literal(name).formatted(Formatting.WHITE)).append(Text.literal(".").formatted(Formatting.GREEN))
        );
    }

    public static MutableText rulerRemoved() {
        return prefixed(Text.literal("Ruler removed; throne is vacant.").formatted(Formatting.YELLOW));
    }

    public static MutableText phaseSet(String phase) {
        return prefixed(
            Text.literal("Phase set to ").formatted(Formatting.GREEN)
                .append(Text.literal(phase).formatted(Formatting.WHITE)).append(Text.literal(".").formatted(Formatting.GREEN))
        );
    }

    public static MutableText configReloaded() {
        return prefixed(Text.literal("Note: config is loaded once at startup and cannot be hot-reloaded.").formatted(Formatting.YELLOW));
    }

    public static MutableText orbGiven(String playerName) {
        return prefixed(
            Text.literal("Origin orb given to ").formatted(Formatting.GREEN)
                .append(Text.literal(playerName).formatted(Formatting.WHITE)).append(Text.literal(".").formatted(Formatting.GREEN))
        );
    }

    public static MutableText originAssigned(String playerName, String originId) {
        return prefixed(
            Text.literal("Assigned origin ").formatted(Formatting.GREEN)
                .append(Text.literal(originId).formatted(Formatting.AQUA))
                .append(Text.literal(" to ")).append(Text.literal(playerName).formatted(Formatting.WHITE)).append(Text.literal(".").formatted(Formatting.GREEN))
        );
    }

    public static MutableText debugSynced() {
        return prefixed(Text.literal("Origin sync validated for current ruler.").formatted(Formatting.GREEN));
    }

    public static MutableText noRulerToSync() {
        return prefixed(Text.literal("No ruler currently in office.").formatted(Formatting.GRAY));
    }

    public static MutableText error(String message) {
        return prefixed(Text.literal(message).formatted(Formatting.RED));
    }

    public static MutableText playerNotFound(String name) {
        return prefixed(Text.literal("Player not found: " + name).formatted(Formatting.RED));
    }

    public static MutableText noPermission() {
        return prefixed(Text.literal("You do not have permission to do that.").formatted(Formatting.RED));
    }

    // ------------------------------------------------------------------
    // Announcements (broadcast)
    // ------------------------------------------------------------------

    public static MutableText announceElectionStart(String officeName) {
        return Text.literal("[KINGDOM] ").formatted(Formatting.GOLD, Formatting.BOLD)
            .append(Text.literal("An election for " + officeName + " has begun! Nominations are now open.").formatted(Formatting.YELLOW));
    }

    public static MutableText announceVotingOpen(List<String> names) {
        MutableText msg = Text.literal("[KINGDOM] ").formatted(Formatting.GOLD, Formatting.BOLD)
            .append(Text.literal("Voting is now open! Candidates: ").formatted(Formatting.YELLOW));
        msg.append(Text.literal(String.join(", ", names)).formatted(Formatting.WHITE));
        return msg;
    }

    public static MutableText announceWinner(String winnerName) {
        return Text.literal("[KINGDOM] ").formatted(Formatting.GOLD, Formatting.BOLD)
            .append(Text.literal(winnerName).formatted(Formatting.AQUA))
            .append(Text.literal(" has won the election and is now the ruler!").formatted(Formatting.YELLOW));
    }

    public static MutableText announceRulerChange(String oldRuler, String newRuler) {
        if (newRuler == null) {
            // Ruler removed; throne vacant
            return Text.literal("[KINGDOM] ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("The throne is now vacant").formatted(Formatting.YELLOW))
                .append(oldRuler != null
                    ? Text.literal(" — " + oldRuler + "'s term has ended.").formatted(Formatting.GRAY)
                    : Text.literal(".").formatted(Formatting.YELLOW));
        }
        return Text.literal("[KINGDOM] ").formatted(Formatting.GOLD, Formatting.BOLD)
            .append(Text.literal(newRuler).formatted(Formatting.AQUA))
            .append(Text.literal(" has taken the throne").formatted(Formatting.YELLOW))
            .append(oldRuler != null
                ? Text.literal(" from " + oldRuler).formatted(Formatting.GRAY)
                : Text.literal("").formatted(Formatting.YELLOW))
            .append(Text.literal("!").formatted(Formatting.YELLOW));
    }

    public static MutableText announceRoyalDecree(String rulerName, String message) {
        return Text.literal("[ROYAL DECREE] ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
            .append(Text.literal(rulerName + ": ").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.literal(message).formatted(Formatting.WHITE));
    }

    // ------------------------------------------------------------------
    // GUI labels (items in inventory menus)
    // ------------------------------------------------------------------

    public static MutableText guiTitle(String name) {
        return Text.literal(name).formatted(Formatting.DARK_AQUA, Formatting.BOLD);
    }

    public static MutableText guiVoteButton() {
        return Text.literal("Vote").formatted(Formatting.GREEN, Formatting.BOLD);
    }

    public static MutableText guiViewCandidatesButton() {
        return Text.literal("View Candidates").formatted(Formatting.AQUA);
    }

    public static MutableText guiRulerPanelButton() {
        return Text.literal("Ruler Panel").formatted(Formatting.GOLD, Formatting.BOLD);
    }

    public static MutableText guiHistoryButton() {
        return Text.literal("History").formatted(Formatting.YELLOW);
    }

    public static MutableText guiConfirmVote() {
        return Text.literal("Confirm Vote").formatted(Formatting.GREEN, Formatting.BOLD);
    }

    public static MutableText guiCancel() {
        return Text.literal("Cancel").formatted(Formatting.RED, Formatting.BOLD);
    }

    public static MutableText guiBroadcastSpeech() {
        return Text.literal("Broadcast Speech").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
    }

    public static MutableText guiComingSoon() {
        return Text.literal("Coming Soon").formatted(Formatting.GRAY);
    }

    public static MutableText guiPhaseNotice(String phase) {
        return Text.literal("Voting is not open (phase: " + (phase == null ? "IDLE" : phase) + ")").formatted(Formatting.GRAY);
    }

    // ------------------------------------------------------------------
    // Chat prompts
    // ------------------------------------------------------------------

    public static MutableText speechPrompt() {
        return prefixed(
            Text.literal("Type your royal decree in chat. Your next message will be broadcast server-wide.").formatted(Formatting.LIGHT_PURPLE)
        );
    }

    public static MutableText speechCancelled() {
        return prefixed(Text.literal("Royal speech cancelled.").formatted(Formatting.GRAY));
    }

    // ------------------------------------------------------------------
    // Help
    // ------------------------------------------------------------------

    public static MutableText helpHeader() {
        return Text.literal("=== Kingdom Commands ===").formatted(Formatting.GOLD, Formatting.BOLD);
    }

    public static MutableText helpLine(String cmd, String desc) {
        return Text.literal("  " + cmd).formatted(Formatting.YELLOW)
            .append(Text.literal(" — " + desc).formatted(Formatting.GRAY));
    }
}
