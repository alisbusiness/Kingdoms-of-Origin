package com.example.kingdoms.ui.gui;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.db.model.Candidate;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.election.ElectionPhase;
import com.example.kingdoms.election.ElectionService;
import com.example.kingdoms.origin.OfficeService;
import com.example.kingdoms.ui.AnnouncementService;
import com.example.kingdoms.ui.Messages;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Builds and opens inventory GUI menus for political interactions.
 * Each open* method constructs an inventory, populates it, and opens it for the player.
 */
public final class GuiService {

    private final ElectionService electionService;
    private final OfficeService officeService;
    private final AnnouncementService announcementService;
    private final KingdomsPlugin plugin;

    public GuiService(
        ElectionService electionService,
        OfficeService officeService,
        AnnouncementService announcementService,
        KingdomsPlugin plugin
    ) {
        this.electionService    = electionService;
        this.officeService      = officeService;
        this.announcementService = announcementService;
        this.plugin             = plugin;
    }

    // ------------------------------------------------------------------
    // a. Main politics menu
    // ------------------------------------------------------------------

    public void openMainMenu(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(9 * 3);
        String rulerUuid  = officeService.getRuler();
        String phase      = officeService.getPhase();
        long   termEnd    = officeService.getTermEnd();

        // Slot 4 — current ruler skull
        if (rulerUuid != null) {
            MinecraftServer server = player.getServer();
            ServerPlayerEntity rulerPlayer = server != null
                ? server.getPlayerManager().getPlayer(UUID.fromString(rulerUuid)) : null;
            String rulerName = rulerPlayer != null ? rulerPlayer.getName().getString() : rulerUuid;
            inv.setStack(4, headStack(rulerUuid, rulerName,
                List.of(Messages.termEndLine(termEnd), Messages.phaseLine(phase))));
        } else {
            inv.setStack(4, namedStack(Items.BARRIER,
                Text.literal("No Ruler").formatted(Formatting.GRAY),
                List.of(Text.literal("The throne is vacant.").formatted(Formatting.DARK_GRAY))));
        }

        // Slot 11 — Vote button
        ElectionPhase electionPhase = ElectionPhase.fromString(phase);
        if (electionPhase == ElectionPhase.VOTING) {
            inv.setStack(11, namedStack(Items.LIME_STAINED_GLASS_PANE,
                Messages.guiVoteButton(),
                List.of(Text.literal("Click to see candidates and vote.").formatted(Formatting.GRAY))));
        } else {
            inv.setStack(11, namedStack(Items.GRAY_STAINED_GLASS_PANE,
                Messages.guiVoteButton(),
                List.of(Messages.guiPhaseNotice(phase))));
        }

        // Slot 13 — View Candidates
        inv.setStack(13, namedStack(Items.BOOK,
            Messages.guiViewCandidatesButton(),
            List.of(Text.literal("See who is running.").formatted(Formatting.GRAY))));

        // Slot 15 — Ruler Panel (only for current ruler)
        if (rulerUuid != null && rulerUuid.equals(player.getUuidAsString())) {
            inv.setStack(15, namedStack(Items.GOLDEN_HELMET,
                Messages.guiRulerPanelButton(),
                List.of(Text.literal("Access your administrative panel.").formatted(Formatting.GRAY))));
        }

        // Slot 17 — History placeholder
        inv.setStack(17, namedStack(Items.WRITABLE_BOOK,
            Messages.guiHistoryButton(),
            List.of(Text.literal("View kingdom history.").formatted(Formatting.GRAY),
                    Text.literal("(Coming soon)").formatted(Formatting.DARK_GRAY))));

        openScreen(player, inv, 3, Messages.guiTitle("Kingdom Politics"), (slot, clicker) -> {
            switch (slot) {
                case 11 -> {
                    if (ElectionPhase.fromString(officeService.getPhase()) == ElectionPhase.VOTING) {
                        openCandidateList(clicker);
                    } else {
                        clicker.sendMessage(Messages.votingNotOpen(), false);
                    }
                }
                case 13 -> openCandidateList(clicker);
                case 15 -> {
                    if (officeService.getRuler() != null
                            && officeService.getRuler().equals(clicker.getUuidAsString())) {
                        openRulerPanel(clicker);
                    }
                }
                default -> {}
            }
        });
    }

