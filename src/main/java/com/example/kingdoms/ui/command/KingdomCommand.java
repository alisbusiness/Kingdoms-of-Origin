package com.example.kingdoms.ui.command;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.election.ElectionPhase;
import com.example.kingdoms.election.ElectionService;
import com.example.kingdoms.origin.OfficeService;
import com.example.kingdoms.origin.OriginAdapter;
import com.example.kingdoms.origin.OriginTransferService;
import com.example.kingdoms.perk.PerkDefinition;
import com.example.kingdoms.perk.PerkRegistry;
import com.example.kingdoms.treasury.SpendingCategory;
import com.example.kingdoms.treasury.TreasuryState;
import com.example.kingdoms.treasury.TreasuryService;
import com.example.kingdoms.ui.Messages;
import com.example.kingdoms.ui.Permissions;
import com.example.kingdoms.ui.gui.GuiService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Registers all /kingdom commands using Fabric's Brigadier API.
 * Call {@link #register()} during ModInitializer.onInitialize() — before the server starts.
 */
public final class KingdomCommand {

    private final KingdomsPlugin plugin;
    private final ElectionService electionService;
    private final OfficeService officeService;
    private final OriginTransferService transferService;
    private final GuiService guiService;

    public KingdomCommand(
        KingdomsPlugin plugin,
        ElectionService electionService,
        OfficeService officeService,
        OriginTransferService transferService,
        GuiService guiService
    ) {
        this.plugin          = plugin;
        this.electionService = electionService;
        this.officeService   = officeService;
        this.transferService = transferService;
        this.guiService      = guiService;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            build(dispatcher));
    }

    // ------------------------------------------------------------------
    // Command tree
    // ------------------------------------------------------------------

    private void build(CommandDispatcher<ServerCommandSource> dispatcher) {

        var kingdom = CommandManager.literal("kingdom");

        // --- Player sub-commands ---

        kingdom.then(CommandManager.literal("status")
            .requires(Permissions::isPlayer)
            .executes(this::cmdStatus));

        kingdom.then(CommandManager.literal("vote")
            .requires(Permissions::isPlayer)
            .executes(this::cmdVote));

        kingdom.then(CommandManager.literal("candidates")
            .requires(Permissions::isPlayer)
            .executes(this::cmdCandidates));

        kingdom.then(CommandManager.literal("run")
            .requires(Permissions::isPlayer)
            .then(CommandManager.argument("slogan", StringArgumentType.greedyString())
                .executes(ctx -> cmdRun(ctx, StringArgumentType.getString(ctx, "slogan"))))
            .executes(ctx -> cmdRun(ctx, "")));

        kingdom.then(CommandManager.literal("ruler")
            .requires(Permissions::isPlayer)
            .executes(this::cmdRuler));

        kingdom.then(CommandManager.literal("promise")
            .requires(Permissions::isPlayer)
            .executes(this::cmdPromiseMenu)
            .then(CommandManager.argument("perks", StringArgumentType.greedyString())
                .executes(ctx -> cmdPromise(ctx, StringArgumentType.getString(ctx, "perks")))));

        kingdom.then(CommandManager.literal("setperks")
            .requires(Permissions::isPlayer)
            .executes(this::cmdSetPerksMenu)
            .then(CommandManager.argument("perks", StringArgumentType.greedyString())
                .executes(ctx -> cmdSetPerks(ctx, StringArgumentType.getString(ctx, "perks")))));

        kingdom.then(CommandManager.literal("perks")
            .requires(Permissions::isPlayer)
            .executes(this::cmdViewPerks));

        kingdom.then(CommandManager.literal("perk")
            .then(CommandManager.argument("id", StringArgumentType.word())
                .executes(ctx -> cmdInspectPerk(ctx, StringArgumentType.getString(ctx, "id")))));

        kingdom.then(CommandManager.literal("trust")
            .executes(this::cmdTrust));

        kingdom.then(CommandManager.literal("treasury")
            .requires(Permissions::isPlayer)
            .executes(this::cmdTreasury)
            .then(CommandManager.literal("gui").executes(ctx -> withPlayer(ctx, guiService::openTreasuryMenu)))
            .then(CommandManager.literal("deposit")
                .then(CommandManager.argument("diamonds", IntegerArgumentType.integer(0))
                    .then(CommandManager.argument("blocks", IntegerArgumentType.integer(0))
                        .executes(ctx -> cmdTreasuryDeposit(ctx, IntegerArgumentType.getInteger(ctx, "diamonds"), IntegerArgumentType.getInteger(ctx, "blocks"))))))
            .then(CommandManager.literal("redeem")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> cmdRedeem(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
            .then(CommandManager.literal("tax")
                .then(CommandManager.argument("channel", StringArgumentType.word())
                    .then(CommandManager.argument("rate", IntegerArgumentType.integer(0, 50))
                        .executes(ctx -> cmdSetTax(ctx, StringArgumentType.getString(ctx, "channel"), IntegerArgumentType.getInteger(ctx, "rate"))))))
            .then(CommandManager.literal("mint")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 500))
                    .executes(ctx -> cmdMint(ctx, IntegerArgumentType.getInteger(ctx, "amount"), false))))
            .then(CommandManager.literal("emergency-mint")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 500))
                    .executes(ctx -> cmdMint(ctx, IntegerArgumentType.getInteger(ctx, "amount"), true))))
            .then(CommandManager.literal("spend")
                .then(CommandManager.argument("category", StringArgumentType.word())
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 250))
                        .executes(ctx -> cmdSpend(ctx, StringArgumentType.getString(ctx, "category"), IntegerArgumentType.getInteger(ctx, "amount")))))));

        kingdom.then(CommandManager.literal("revolt")
            .requires(Permissions::isPlayer)
            .executes(this::cmdRevolt)
            .then(CommandManager.literal("join").executes(ctx -> cmdJoinRevolt(ctx, "rebel")))
            .then(CommandManager.literal("defend").executes(ctx -> cmdJoinRevolt(ctx, "loyalist"))));

        kingdom.then(CommandManager.literal("help")
            .executes(this::cmdHelp));

        kingdom.then(CommandManager.literal("menu")
            .requires(Permissions::isPlayer)
            .executes(this::cmdMenu));

        kingdom.then(CommandManager.literal("start-election")
            .requires(Permissions::isPlayer)
            .executes(this::cmdStartElection));

        // --- Admin sub-commands ---

        var admin = CommandManager.literal("admin")
            .requires(Permissions::isAdmin);

        admin.then(CommandManager.literal("start-election")
            .executes(this::adminStartElection));

        admin.then(CommandManager.literal("end-election")
            .executes(this::adminEndElection));

        admin.then(CommandManager.literal("set-ruler")
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(ctx -> adminSetRuler(ctx, EntityArgumentType.getPlayer(ctx, "player")))));

        admin.then(CommandManager.literal("remove-ruler")
            .executes(this::adminRemoveRuler));

        admin.then(CommandManager.literal("force-transfer")
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(ctx -> adminForceTransfer(ctx, EntityArgumentType.getPlayer(ctx, "player")))));

        admin.then(CommandManager.literal("give-orb")
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(ctx -> adminGiveOrb(ctx, EntityArgumentType.getPlayer(ctx, "player")))));

        admin.then(CommandManager.literal("set-phase")
            .then(CommandManager.argument("phase", StringArgumentType.word())
                .executes(ctx -> adminSetPhase(ctx, StringArgumentType.getString(ctx, "phase")))));

        admin.then(CommandManager.literal("reload")
            .executes(this::adminReload));

        admin.then(CommandManager.literal("debug-sync")
            .executes(this::adminDebugSync));

        admin.then(CommandManager.literal("db-status")
            .executes(this::adminDbStatus));

        admin.then(CommandManager.literal("election-status")
            .executes(this::adminElectionStatus));

        admin.then(CommandManager.literal("treasury-ledger")
            .executes(this::adminTreasuryLedger));

        admin.then(CommandManager.literal("repair-office-state")
            .executes(this::adminRepairOfficeState));

        kingdom.then(admin);

        dispatcher.register(kingdom);
    }

    // ------------------------------------------------------------------
    // Player command handlers
    // ------------------------------------------------------------------

    private int cmdStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String rulerUuid = officeService.getRuler();
        String phase     = officeService.getPhase();
        long   termEnd   = officeService.getTermEnd();

        src.sendMessage(Messages.statusHeader());
        if (rulerUuid != null) {
            src.sendMessage(Messages.rulerLine(resolveOrUuid(src, rulerUuid)));
            src.sendMessage(Messages.termEndLine(termEnd));
        } else {
            src.sendMessage(Messages.noRuler());
        }
        src.sendMessage(Messages.phaseLine(phase));
        return 1;
    }

    private int cmdVote(CommandContext<ServerCommandSource> ctx) {
        return withPlayer(ctx, player -> {
            ElectionPhase phase = ElectionPhase.fromString(officeService.getPhase());
            if (phase != ElectionPhase.VOTING) {
                player.sendMessage(Messages.votingNotOpen(), false);
                return;
            }
            guiService.openCandidateList(player);
        });
    }

    private int cmdCandidates(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            String officeId = plugin.getConfig().office().id();
            electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                try {
                    var candidates = electionService.getCandidates(election.getId());
                    src.sendMessage(Messages.candidatesHeader(candidates.size()));
                    for (int i = 0; i < candidates.size(); i++) {
                        var c    = candidates.get(i);
                        String name = resolveOrUuid(src, c.getPlayerUuid());
                        src.sendMessage(Messages.candidateLine(i, name, c.getSlogan()));
                    }
                    if (candidates.isEmpty()) src.sendMessage(Messages.noCandidates());
                } catch (SQLException e) {
                    src.sendMessage(Messages.error("Could not load candidates."));
                }
            }, () -> src.sendMessage(Messages.noActiveElection()));
        } catch (SQLException e) {
            src.sendMessage(Messages.error("A server error occurred."));
        }
        return 1;
    }

    private int cmdRun(CommandContext<ServerCommandSource> ctx, String slogan) {
        return withPlayer(ctx, player -> {
            try {
                String officeId = plugin.getConfig().office().id();
                electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                    try {
                        electionService.registerCandidate(election.getId(), player.getUuidAsString(), slogan);
                        player.sendMessage(Messages.registeredAsCandidate(slogan), false);
                    } catch (ElectionException e) {
                        String msg = e.getMessage();
                        if (msg.contains("already a candidate")) {
                            player.sendMessage(Messages.alreadyCandidate(), false);
                        } else if (msg.contains("Nominations are closed")) {
                            player.sendMessage(Messages.nominationsNotOpen(), false);
                        } else if (msg.contains("cannot run")) {
                            player.sendMessage(Messages.holderCannotRun(), false);
                        } else {
                            player.sendMessage(Messages.error(msg), false);
                        }
                    } catch (SQLException e) {
                        KingdomsPlugin.LOGGER.error("Register candidate failed", e);
                        player.sendMessage(Messages.error("A server error occurred."), false);
                    }
                }, () -> player.sendMessage(Messages.noActiveElection(), false));
            } catch (SQLException e) {
                KingdomsPlugin.LOGGER.error("Election lookup failed", e);
                player.sendMessage(Messages.error("A server error occurred."), false);
            }
        });
    }

    private int cmdRuler(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String rulerUuid = officeService.getRuler();
        if (rulerUuid == null) {
            src.sendMessage(Messages.noRuler());
        } else {
            src.sendMessage(Messages.rulerLine(resolveOrUuid(src, rulerUuid)));
            src.sendMessage(Messages.termEndLine(officeService.getTermEnd()));
            src.sendMessage(Messages.phaseLine(officeService.getPhase()));
            String activePerks = officeService.getActivePerks();
            if (activePerks != null && !activePerks.isBlank()) {
                src.sendMessage(net.minecraft.text.Text.literal("Active Policies: " + activePerks).formatted(net.minecraft.util.Formatting.AQUA));
            }
            try {
                src.sendMessage(net.minecraft.text.Text.literal("Trust Score: " + plugin.getPersistence().trust().getScore(rulerUuid)).formatted(net.minecraft.util.Formatting.GREEN));
            } catch (SQLException ignored) {}
        }
        return 1;
    }

    private int cmdPromiseMenu(CommandContext<ServerCommandSource> ctx) {
        return withPlayer(ctx, player -> guiService.openPerkSelectionMenu(player, false, 0));
    }

    private int cmdPromise(CommandContext<ServerCommandSource> ctx, String perks) {
        return withPlayer(ctx, player -> {
            var ids = PerkRegistry.parseIds(perks);
            var budget = PerkRegistry.validateBudget(ids);
            if (!budget.valid()) {
                player.sendMessage(net.minecraft.text.Text.literal(String.join(" ", budget.errors())).formatted(net.minecraft.util.Formatting.RED), false);
                return;
            }

            try {
                String officeId = plugin.getConfig().office().id();
                electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                    try {
                        var candidateOpt = plugin.getPersistence().candidates().findByElectionAndPlayer(election.getId(), player.getUuidAsString());
                        if (candidateOpt.isPresent()) {
                            var candidate = candidateOpt.get();
                            String encoded = PerkRegistry.serialize(ids);
                            plugin.getPersistence().candidates().updatePromises(candidate.getId(), encoded);
                            player.sendMessage(net.minecraft.text.Text.literal("Promised policies updated. Remaining Policy Points if enacted: " + budget.remaining()).formatted(net.minecraft.util.Formatting.GREEN), false);
                        } else {
                            player.sendMessage(net.minecraft.text.Text.literal("You must /kingdom run first.").formatted(net.minecraft.util.Formatting.RED), false);
                        }
                    } catch (SQLException e) {
                        player.sendMessage(Messages.error("A server error occurred."), false);
                    }
                }, () -> player.sendMessage(Messages.noActiveElection(), false));
            } catch (SQLException e) {
                player.sendMessage(Messages.error("A server error occurred."), false);
            }
        });
    }

    private int cmdSetPerksMenu(CommandContext<ServerCommandSource> ctx) {
        return withPlayer(ctx, player -> guiService.openPerkSelectionMenu(player, true, 0));
    }

    private int cmdSetPerks(CommandContext<ServerCommandSource> ctx, String perks) {
        return withPlayer(ctx, player -> {
            String rulerUuid = officeService.getRuler();
            if (rulerUuid == null || !rulerUuid.equals(player.getUuidAsString())) {
                player.sendMessage(net.minecraft.text.Text.literal("Only the King can set perks!").formatted(net.minecraft.util.Formatting.RED), false);
                return;
            }
            if (officeService.getActivePerks() != null) {
                player.sendMessage(net.minecraft.text.Text.literal("Perks have already been set for this term!").formatted(net.minecraft.util.Formatting.RED), false);
                return;
            }
            
            try {
                var result = plugin.getPerkService().activate(player, PerkRegistry.parseIds(perks));
                player.sendMessage(net.minecraft.text.Text.literal(result.message()).formatted(result.success() ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED), false);
            } catch (SQLException e) {
                player.sendMessage(Messages.error("Failed to save active perks."), false);
            }
        });
    }

    private int cmdViewPerks(CommandContext<ServerCommandSource> ctx) {
        return withPlayer(ctx, guiService::openActivePerksMenu);
    }

    private int cmdInspectPerk(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        PerkDefinition perk = PerkRegistry.find(id).orElse(null);
        if (perk == null) {
            src.sendMessage(net.minecraft.text.Text.literal("Unknown perk: " + id).formatted(net.minecraft.util.Formatting.RED));
            return 0;
        }
        src.sendMessage(net.minecraft.text.Text.literal(perk.name() + " [" + perk.category().displayName() + ", " + perk.kind().label() + ", " + perk.cost() + " points]").formatted(net.minecraft.util.Formatting.GOLD));
        src.sendMessage(net.minecraft.text.Text.literal(perk.description()).formatted(net.minecraft.util.Formatting.GRAY));
        return 1;
    }

    private int cmdTrust(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String rulerUuid = officeService.getRuler();
        if (rulerUuid == null) {
            src.sendMessage(Messages.noRuler());
            return 1;
        }
        try {
            src.sendMessage(net.minecraft.text.Text.literal("Trust Score: " + plugin.getPersistence().trust().getScore(rulerUuid) + "/100").formatted(net.minecraft.util.Formatting.GREEN));
            String history = plugin.getPersistence().trust().recentHistory(rulerUuid, 5);
            if (!history.isBlank()) {
                for (String line : history.split("\\n")) src.sendMessage(net.minecraft.text.Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY));
            }
        } catch (SQLException e) {
            src.sendMessage(Messages.error("Could not load trust score."));
        }
        return 1;
    }

    private int cmdHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendMessage(Messages.helpHeader());
        src.sendMessage(Messages.helpLine("/kingdom status",     "Show current ruler and election phase"));
        src.sendMessage(Messages.helpLine("/kingdom vote",       "Open the voting GUI"));
        src.sendMessage(Messages.helpLine("/kingdom candidates", "List current candidates in chat"));
        src.sendMessage(Messages.helpLine("/kingdom run [slogan]", "Register your candidacy"));
        src.sendMessage(Messages.helpLine("/kingdom promise <perks>", "Set your promised perks"));
        src.sendMessage(Messages.helpLine("/kingdom setperks <perks>", "King only: lock in active perks"));
        src.sendMessage(Messages.helpLine("/kingdom perks", "View active policies"));
        src.sendMessage(Messages.helpLine("/kingdom perk <id>", "Inspect a policy"));
        src.sendMessage(Messages.helpLine("/kingdom trust", "View king trust and promise history"));
        src.sendMessage(Messages.helpLine("/kingdom treasury", "View reserves, taxes, currency, legitimacy, and unrest"));
        src.sendMessage(Messages.helpLine("/kingdom treasury deposit <diamonds> <blocks>", "Deposit diamond reserves"));
        src.sendMessage(Messages.helpLine("/kingdom treasury redeem <amount>", "Redeem currency for diamond reserves"));
        src.sendMessage(Messages.helpLine("/kingdom treasury tax <xp|trade|resource|levy> <rate>", "King only: set tax rates"));
        src.sendMessage(Messages.helpLine("/kingdom treasury mint <amount>", "King only: mint currency"));
        src.sendMessage(Messages.helpLine("/kingdom treasury spend <category> <amount>", "King only: public or corrupt spending"));
        src.sendMessage(Messages.helpLine("/kingdom revolt join|defend", "Join a revolt side when unrest opens the window"));
        src.sendMessage(Messages.helpLine("/kingdom ruler",      "Show ruler details"));
        src.sendMessage(Messages.helpLine("/kingdom menu",       "Open the main kingdom GUI"));
        if (Permissions.isAdmin(src)) {
            src.sendMessage(Messages.helpLine("/kingdom admin ...", "Admin commands (you have access)"));
        }
        return 1;
    }

    private int cmdMenu(CommandContext<ServerCommandSource> ctx) {
        return withPlayer(ctx, guiService::openMainMenu);
    }

    private int cmdTreasury(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            TreasuryState s = plugin.getTreasuryService().state();
            src.sendMessage(net.minecraft.text.Text.literal("Treasury: " + s.rawDiamonds() + " diamonds, " + s.diamondBlocks() + " blocks (" + s.reserveValue() + " diamond reserve)").formatted(s.reserveHealth().color()));
            String currency = plugin.getTreasuryService().currencyName();
            src.sendMessage(net.minecraft.text.Text.literal(currency + ": " + s.currencySupply() + " supply, reserve ratio " + Math.round(s.reserveRatio() * 100.0) + "%, " + s.reserveHealth().label()).formatted(s.reserveHealth().color()));
            src.sendMessage(net.minecraft.text.Text.literal("Taxes: XP " + s.xpTaxRate() + "%, trade " + s.tradeTaxRate() + "%, resource " + s.resourceTitheRate() + "%, emergency levy " + s.emergencyLevyRate() + "%").formatted(net.minecraft.util.Formatting.GOLD));
            src.sendMessage(net.minecraft.text.Text.literal("Trust/Legitimacy/Heat/Unrest: " + trustScore() + "/100, " + s.legitimacy() + "/100, " + s.corruptionHeat() + "/100, " + s.unrest() + "/100 (" + s.unrestBand().label() + ")").formatted(s.unrestBand().color()));
            if (src.getEntity() instanceof ServerPlayerEntity player) {
                src.sendMessage(net.minecraft.text.Text.literal("Your " + currency + ": " + plugin.getTreasuryService().balance(player.getUuidAsString())).formatted(net.minecraft.util.Formatting.AQUA));
            }
            if (s.revoltActive()) src.sendMessage(net.minecraft.text.Text.literal("Revolt active: capture the capital at " + plugin.getTreasuryService().capitalText() + ". Progress " + s.captureProgress() + "%").formatted(net.minecraft.util.Formatting.DARK_RED));
        } catch (SQLException e) {
            src.sendMessage(Messages.error("Could not load treasury."));
        }
        return 1;
    }

    private int cmdTreasuryDeposit(CommandContext<ServerCommandSource> ctx, int diamonds, int blocks) {
        return withPlayer(ctx, player -> {
            try {
                plugin.getTreasuryService().depositReserves(player, diamonds, blocks);
            } catch (SQLException e) {
                player.sendMessage(Messages.error("Could not deposit reserves."), false);
            }
        });
    }

    private int cmdRedeem(CommandContext<ServerCommandSource> ctx, int amount) {
        return withPlayer(ctx, player -> {
            try {
                plugin.getTreasuryService().redeem(player, amount);
            } catch (SQLException e) {
                player.sendMessage(Messages.error("Could not redeem Crowns."), false);
            }
        });
    }

    private int cmdSetTax(CommandContext<ServerCommandSource> ctx, String channel, int rate) {
        return withPlayer(ctx, player -> {
            try {
                plugin.getTreasuryService().setTax(player, channel, rate);
            } catch (Exception e) {
                player.sendMessage(Messages.error(e.getMessage()), false);
            }
        });
    }

    private int cmdMint(CommandContext<ServerCommandSource> ctx, int amount, boolean emergency) {
        return withPlayer(ctx, player -> {
            try {
                plugin.getTreasuryService().mint(player, amount, emergency);
            } catch (Exception e) {
                player.sendMessage(Messages.error(e.getMessage()), false);
            }
        });
    }

    private int cmdSpend(CommandContext<ServerCommandSource> ctx, String category, int amount) {
        return withPlayer(ctx, player -> {
            try {
                SpendingCategory c = SpendingCategory.valueOf(category.toUpperCase());
                plugin.getTreasuryService().spend(player, c, amount);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Messages.error("Categories: public_works, military, relief, infrastructure, festival, stabilization, palace, siphon"), false);
            } catch (Exception e) {
                player.sendMessage(Messages.error(e.getMessage()), false);
            }
        });
    }

    private int cmdRevolt(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            TreasuryState s = plugin.getTreasuryService().state();
            src.sendMessage(net.minecraft.text.Text.literal("Revolt: " + (s.revoltActive() ? "ACTIVE" : "inactive") + ", unrest " + s.unrest() + "/100, capture " + s.captureProgress() + "%").formatted(s.revoltActive() ? net.minecraft.util.Formatting.DARK_RED : net.minecraft.util.Formatting.GRAY));
            src.sendMessage(net.minecraft.text.Text.literal("Capital objective: stand near " + plugin.getTreasuryService().capitalText() + " during revolt. Rebels progress only if they outnumber loyalists in the zone.").formatted(net.minecraft.util.Formatting.YELLOW));
        } catch (SQLException e) {
            src.sendMessage(Messages.error("Could not load revolt status."));
        }
        return 1;
    }

    private int cmdJoinRevolt(CommandContext<ServerCommandSource> ctx, String side) {
        return withPlayer(ctx, player -> {
            try {
                plugin.getTreasuryService().joinRevolt(player, side);
            } catch (SQLException e) {
                player.sendMessage(Messages.error("Could not join revolt."), false);
            }
        });
    }

    private int cmdStartElection(CommandContext<ServerCommandSource> ctx) {
        return withPlayer(ctx, player -> {
            String rulerUuid = officeService.getRuler();
            if (rulerUuid == null || !rulerUuid.equals(player.getUuidAsString())) {
                player.sendMessage(net.minecraft.text.Text.literal("Only the King can start elections!").formatted(net.minecraft.util.Formatting.RED), false);
                return;
            }
            try {
                plugin.startElectionAndSchedule(plugin.getConfig().office().id());
                player.sendMessage(Messages.electionStarted(), false);
            } catch (ElectionException e) {
                player.sendMessage(Messages.error(e.getMessage()), false);
            } catch (SQLException e) {
                KingdomsPlugin.LOGGER.error("Start election failed", e);
                player.sendMessage(Messages.error("A server error occurred."), false);
            }
        });
    }

    // ------------------------------------------------------------------
    // Admin command handlers
    // ------------------------------------------------------------------

    private int adminStartElection(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            plugin.startElectionAndSchedule(plugin.getConfig().office().id());
            src.sendMessage(Messages.electionStarted());
        } catch (ElectionException e) {
            src.sendMessage(Messages.error(e.getMessage()));
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Start election failed", e);
            src.sendMessage(Messages.error("A server error occurred."));
        }
        return 1;
    }

    private int adminEndElection(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            String officeId = plugin.getConfig().office().id();
            electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                try {
                    plugin.getScheduleService().forceAdvancePhase(election.getId());
                    src.sendMessage(Messages.electionEnded());
                } catch (Exception e) {
                    src.sendMessage(Messages.error("Failed to end election: " + e.getMessage()));
                }
            }, () -> src.sendMessage(Messages.noActiveElection()));
        } catch (SQLException e) {
            src.sendMessage(Messages.error("A server error occurred."));
        }
        return 1;
    }

    private int adminSetRuler(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerCommandSource src = ctx.getSource();
        try {
            officeService.assignRuler(target.getUuid());
            src.sendMessage(Messages.rulerSet(target.getName().getString()));
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Set ruler failed", e);
            src.sendMessage(Messages.error("A server error occurred."));
        }
        return 1;
    }

    private int adminRemoveRuler(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            officeService.removeRuler(OfficeService.RemovalReason.FORCED);
            src.sendMessage(Messages.rulerRemoved());
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Remove ruler failed", e);
            src.sendMessage(Messages.error("A server error occurred."));
        }
        return 1;
    }

    private int adminForceTransfer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerCommandSource src = ctx.getSource();
        String currentRulerUuid = officeService.getRuler();
        if (currentRulerUuid == null) {
            // No current ruler — just assign
            try {
                officeService.assignRuler(target.getUuid());
                src.sendMessage(Messages.rulerSet(target.getName().getString()));
            } catch (SQLException e) {
                src.sendMessage(Messages.error("A server error occurred."));
            }
            return 1;
        }
        try {
            transferService.transferOffice(UUID.fromString(currentRulerUuid), target.getUuid());
            src.sendMessage(Messages.rulerSet(target.getName().getString()));
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Force transfer failed", e);
            src.sendMessage(Messages.error("A server error occurred."));
        }
        return 1;
    }

    private int adminGiveOrb(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerCommandSource src = ctx.getSource();
        try {
            transferService.giveOriginOrb(target);
            src.sendMessage(Messages.orbGiven(target.getName().getString()));
        } catch (Exception e) {
            KingdomsPlugin.LOGGER.error("Give orb failed", e);
            src.sendMessage(Messages.error("Failed to give orb: " + e.getMessage()));
        }
        return 1;
    }

    private int adminSetPhase(CommandContext<ServerCommandSource> ctx, String phase) {
        ServerCommandSource src = ctx.getSource();
        try {
            ElectionPhase nextPhase = ElectionPhase.valueOf(phase.toUpperCase());
            String officeId = plugin.getConfig().office().id();
            electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                try {
                    var updated = electionService.setPhase(election.getId(), nextPhase);
                    if (nextPhase == ElectionPhase.COMPLETE || nextPhase == ElectionPhase.IDLE) {
                        plugin.getScheduleService().cancelTransition(updated.getId());
                    } else {
                        plugin.getScheduleService().scheduleNextTransition(updated);
                    }
                    src.sendMessage(Messages.phaseSet(nextPhase.name()));
                } catch (SQLException | ElectionException e) {
                    src.sendMessage(Messages.error("A server error occurred."));
                }
            }, () -> src.sendMessage(Messages.noActiveElection()));
        } catch (IllegalArgumentException e) {
            src.sendMessage(Messages.error("Unknown phase: " + phase));
        } catch (SQLException e) {
            src.sendMessage(Messages.error("A server error occurred."));
        }
        return 1;
    }

    private int adminReload(CommandContext<ServerCommandSource> ctx) {
        plugin.reloadConfig();
        ctx.getSource().sendMessage(Messages.configReloaded());
        return 1;
    }

    private int adminDebugSync(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String rulerUuid = officeService.getRuler();
        if (rulerUuid == null) {
            src.sendMessage(Messages.noRulerToSync());
            return 1;
        }
        ServerPlayerEntity ruler = src.getServer().getPlayerManager().getPlayer(UUID.fromString(rulerUuid));
        if (ruler != null) {
            transferService.validateSync(ruler);
            src.sendMessage(Messages.debugSynced());
        } else {
            src.sendMessage(Messages.error("Ruler is offline; sync will apply on next login."));
        }
        return 1;
    }

    private int adminDbStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            String officeId = plugin.getConfig().office().id();
            int players = plugin.getPersistence().players().findAll().size();
            int elections = plugin.getPersistence().elections().findByOfficeId(officeId).size();
            var treasury = plugin.getTreasuryService().state();
            src.sendMessage(net.minecraft.text.Text.literal("DB OK: " + players + " players, " + elections + " elections for office '" + officeId + "'.").formatted(net.minecraft.util.Formatting.GREEN));
            src.sendMessage(net.minecraft.text.Text.literal("Treasury row OK: reserve " + treasury.reserveValue() + ", supply " + treasury.currencySupply() + ".").formatted(net.minecraft.util.Formatting.GRAY));
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("DB status check failed", e);
            src.sendMessage(Messages.error("Database check failed: " + e.getMessage()));
        }
        return 1;
    }

    private int adminElectionStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            String officeId = plugin.getConfig().office().id();
            var current = electionService.getCurrentElection(officeId);
            if (current.isEmpty()) {
                src.sendMessage(Messages.noActiveElection());
                return 1;
            }
            var e = current.get();
            src.sendMessage(net.minecraft.text.Text.literal("Election " + e.getId() + ": " + e.getStatus() + ", winner=" + e.getWinnerUuid()).formatted(net.minecraft.util.Formatting.GOLD));
            src.sendMessage(net.minecraft.text.Text.literal("Nomination opens " + e.getNominationOpensAt() + ", voting opens " + e.getVotingOpensAt() + ", voting closes " + e.getVotingClosesAt()).formatted(net.minecraft.util.Formatting.GRAY));
            src.sendMessage(net.minecraft.text.Text.literal("Candidates: " + electionService.getCandidates(e.getId()).size()).formatted(net.minecraft.util.Formatting.GRAY));
        } catch (SQLException e) {
            src.sendMessage(Messages.error("Could not load election status."));
        }
        return 1;
    }

    private int adminTreasuryLedger(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            var rows = plugin.getPersistence().treasury().recentPublicLedger(plugin.getConfig().office().id(), 10);
            src.sendMessage(net.minecraft.text.Text.literal("Recent public treasury ledger").formatted(net.minecraft.util.Formatting.AQUA));
            if (rows.isEmpty()) {
                src.sendMessage(net.minecraft.text.Text.literal("No public ledger entries.").formatted(net.minecraft.util.Formatting.GRAY));
            }
            for (String row : rows) {
                src.sendMessage(net.minecraft.text.Text.literal(row).formatted(net.minecraft.util.Formatting.GRAY));
            }
        } catch (SQLException e) {
            src.sendMessage(Messages.error("Could not load treasury ledger."));
        }
        return 1;
    }

    private int adminRepairOfficeState(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            String officeId = plugin.getConfig().office().id();
            var current = electionService.getCurrentElection(officeId);
            var state = plugin.getPersistence().officeStates().findByOfficeId(officeId)
                .orElseGet(() -> new com.example.kingdoms.db.model.OfficeState(officeId, null, null, null, 0L, 0L, null, null));
            state.setPhase(current.map(com.example.kingdoms.db.model.Election::getStatus).orElse(ElectionPhase.IDLE.name()));
            plugin.getPersistence().officeStates().save(state);
            src.sendMessage(net.minecraft.text.Text.literal("Office state repaired: phase=" + state.getPhase()).formatted(net.minecraft.util.Formatting.GREEN));
        } catch (SQLException e) {
            KingdomsPlugin.LOGGER.error("Office state repair failed", e);
            src.sendMessage(Messages.error("Could not repair office state."));
        }
        return 1;
    }

    // ------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------

    /** Runs an action requiring a player source; sends an error and returns 0 if not a player. */
    private int withPlayer(CommandContext<ServerCommandSource> ctx, java.util.function.Consumer<ServerPlayerEntity> action) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            ctx.getSource().sendMessage(Messages.error("This command can only be used by players."));
            return 0;
        }
        action.accept(player);
        return 1;
    }

    private String resolveOrUuid(ServerCommandSource src, String uuid) {
        try {
            ServerPlayerEntity online = src.getServer().getPlayerManager().getPlayer(UUID.fromString(uuid));
            if (online != null) return online.getName().getString();
            return plugin.getPersistence().players().findByUuid(uuid)
                .map(p -> p.getUsername() != null ? p.getUsername() : uuid)
                .orElse(uuid);
        } catch (Exception e) {
            return uuid;
        }
    }

    private int trustScore() {
        String rulerUuid = officeService.getRuler();
        if (rulerUuid == null) return 50;
        try {
            return plugin.getPersistence().trust().getScore(rulerUuid);
        } catch (SQLException e) {
            return 50;
        }
    }
}
