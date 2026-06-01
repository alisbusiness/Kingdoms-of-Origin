package com.example.kingdoms;

import com.example.kingdoms.config.ConfigLoader;
import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.election.ElectionPhase;
import com.example.kingdoms.election.ElectionService;
import com.example.kingdoms.law.LawService;
import com.example.kingdoms.origin.OfficeService;
import com.example.kingdoms.origin.OriginAdapter;
import com.example.kingdoms.origin.OriginTransferService;
import com.example.kingdoms.origin.OriginsAdapterImpl;
import com.example.kingdoms.origin.PlayerStateService;
import com.example.kingdoms.perk.PerkService;
import com.example.kingdoms.schedule.ScheduleService;
import com.example.kingdoms.schedule.StartupRecoveryService;
import com.example.kingdoms.map.MapIntegrationService;
import com.example.kingdoms.network.ServerNetworking;
import com.example.kingdoms.treasury.TreasuryService;
import com.example.kingdoms.ui.AnnouncementService;
import com.example.kingdoms.ui.EventListeners;
import com.example.kingdoms.ui.command.KingdomCommand;
import com.example.kingdoms.ui.gui.GuiService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;

public class KingdomsPlugin implements ModInitializer {

    public static final String MOD_ID = "kingdoms_of_origin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KingdomsPlugin instance;

    private Path configDir;
    private ConfigLoader config;
    private PersistenceService persistence;
    private ElectionService electionService;
    private ScheduleService scheduleService;
    private OriginAdapter originAdapter;
    private OriginTransferService transferService;
    private OfficeService officeService;
    private PlayerStateService playerStateService;
    private AnnouncementService announcementService;
    private EventListeners eventListeners;
    private MapIntegrationService mapIntegrationService;
    private PerkService perkService;
    private TreasuryService treasuryService;
    private LawService lawService;

    @Override
    public void onInitialize() {
        instance = this;

        configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        ensureDefaultConfig(configDir);
        config = ConfigLoader.load(configDir.resolve("config.yml"));

        Path dataDir = FabricLoader.getInstance().getGameDir().resolve("data").resolve(MOD_ID);
        persistence = new PersistenceService(dataDir);
        persistence.open();

        Clock clock = Clock.systemUTC();
        electionService  = new ElectionService(persistence, config, clock);
        scheduleService  = new ScheduleService(electionService, persistence, config, clock);
        announcementService = new AnnouncementService(config);

        // Wire election phase announcements (runs on scheduler thread; dispatch to server thread inside)
        electionService.addPhaseListener((election, phase) -> {
            if (announcementService == null) return;
            switch (phase) {
                case CAMPAIGN -> announcementService.broadcastElectionStart(config.office().displayName());
                case VOTING   -> {
                    try {
                        var candidates = electionService.getCandidates(election.getId());
                        var names = candidates.stream()
                            .map(c -> {
                                try {
                                    return persistence.players().findByUuid(c.getPlayerUuid())
                                        .map(p -> p.getUsername() != null ? p.getUsername() : c.getPlayerUuid())
                                        .orElse(c.getPlayerUuid());
                                } catch (SQLException e) {
                                    return c.getPlayerUuid();
                                }
                            })
                            .toList();
                        announcementService.broadcastVotingOpen(names);
                    } catch (SQLException e) {
                        LOGGER.error("Could not fetch candidates for voting announcement", e);
                    }
                }
                default -> {}
            }
        });

        try {
            new StartupRecoveryService(persistence, electionService, scheduleService, config, clock)
                .recover();
        } catch (SQLException e) {
            LOGGER.error("Startup recovery failed", e);
        }

        if (FabricLoader.getInstance().isModLoaded("origins")) {
            originAdapter = new OriginsAdapterImpl();
            LOGGER.info("Origins mod detected; origin adapter activated.");
        } else {
            LOGGER.warn("Origins mod not loaded; origin assignment features are disabled.");
        }

        ServerNetworking.register(originAdapter);

        transferService    = new OriginTransferService(originAdapter, persistence, config);
        officeService      = new OfficeService(persistence, config, electionService, transferService, clock);
        officeService.setAnnouncementService(announcementService);
        playerStateService = new PlayerStateService(originAdapter, persistence, config);
        perkService = new PerkService(this, officeService);
        treasuryService = new TreasuryService(persistence, config, officeService);
        treasuryService.setAnnouncementService(announcementService);
        lawService = new LawService(persistence, officeService);

        try {
            officeService.init();
        } catch (SQLException e) {
            LOGGER.error("OfficeService initialization failed", e);
        }

        // Note: playerStateService.handleJoin is called from the consolidated JOIN listener in EventListeners.

        GuiService guiService = new GuiService(
            electionService, officeService, announcementService, this);

        eventListeners = new EventListeners(this, officeService, announcementService, playerStateService);
        eventListeners.register();
        perkService.registerEvents();

        new KingdomCommand(this, electionService, officeService, transferService, guiService).register();

        mapIntegrationService = new MapIntegrationService(config);
        officeService.setMapService(mapIntegrationService);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            officeService.setServer(server);
            treasuryService.setServer(server);
            officeService.onServerStarted();
            announcementService.setServer(server);
            mapIntegrationService.init();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            scheduleService.shutdown();
            persistence.close();
            LOGGER.info("Kingdoms of Origin shut down cleanly.");
        });

        LOGGER.info("Kingdoms of Origin initialized.");
    }

    // -------------------------------------------------------------------------
    // Config reload
    // -------------------------------------------------------------------------

    public void reloadConfig() {
        config.reload(configDir.resolve("config.yml"));
        LOGGER.info("Config reloaded from disk.");
    }

    // -------------------------------------------------------------------------
    // Election facade — starts election AND schedules transitions in one call
    // -------------------------------------------------------------------------

    public void startElectionAndSchedule(String officeId) throws com.example.kingdoms.election.ElectionException, SQLException {
        var election = electionService.startElection(officeId);
        scheduleService.scheduleNextTransition(election);
        announcementService.broadcastElectionStart(config.office().displayName());
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public static KingdomsPlugin getInstance()              { return instance; }
    public ConfigLoader getConfig()                         { return config; }
    public PersistenceService getPersistence()              { return persistence; }
    public ElectionService getElectionService()             { return electionService; }
    public ScheduleService getScheduleService()             { return scheduleService; }
    public OriginAdapter getOriginAdapter()                 { return originAdapter; }
    public OriginTransferService getTransferService()       { return transferService; }
    public OfficeService getOfficeService()                 { return officeService; }
    public PlayerStateService getPlayerStateService()       { return playerStateService; }
    public EventListeners getEventListeners()               { return eventListeners; }
    public MapIntegrationService getMapIntegrationService() { return mapIntegrationService; }
    public AnnouncementService getAnnouncementService()     { return announcementService; }
    public PerkService getPerkService()                     { return perkService; }
    public TreasuryService getTreasuryService()             { return treasuryService; }
    public LawService getLawService()                       { return lawService; }

    private void ensureDefaultConfig(Path configDir) {
        Path configFile = configDir.resolve("config.yml");
        if (Files.exists(configFile)) return;
        try {
            Files.createDirectories(configDir);
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) Files.copy(in, configFile);
            }
        } catch (IOException e) {
            LOGGER.error("Could not write default config.yml", e);
        }
    }
}
