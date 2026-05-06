package com.example.kingdoms.ui.gui;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.db.model.Candidate;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.election.ElectionPhase;
import com.example.kingdoms.election.ElectionService;
import com.example.kingdoms.origin.OfficeService;
import com.example.kingdoms.perk.PerkCategory;
import com.example.kingdoms.perk.PerkDefinition;
import com.example.kingdoms.perk.PerkRegistry;
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
    
    private final java.util.Map<UUID, java.util.Set<String>> activePerkSelections = new java.util.concurrent.ConcurrentHashMap<>();

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

        // Slot 14 — Run for Office or Manage Promises
        if (electionPhase == ElectionPhase.NOMINATION || electionPhase == ElectionPhase.CAMPAIGN) {
            boolean isCandidate = false;
            try {
                String officeId = plugin.getConfig().office().id();
                var electionOpt = electionService.getCurrentElection(officeId);
                if (electionOpt.isPresent()) {
                    long electionId = electionOpt.get().getId();
                    isCandidate = plugin.getPersistence().candidates()
                        .findByElectionAndPlayer(electionId, player.getUuidAsString()).isPresent();
                }
            } catch (SQLException ignored) {}

            if (isCandidate) {
                inv.setStack(14, namedStack(Items.WRITABLE_BOOK,
                    Text.literal("Manage Promises").formatted(Formatting.GOLD, Formatting.BOLD),
                    List.of(Text.literal("Update your campaign promises.").formatted(Formatting.GRAY))));
            } else {
                inv.setStack(14, namedStack(Items.PAPER,
                    Text.literal("Run for Office").formatted(Formatting.GOLD, Formatting.BOLD),
                    List.of(Text.literal("Register as a candidate.").formatted(Formatting.GRAY))));
            }
        }

        // Slot 17 — History
        inv.setStack(17, namedStack(Items.WRITABLE_BOOK,
            Messages.guiHistoryButton(),
            List.of(Text.literal("View kingdom history.").formatted(Formatting.GRAY))));

        inv.setStack(10, namedStack(Items.DIAMOND,
            Text.literal("Treasury").formatted(Formatting.AQUA, Formatting.BOLD),
            List.of(Text.literal("View reserves, Crowns, taxes, and unrest.").formatted(Formatting.GRAY))));

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
                case 10 -> openTreasuryMenu(clicker);
                case 14 -> {
                    if (ElectionPhase.fromString(officeService.getPhase()) == ElectionPhase.NOMINATION ||
                        ElectionPhase.fromString(officeService.getPhase()) == ElectionPhase.CAMPAIGN) {
                        
                        boolean isCandidate = false;
                        try {
                            String officeId = plugin.getConfig().office().id();
                            var electionOpt = electionService.getCurrentElection(officeId);
                            if (electionOpt.isPresent()) {
                                isCandidate = plugin.getPersistence().candidates()
                                    .findByElectionAndPlayer(electionOpt.get().getId(), clicker.getUuidAsString()).isPresent();
                            }
                        } catch (SQLException ignored) {}
                        
                        if (isCandidate) {
                            openPerkSelectionMenu(clicker, false, 0);
                        } else {
                            clicker.closeHandledScreen();
                            MinecraftServer server = clicker.getServer();
                            if (server != null) {
                                server.getCommandManager().executeWithPrefix(clicker.getCommandSource(), "kingdom run");
                            }
                        }
                    }
                }
                case 15 -> {
                    if (officeService.getRuler() != null
                            && officeService.getRuler().equals(clicker.getUuidAsString())) {
                        openRulerPanel(clicker);
                    }
                }
                case 17 -> openHistoryMenu(clicker, 0);
                default -> {}
            }
        });
    }

    public void openTreasuryMenu(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(27);
        try {
            var s = plugin.getTreasuryService().state();
            int balance = plugin.getTreasuryService().balance(player.getUuidAsString());
            inv.setStack(4, namedStack(Items.DIAMOND_BLOCK,
                Text.literal("Diamond Reserves").formatted(s.reserveHealth().color(), Formatting.BOLD),
                List.of(
                    Text.literal(s.rawDiamonds() + " diamonds, " + s.diamondBlocks() + " diamond blocks").formatted(Formatting.WHITE),
                    Text.literal(s.reserveValue() + " diamond-equivalent reserve").formatted(Formatting.AQUA),
                    Text.literal("State: " + s.reserveHealth().label()).formatted(s.reserveHealth().color())
                )));
            String currency = plugin.getTreasuryService().currencyName();
            inv.setStack(10, namedStack(Items.PAPER,
                Text.literal(currency).formatted(Formatting.GOLD, Formatting.BOLD),
                List.of(
                    Text.literal("Supply: " + s.currencySupply()).formatted(Formatting.WHITE),
                    Text.literal("Reserve ratio: " + Math.round(s.reserveRatio() * 100.0) + "%").formatted(s.reserveHealth().color()),
                    Text.literal("Your balance: " + balance).formatted(Formatting.AQUA)
                )));
            inv.setStack(12, namedStack(Items.WRITABLE_BOOK,
                Text.literal("Tax Policy").formatted(Formatting.YELLOW, Formatting.BOLD),
                List.of(
                    Text.literal("XP: " + s.xpTaxRate() + "%").formatted(Formatting.GRAY),
                    Text.literal("Trade: " + s.tradeTaxRate() + "%").formatted(Formatting.GRAY),
                    Text.literal("Resource tithe: " + s.resourceTitheRate() + "%").formatted(Formatting.GRAY),
                    Text.literal("Emergency levy: " + s.emergencyLevyRate() + "%").formatted(Formatting.RED)
                )));
            inv.setStack(14, namedStack(Items.REDSTONE,
                Text.literal("Political Stability").formatted(s.unrestBand().color(), Formatting.BOLD),
                List.of(
                    Text.literal("Legitimacy: " + s.legitimacy() + "/100").formatted(Formatting.GREEN),
                    Text.literal("Corruption Heat: " + s.corruptionHeat() + "/100").formatted(Formatting.DARK_RED),
                    Text.literal("Unrest: " + s.unrest() + "/100 (" + s.unrestBand().label() + ")").formatted(s.unrestBand().color()),
                    Text.literal("Revolt: " + (s.revoltActive() ? "OPEN" : "closed")).formatted(s.revoltActive() ? Formatting.DARK_RED : Formatting.GRAY)
                )));
            inv.setStack(16, namedStack(Items.BELL,
                Text.literal("Public Ledger").formatted(Formatting.AQUA, Formatting.BOLD),
                plugin.getPersistence().treasury().recentPublicLedger(plugin.getConfig().office().id(), 5).stream()
                    .<Text>map(line -> Text.literal(line).formatted(Formatting.GRAY))
                    .toList()));
            if (player.getUuidAsString().equals(officeService.getRuler())) {
                inv.setStack(22, namedStack(Items.GOLD_INGOT,
                    Text.literal("Fiscal Controls").formatted(Formatting.GOLD, Formatting.BOLD),
                    List.of(
                        Text.literal("Use /kingdom treasury tax <channel> <rate>").formatted(Formatting.GRAY),
                        Text.literal("Use /kingdom treasury mint <amount>").formatted(Formatting.GRAY),
                        Text.literal("Use /kingdom treasury spend <category> <amount>").formatted(Formatting.GRAY)
                    )));
            }
        } catch (SQLException e) {
            inv.setStack(13, namedStack(Items.BARRIER, Text.literal("Treasury unavailable").formatted(Formatting.RED), List.of()));
        }
        openScreen(player, inv, 3, Text.literal("Kingdom Treasury"), (slot, clicker) -> {});
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
            String perks = c.getPromisedPerks();
            
            java.util.List<Text> lore = new java.util.ArrayList<>();
            if (slogan != null && !slogan.isBlank()) {
                lore.add(Text.literal(slogan).formatted(Formatting.GRAY));
            }
            if (perks != null && !perks.isBlank()) {
                lore.add(Text.literal("Promises: " + perks).formatted(Formatting.AQUA));
            }
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
        String perks = candidate.getPromisedPerks();
        
        java.util.List<Text> lore = new java.util.ArrayList<>();
        if (slogan != null && !slogan.isBlank()) {
            lore.add(Text.literal(slogan).formatted(Formatting.ITALIC));
        }
        if (perks != null && !perks.isBlank()) {
            lore.add(Text.literal("Promises: " + perks).formatted(Formatting.AQUA));
        }

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

        // Slot 13 — Set Active Perks
        inv.setStack(13, namedStack(Items.EMERALD,
            Text.literal("Set Active Policies").formatted(Formatting.GREEN),
            List.of(Text.literal("Spend up to 20 Policy Points.").formatted(Formatting.GRAY))));

        // Slot 14 — Abdicate
        inv.setStack(14, namedStack(Items.IRON_DOOR,
            Text.literal("Step Down").formatted(Formatting.RED, Formatting.BOLD),
            List.of(Text.literal("Abdicate the throne.").formatted(Formatting.GRAY))));

        // Slot 15 — Start Election
        inv.setStack(15, namedStack(Items.BEACON,
            Text.literal("Start Election").formatted(Formatting.GOLD, Formatting.BOLD),
            List.of(Text.literal("Begin a new election cycle.").formatted(Formatting.GRAY))));

        // Slot 16 — Empty
        inv.setStack(16, namedStack(Items.GRAY_STAINED_GLASS_PANE,
            Text.literal(""),
            List.of()));

        openScreen(player, inv, 3, Messages.guiTitle("Ruler Panel"), (slot, clicker) -> {
            if (slot == 11) {
                clicker.closeHandledScreen();
                plugin.getEventListeners().setPendingSpeech(clicker.getUuid());
                clicker.sendMessage(Messages.speechPrompt(), false);
            } else if (slot == 13) {
                openPerkSelectionMenu(clicker, true, 0);
            } else if (slot == 14) {
                clicker.closeHandledScreen();
                try {
                    officeService.removeRuler(com.example.kingdoms.origin.OfficeService.RemovalReason.ABDICATION);
                    clicker.sendMessage(Text.literal("You have abdicated the throne.").formatted(Formatting.YELLOW), false);
                } catch (SQLException e) {
                    clicker.sendMessage(Messages.error("A server error occurred."), false);
                }
            } else if (slot == 15) {
                clicker.closeHandledScreen();
                MinecraftServer server = clicker.getServer();
                if (server != null) {
                    server.getCommandManager().executeWithPrefix(clicker.getCommandSource(), "kingdom start-election");
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // e. Perk selection menu
    // ------------------------------------------------------------------

    public void openPerkSelectionMenu(ServerPlayerEntity player, boolean isSetPerks, int page) {
        java.util.Set<String> selected = activePerkSelections.computeIfAbsent(player.getUuid(), k -> new java.util.HashSet<>());
        java.util.List<PerkDefinition> perks = PerkRegistry.all();
        int totalPerks = perks.size();
        int perksPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalPerks / perksPerPage);
        
        SimpleInventory inv = new SimpleInventory(54);
        
        int startIndex = page * perksPerPage;
        int endIndex = Math.min(startIndex + perksPerPage, totalPerks);
        java.util.Set<PerkCategory> selectedCategories = selected.stream()
            .map(id -> PerkRegistry.find(id).map(PerkDefinition::category).orElse(null))
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
        
        for (int i = startIndex; i < endIndex; i++) {
            PerkDefinition perk = perks.get(i);
            boolean isSelected = selected.contains(perk.id());
            boolean blockedByCategory = !isSelected && selectedCategories.contains(perk.category());
            
            ItemStack stack = namedStack(blockedByCategory ? Items.GRAY_DYE : (perk.category() == PerkCategory.CORRUPTION ? Items.REDSTONE : perk.icon()),
                Text.literal(perk.name()).formatted(perk.category() == PerkCategory.CORRUPTION ? Formatting.RED : (isSelected ? Formatting.GREEN : Formatting.YELLOW)),
                List.of(
                    Text.literal(perk.id()).formatted(Formatting.DARK_GRAY),
                    Text.literal(perk.category().displayName() + " - " + perk.kind().label() + " (" + perk.cost() + " points)").formatted(Formatting.AQUA),
                    Text.literal(perk.description()).formatted(Formatting.GRAY),
                    Text.literal(blockedByCategory ? "Category already selected" : (isSelected ? "Click to deselect" : "Click to select")).formatted(blockedByCategory ? Formatting.RED : Formatting.WHITE)
                ));
            if (isSelected) {
                stack.addEnchantment(net.minecraft.enchantment.Enchantments.UNBREAKING, 1);
                stack.getOrCreateNbt().putInt("HideFlags", 1); // Hide enchantments
            }
            inv.setStack(i - startIndex, stack);
        }
        
        // Navigation & Confirmation
        if (page > 0) {
            inv.setStack(45, namedStack(Items.ARROW, Text.literal("Previous Page").formatted(Formatting.WHITE), List.of()));
        }
        if (page < totalPages - 1) {
            inv.setStack(53, namedStack(Items.ARROW, Text.literal("Next Page").formatted(Formatting.WHITE), List.of()));
        }
        
        inv.setStack(48, namedStack(Items.REDSTONE_BLOCK, 
            Text.literal("Clear Selection").formatted(Formatting.RED),
            List.of(Text.literal("Remove all selected perks.").formatted(Formatting.GRAY))));
            
        inv.setStack(49, namedStack(Items.EMERALD_BLOCK, 
            Text.literal("Confirm Selection").formatted(Formatting.GREEN),
            List.of(
                Text.literal("Selected: " + selected.size() + "/5 categories").formatted(Formatting.GRAY),
                Text.literal("Remaining Policy Points: " + plugin.getPerkService().remainingPoints(new java.util.ArrayList<>(selected))).formatted(Formatting.AQUA)
            )));
            
        String titleType = isSetPerks ? "Set Perks" : "Promised Perks";
        
        openScreen(player, inv, 6, Text.literal(titleType + " (Page " + (page + 1) + "/" + totalPages + ")"), (slot, clicker) -> {
            if (slot < 45) {
                int perkIndex = startIndex + slot;
                if (perkIndex < totalPerks) {
                    PerkDefinition perk = perks.get(perkIndex);
                    if (selected.contains(perk.id())) {
                        selected.remove(perk.id());
                    } else {
                        boolean categoryTaken = selected.stream()
                            .map(id -> PerkRegistry.find(id).map(PerkDefinition::category).orElse(null))
                            .anyMatch(category -> category == perk.category());
                        java.util.List<String> next = new java.util.ArrayList<>(selected);
                        next.add(perk.id());
                        var budget = PerkRegistry.validateBudget(next);
                        if (categoryTaken) {
                            clicker.sendMessage(Text.literal("Only one " + perk.category().displayName() + " policy may be active.").formatted(Formatting.RED), false);
                        } else if (!budget.valid()) {
                            clicker.sendMessage(Text.literal(String.join(" ", budget.errors())).formatted(Formatting.RED), false);
                        } else {
                            selected.add(perk.id());
                        }
                    }
                    openPerkSelectionMenu(clicker, isSetPerks, page);
                }
            } else if (slot == 45 && page > 0) {
                openPerkSelectionMenu(clicker, isSetPerks, page - 1);
            } else if (slot == 53 && page < totalPages - 1) {
                openPerkSelectionMenu(clicker, isSetPerks, page + 1);
            } else if (slot == 48) {
                selected.clear();
                openPerkSelectionMenu(clicker, isSetPerks, page);
            } else if (slot == 49) {
                clicker.closeHandledScreen();
                String perksStr = PerkRegistry.serialize(new java.util.ArrayList<>(selected));
                if (perksStr.isEmpty()) {
                    clicker.sendMessage(Text.literal("No policies selected.").formatted(Formatting.YELLOW), false);
                    activePerkSelections.remove(clicker.getUuid());
                    return;
                }
                if (isSetPerks) {
                    openPerkConfirmation(clicker, perksStr);
                } else {
                    MinecraftServer server = clicker.getServer();
                    if (server != null) server.getCommandManager().executeWithPrefix(clicker.getCommandSource(), "kingdom promise " + perksStr);
                    activePerkSelections.remove(clicker.getUuid());
                }
            }
        });
    }

    public void openActivePerksMenu(ServerPlayerEntity player) {
        java.util.List<String> ids = PerkRegistry.parseIds(officeService.getActivePerks());
        SimpleInventory inv = new SimpleInventory(27);
        int slot = 0;
        for (String id : ids) {
            PerkDefinition perk = PerkRegistry.find(id).orElse(null);
            if (perk == null || slot >= 26) continue;
            inv.setStack(slot++, namedStack(perk.icon(), Text.literal(perk.name()).formatted(Formatting.GOLD), List.of(
                Text.literal(perk.category().displayName() + " - " + perk.kind().label()).formatted(Formatting.AQUA),
                Text.literal(perk.description()).formatted(Formatting.GRAY)
            )));
        }
        if (ids.isEmpty()) inv.setStack(13, namedStack(Items.BARRIER, Text.literal("No active policies").formatted(Formatting.GRAY), List.of()));
        try {
            String ruler = officeService.getRuler();
            int trust = ruler == null ? 0 : plugin.getPersistence().trust().getScore(ruler);
            inv.setStack(26, namedStack(Items.NAME_TAG, Text.literal("King Trust: " + trust + "/100").formatted(Formatting.GREEN), List.of()));
        } catch (SQLException ignored) {}
        openScreen(player, inv, 3, Text.literal("Active Kingdom Policies"), (slotId, clicker) -> {});
    }

    private void openPerkConfirmation(ServerPlayerEntity player, String perksStr) {
        java.util.List<String> ids = PerkRegistry.parseIds(perksStr);
        var budget = PerkRegistry.validateBudget(ids);
        SimpleInventory inv = new SimpleInventory(9);
        inv.setStack(2, namedStack(Items.LIME_CONCRETE, Text.literal("Confirm Policies").formatted(Formatting.GREEN), List.of(
            Text.literal("Remaining Policy Points: " + budget.remaining()).formatted(Formatting.AQUA),
            Text.literal(perksStr).formatted(Formatting.GRAY)
        )));
        inv.setStack(6, namedStack(Items.RED_CONCRETE, Text.literal("Cancel").formatted(Formatting.RED), List.of()));
        openScreen(player, inv, 1, Text.literal("Confirm Policy Decree"), (slot, clicker) -> {
            if (slot == 2) {
                clicker.closeHandledScreen();
                MinecraftServer server = clicker.getServer();
                if (server != null) server.getCommandManager().executeWithPrefix(clicker.getCommandSource(), "kingdom setperks " + perksStr);
                activePerkSelections.remove(clicker.getUuid());
            } else if (slot == 6) {
                openPerkSelectionMenu(clicker, true, 0);
            }
        });
    }

    // ------------------------------------------------------------------
    // f. History menu
    // ------------------------------------------------------------------

    public void openHistoryMenu(ServerPlayerEntity player, int page) {
        List<com.example.kingdoms.db.model.History> historyList = new java.util.ArrayList<>();
        try {
            historyList = plugin.getPersistence().history().findRecent(100);
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Failed to load history", e);
        }

        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) historyList.size() / itemsPerPage));
        
        SimpleInventory inv = new SimpleInventory(54);
        
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, historyList.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            com.example.kingdoms.db.model.History h = historyList.get(i);
            
            String eventType = h.getEventType();
            String targetUuid = h.getTargetUuid() != null ? h.getTargetUuid() : "Unknown";
            String title = "Event: " + eventType;
            String desc = h.getPayloadJson();
            
            if ("office_appointed".equals(eventType)) {
                title = "King Appointed";
                targetUuid = resolvePlayerName(player, h.getTargetUuid());
                desc = targetUuid + " became King.";
            } else if ("office_removed".equals(eventType)) {
                title = "King Removed";
                targetUuid = resolvePlayerName(player, h.getActorUuid());
                desc = targetUuid + " was removed from office.";
            }
            
            java.time.Instant time = java.time.Instant.ofEpochMilli(h.getCreatedAt());
            String date = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(java.time.ZoneOffset.UTC).format(time);
            
            inv.setStack(i - startIndex, namedStack(Items.PAPER,
                Text.literal(title).formatted(Formatting.GOLD),
                List.of(
                    Text.literal(desc).formatted(Formatting.WHITE),
                    Text.literal(date).formatted(Formatting.GRAY)
                )));
        }
        
        if (page > 0) {
            inv.setStack(45, namedStack(Items.ARROW, Text.literal("Previous Page").formatted(Formatting.WHITE), List.of()));
        }
        if (page < totalPages - 1) {
            inv.setStack(53, namedStack(Items.ARROW, Text.literal("Next Page").formatted(Formatting.WHITE), List.of()));
        }
        
        inv.setStack(49, namedStack(Items.OAK_DOOR, Text.literal("Back to Main Menu").formatted(Formatting.YELLOW), List.of()));
        
        openScreen(player, inv, 6, Text.literal("Kingdom History (Page " + (page + 1) + "/" + totalPages + ")"), (slot, clicker) -> {
            if (slot == 45 && page > 0) {
                openHistoryMenu(clicker, page - 1);
            } else if (slot == 53 && page < totalPages - 1) {
                openHistoryMenu(clicker, page + 1);
            } else if (slot == 49) {
                openMainMenu(clicker);
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