    // ------------------------------------------------------------------
    // b. Candidate list menu
    // ------------------------------------------------------------------

    public void openCandidateList(ServerPlayerEntity player) {
        List<Candidate> candidates = loadCandidates();
        int rows = Math.max(1, (int) Math.ceil((candidates.size() + 1) / 9.0));
        rows = Math.min(rows, 6);

        SimpleInventory inv = new SimpleInventory(rows * 9);
        for (int i = 0; i < candidates.size() && i < rows * 9; i++) {
            Candidate c = candidates.get(i);
            String name   = resolvePlayerName(player, c.getPlayerUuid());
            String slogan = c.getSlogan();
            List<Text> lore = slogan != null && !slogan.isBlank()
                ? List.of(Text.literal(slogan).formatted(Formatting.GRAY))
                : List.of();
            inv.setStack(i, headStack(c.getPlayerUuid(), name, lore));
        }

        if (candidates.isEmpty()) {
            inv.setStack(4, namedStack(Items.BARRIER,
                Text.literal("No candidates yet.").formatted(Formatting.GRAY),
                List.of()));
        }

        openScreen(player, inv, rows, Messages.guiTitle("Candidates"), (slot, clicker) -> {
            if (slot < candidates.size()) {
                openVoteConfirmation(clicker, candidates.get(slot));
            }
        });
    }

    // ------------------------------------------------------------------
    // c. Vote confirmation menu
    // ------------------------------------------------------------------

    public void openVoteConfirmation(ServerPlayerEntity player, Candidate candidate) {
        SimpleInventory inv = new SimpleInventory(9);

        String name = resolvePlayerName(player, candidate.getPlayerUuid());
        String slogan = candidate.getSlogan();
        List<Text> lore = slogan != null && !slogan.isBlank()
            ? List.of(Text.literal(slogan).formatted(Formatting.ITALIC))
            : List.of();

        inv.setStack(4, headStack(candidate.getPlayerUuid(), name, lore));

        inv.setStack(2, namedStack(Items.LIME_STAINED_GLASS_PANE,
            Messages.guiConfirmVote(),
            List.of(Text.literal("Confirm your vote for " + name + ".").formatted(Formatting.GRAY))));

        inv.setStack(6, namedStack(Items.RED_STAINED_GLASS_PANE,
            Messages.guiCancel(),
            List.of(Text.literal("Go back.").formatted(Formatting.GRAY))));

        openScreen(player, inv, 1, Messages.guiTitle("Confirm Vote"), (slot, clicker) -> {
            if (slot == 2) {
                clicker.closeHandledScreen();
                castVoteFor(clicker, candidate);
            } else if (slot == 6) {
                clicker.closeHandledScreen();
                openCandidateList(clicker);
            }
        });
    }

    // ------------------------------------------------------------------
    // d. Ruler panel menu
    // ------------------------------------------------------------------

