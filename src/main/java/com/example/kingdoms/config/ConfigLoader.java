package com.example.kingdoms.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);

    public record OfficeConfig(
        String id,
        String displayName,
        int termDays,
        boolean electionEnabled,
        int nominationDays,
        int campaignDays,
        int votingDays
    ) {}

    public record OriginModeConfig(
        String kingOriginId
    ) {}

    public record OriginRestoreConfig(
        boolean restorePreviousOriginOnTermEnd,
        boolean clearKingOriginOnTermEnd
    ) {}

    public record TransitionConfig(
        boolean giveOrbOnTransfer,
        boolean giveOrbOnAbdication,
        boolean giveOrbOnForcedRemoval
    ) {}

    public record VotingConfig(
        String system,
        int minimumPlaytimeMinutes,
        boolean anonymousVotes
    ) {}

    public record UiConfig(
        boolean useScoreboardAnnouncements,
        boolean useBossbarDuringElection,
        boolean sendChatBroadcasts
    ) {}

    public record MapConfig(
        String provider,
        boolean showCapitalMarker
    ) {}

    public record TreasuryConfig(
        String currencyName,
        int mintMax,
        int spendMax,
        int transitionFreezeMinutes,
        int xpTaxCap,
        int tradeTaxCap,
        int resourceTaxCap,
        int levyTaxCap
    ) {}

    public record RevoltConfig(
        int threshold,
        int captureRequired,
        double captureRadius,
        double capitalX,
        double capitalY,
        double capitalZ
    ) {}

    public record DebugConfig(
        boolean logOriginTransfers,
        boolean logGuiActions
    ) {}

    private OfficeConfig office;
    private OriginModeConfig originMode;
    private OriginRestoreConfig originRestore;
    private TransitionConfig transition;
    private VotingConfig voting;

    private UiConfig ui;
    private MapConfig map;
    private TreasuryConfig treasury;
    private RevoltConfig revolt;
    private DebugConfig debug;

    private ConfigLoader(Map<String, Object> raw) {
        office       = parseOffice(section(raw, "office"));
        originMode   = parseOriginMode(section(raw, "origin_mode"));
        originRestore = parseOriginRestore(section(raw, "origin_restore"));
        transition   = parseTransition(section(raw, "transition"));
        voting       = parseVoting(section(raw, "voting"));

        ui           = parseUi(section(raw, "ui"));
        map          = parseMap(section(raw, "map"));
        treasury     = parseTreasury(section(raw, "treasury"));
        revolt       = parseRevolt(section(raw, "revolt"));
        debug        = parseDebug(section(raw, "debug"));
        validate();
    }

    public static ConfigLoader load(Path configFile) {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configFile)) {
            Map<String, Object> raw = yaml.load(in);
            if (raw == null) raw = Map.of();
            return new ConfigLoader(raw);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config from " + configFile, e);
        }
    }

    public void reload(Path configFile) {
        ConfigLoader next = load(configFile);
        office = next.office;
        originMode = next.originMode;
        originRestore = next.originRestore;
        transition = next.transition;
        voting = next.voting;
        ui = next.ui;
        map = next.map;
        treasury = next.treasury;
        revolt = next.revolt;
        debug = next.debug;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> parent, String key) {
        Object val = parent.get(key);
        if (val instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return Map.of();
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v instanceof String s ? s : def;
    }

    private static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : def;
    }

    private static int integer(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private static double decimal(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List<?> list) return (List<String>) list;
        return List.of();
    }

    private static OfficeConfig parseOffice(Map<String, Object> m) {
        return new OfficeConfig(
            str(m, "id", "king"),
            str(m, "display_name", "King"),
            positiveInt(m, "term_days", 7),
            bool(m, "election_enabled", true),
            positiveInt(m, "nomination_days", 2),
            positiveInt(m, "campaign_days", 2),
            positiveInt(m, "voting_days", 1)
        );
    }

    private static OriginModeConfig parseOriginMode(Map<String, Object> m) {
        return new OriginModeConfig(
            str(m, "king_origin_id", "kingdoms_of_origin:king")
        );
    }

    private static OriginRestoreConfig parseOriginRestore(Map<String, Object> m) {
        return new OriginRestoreConfig(
            bool(m, "restore_previous_origin_on_term_end", true),
            bool(m, "clear_king_origin_on_term_end", true)
        );
    }

    private static TransitionConfig parseTransition(Map<String, Object> m) {
        return new TransitionConfig(
            bool(m, "give_orb_on_transfer", true),
            bool(m, "give_orb_on_abdication", true),
            bool(m, "give_orb_on_forced_removal", false)
        );
    }

    private static VotingConfig parseVoting(Map<String, Object> m) {
        return new VotingConfig(
            str(m, "system", "plurality"),
            Math.max(0, integer(m, "minimum_playtime_minutes", 60)),
            bool(m, "anonymous_votes", true)
        );
    }

    private static UiConfig parseUi(Map<String, Object> m) {
        return new UiConfig(
            bool(m, "use_scoreboard_announcements", true),
            bool(m, "use_bossbar_during_election", true),
            bool(m, "send_chat_broadcasts", true)
        );
    }

    private static MapConfig parseMap(Map<String, Object> m) {
        String provider = str(m, "provider", "bluemap").toLowerCase(Locale.ROOT);
        if (!provider.equals("bluemap") && !provider.equals("dynmap") && !provider.equals("none")) {
            LOGGER.warn("Invalid map.provider '{}'; using 'none'.", provider);
            provider = "none";
        }
        return new MapConfig(
            provider,
            bool(m, "show_capital_marker", true)
        );
    }

    private static TreasuryConfig parseTreasury(Map<String, Object> m) {
        Map<String, Object> caps = section(m, "tax_caps");
        return new TreasuryConfig(
            str(m, "currency_name", "Crowns"),
            positiveInt(m, "mint_max", 500),
            positiveInt(m, "spend_max", 250),
            positiveInt(m, "transition_freeze_minutes", 30),
            clampPercent(integer(caps, "xp", 30)),
            clampPercent(integer(caps, "trade", 30)),
            clampPercent(integer(caps, "resource", 30)),
            clampPercent(integer(caps, "levy", 50))
        );
    }

    private static RevoltConfig parseRevolt(Map<String, Object> m) {
        Map<String, Object> capital = section(m, "capital");
        return new RevoltConfig(
            clampPercent(integer(m, "threshold", 80)),
            positiveInt(m, "capture_required", 100),
            Math.max(1.0, decimal(m, "capture_radius", 18.0)),
            decimal(capital, "x", 0.0),
            decimal(capital, "y", 64.0),
            decimal(capital, "z", 0.0)
        );
    }

    private static DebugConfig parseDebug(Map<String, Object> m) {
        return new DebugConfig(
            bool(m, "log_origin_transfers", true),
            bool(m, "log_gui_actions", false)
        );
    }

    private void validate() {
        if (originMode.kingOriginId() == null || !originMode.kingOriginId().contains(":")) {
            LOGGER.warn("origin_mode.king_origin_id '{}' does not look like a namespaced ID.", originMode.kingOriginId());
        }
        if (!voting.system().equalsIgnoreCase("plurality")) {
            LOGGER.warn("Unsupported voting.system '{}'; current implementation still uses plurality.", voting.system());
        }
    }

    private static int positiveInt(Map<String, Object> m, String key, int def) {
        return Math.max(1, integer(m, key, def));
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public OfficeConfig office()             { return office; }
    public OriginModeConfig originMode()     { return originMode; }
    public OriginRestoreConfig originRestore() { return originRestore; }
    public TransitionConfig transition()     { return transition; }
    public VotingConfig voting()             { return voting; }

    public UiConfig ui()                     { return ui; }
    public MapConfig map()                   { return map; }
    public TreasuryConfig treasury()         { return treasury; }
    public RevoltConfig revolt()             { return revolt; }
    public DebugConfig debug()               { return debug; }
}
