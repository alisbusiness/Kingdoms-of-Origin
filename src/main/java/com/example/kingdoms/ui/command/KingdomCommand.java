package com.example.kingdoms.ui.command;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.election.ElectionException;
import com.example.kingdoms.election.ElectionPhase;
import com.example.kingdoms.election.ElectionService;
import com.example.kingdoms.origin.OfficeService;
import com.example.kingdoms.origin.OriginAdapter;
import com.example.kingdoms.origin.OriginTransferService;
import com.example.kingdoms.ui.Messages;
import com.example.kingdoms.ui.Permissions;
import com.example.kingdoms.ui.gui.GuiService;
import com.mojang.brigadier.CommandDispatcher;
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
                src.sendMessage(net.minecraft.text.Text.literal("Active Perks: " + activePerks).formatted(net.minecraft.util.Formatting.AQUA));
            }
        }
        return 1;
    }

    private int cmdPromiseMenu(CommandContext<ServerCommandSource> ctx) {
        return withPlayer(ctx, player -> guiService.openPerkSelectionMenu(player, false, 0));
    }

    private int cmdPromise(CommandContext<ServerCommandSource> ctx, String perks) {
        return withPlayer(ctx, player -> {
            String[] perksArr = perks.split("[,\\s]+");
            long validCount = java.util.Arrays.stream(perksArr).filter(p -> !p.isBlank()).count();
            if (validCount > 4) {
                player.sendMessage(net.minecraft.text.Text.literal("You can only promise up to 4 perks!").formatted(net.minecraft.util.Formatting.RED), false);
                return;
            }

            try {
                String officeId = plugin.getConfig().office().id();
                electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                    try {
                        var candidateOpt = plugin.getPersistence().candidates().findByElectionAndPlayer(election.getId(), player.getUuidAsString());
                        if (candidateOpt.isPresent()) {
                            var candidate = candidateOpt.get();
                            candidate.setPromisedPerks(perks);
                            plugin.getPersistence().candidates().delete(candidate.getId()); // Delete and re-insert to update
                            plugin.getPersistence().candidates().insert(candidate);
                            player.sendMessage(net.minecraft.text.Text.literal("Promised perks updated to: " + perks).formatted(net.minecraft.util.Formatting.GREEN), false);
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
            String[] perksArr = perks.split("[,\\s]+");
            long validCount = java.util.Arrays.stream(perksArr).filter(p -> !p.isBlank()).count();
            if (validCount > 4) {
                player.sendMessage(net.minecraft.text.Text.literal("You can only set up to 4 perks!").formatted(net.minecraft.util.Formatting.RED), false);
                return;
            }

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
                officeService.setActivePerks(perks);
                player.sendMessage(net.minecraft.text.Text.literal("Active perks set to: " + perks).formatted(net.minecraft.util.Formatting.GREEN), false);
                
                // Grant powers to all players online
                MinecraftServer server = ctx.getSource().getServer();
                for (String perk : perks.split("[,\\s]+")) {
                    if (perk.isBlank()) continue;
                    String powerId = perk.contains(":") ? perk : "kingdom:perks/" + perk;
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), "power grant @a " + powerId);
                }
            } catch (SQLException e) {
                player.sendMessage(Messages.error("Failed to save active perks."), false);
            }
        });
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
            ElectionPhase.valueOf(phase.toUpperCase());
            String officeId = plugin.getConfig().office().id();
            electionService.getCurrentElection(officeId).ifPresentOrElse(election -> {
                try {
                    election.setStatus(phase.toUpperCase());
                    plugin.getPersistence().elections().update(election);
                    src.sendMessage(Messages.phaseSet(phase.toUpperCase()));
                } catch (SQLException e) {
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
}