    public void openRulerPanel(ServerPlayerEntity player) {
        if (!player.getUuidAsString().equals(officeService.getRuler())) {
            player.sendMessage(Messages.noPermission(), false);
            return;
        }

        SimpleInventory inv = new SimpleInventory(9 * 3);
        String rulerName = player.getName().getString();
        long   termEnd   = officeService.getTermEnd();
        String originId  = plugin.getConfig().originMode().kingOriginId();

        // Slot 4 — ruler info skull
        inv.setStack(4, headStack(player.getUuidAsString(), rulerName,
            List.of(
                Messages.termEndLine(termEnd),
                Text.literal("Origin: " + originId).formatted(Formatting.AQUA)
            )));

        // Slot 11 — Broadcast Speech
        inv.setStack(11, namedStack(Items.PAPER,
            Messages.guiBroadcastSpeech(),
            List.of(Text.literal("Broadcast a message to the entire server.").formatted(Formatting.GRAY))));

        // Placeholder decree slots 13, 14, 15, 16
        for (int slot : new int[]{13, 14, 15, 16}) {
            inv.setStack(slot, namedStack(Items.GRAY_STAINED_GLASS_PANE,
                Messages.guiComingSoon(),
                List.of(Text.literal("Future decree options.").formatted(Formatting.DARK_GRAY))));
        }

        openScreen(player, inv, 3, Messages.guiTitle("Ruler Panel"), (slot, clicker) -> {
            if (slot == 11) {
                clicker.closeHandledScreen();
                plugin.getEventListeners().setPendingSpeech(clicker.getUuid());
                clicker.sendMessage(Messages.speechPrompt(), false);
            }
        });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void openScreen(
        ServerPlayerEntity player,
        SimpleInventory inv,
        int rows,
        Text title,
        java.util.function.BiConsumer<Integer, ServerPlayerEntity> clickHandler
    ) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new KingdomScreenHandler(
                syncId, playerInv, inv, rows,
                (slot, entity) -> {
                    if (entity instanceof ServerPlayerEntity sp) clickHandler.accept(slot, sp);
                }
            ),
            title
        ));
    }

    private void castVoteFor(ServerPlayerEntity player, Candidate candidate) {
        try {
            String officeId = plugin.getConfig().office().id();
            electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                int playtimeTicks = player.getStatHandler()
                    .getStat(net.minecraft.stat.Stats.CUSTOM.getOrCreateStat(net.minecraft.stat.Stats.PLAY_TIME));
                int playtimeMinutes = playtimeTicks / 20 / 60;

                try {
                    electionService.castVote(election.getId(), player.getUuidAsString(),
                        candidate.getPlayerUuid(), playtimeMinutes);
                    String name = resolvePlayerName(player, candidate.getPlayerUuid());
                    player.sendMessage(Messages.voteRecorded(name), false);
                } catch (ElectionException e) {
                    player.sendMessage(Messages.error(e.getMessage()), false);
                } catch (SQLException e) {
                    KingdomsPlugin.LOGGER.error("Vote failed", e);
                    player.sendMessage(Messages.error("A server error occurred."), false);
                }
            }, () -> player.sendMessage(Messages.noActiveElection(), false));
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Vote lookup failed", e);
            player.sendMessage(Messages.error("A server error occurred."), false);
        }
    }

    private List<Candidate> loadCandidates() {
        try {
            String officeId = plugin.getConfig().office().id();
            return electionService.getCurrentElection(officeId)
                .map(election -> {
                    try { return electionService.getCandidates(election.getId()); }
                    catch (SQLException e) { return List.<Candidate>of(); }
                })
                .orElse(List.of());
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Failed to load candidates", e);
            return List.of();
        }
    }

    private String resolvePlayerName(ServerPlayerEntity requester, String uuid) {
        MinecraftServer server = requester.getServer();
        if (server == null) return uuid;
        try {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
            if (online != null) return online.getName().getString();
            // Fall back to DB username
            return plugin.getPersistence().players().findByUuid(uuid)
                .map(p -> p.getUsername() != null ? p.getUsername() : uuid)
                .orElse(uuid);
        } catch (Exception e) {
            return uuid;
        }
    }

    // ------------------------------------------------------------------
    // Item builders
    // ------------------------------------------------------------------

    /** Creates an ItemStack with a custom name and lore. */
    private static ItemStack namedStack(net.minecraft.item.Item item, Text name, List<Text> lore) {
        ItemStack stack = new ItemStack(item);
        stack.setCustomName(name);
        if (!lore.isEmpty()) addLore(stack, lore);
        return stack;
    }

    /** Creates a PLAYER_HEAD with the given owner UUID and display name. */
    private static ItemStack headStack(String ownerUuid, String displayName, List<Text> lore) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.setCustomName(Text.literal(displayName).formatted(Formatting.YELLOW));

        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound skullOwner = new NbtCompound();
        skullOwner.putString("Name", displayName);
        try {
            UUID uuid = UUID.fromString(ownerUuid);
            skullOwner.put("Id", new NbtIntArray(uuidToIntArray(uuid)));
        } catch (IllegalArgumentException ignored) {}
        nbt.put("SkullOwner", skullOwner);

        if (!lore.isEmpty()) addLore(stack, lore);
        return stack;
    }

    private static void addLore(ItemStack stack, List<Text> lore) {
        NbtList loreList = new NbtList();
        for (Text line : lore) {
            loreList.add(NbtString.of(Text.Serializer.toJson(line)));
        }
        stack.getOrCreateSubNbt("display").put("Lore", loreList);
    }

    private static int[] uuidToIntArray(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return new int[]{(int) (msb >> 32), (int) msb, (int) (lsb >> 32), (int) lsb};
    }
}
