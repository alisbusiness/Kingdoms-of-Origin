package com.example.kingdoms.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ConfigLoader {

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
        String type,
        String kingOriginId,
        String kingLayerId
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

    public record DebugConfig(
        boolean logOriginTransfers,
        boolean logGuiActions
    ) {}

    private final OfficeConfig office;
    private final OriginModeConfig originMode;
    private final OriginRestoreConfig originRestore;
    private final TransitionConfig transition;
    private final VotingConfig voting;

    private final UiConfig ui;
    private final MapConfig map;
    private final DebugConfig debug;

    private ConfigLoader(Map<String, Object> raw) {
        office       = parseOffice(section(raw, "office"));
        originMode   = parseOriginMode(section(raw, "origin_mode"));
        originRestore = parseOriginRestore(section(raw, "origin_restore"));
        transition   = parseTransition(section(raw, "transition"));
        voting       = parseVoting(section(raw, "voting"));

        ui           = parseUi(section(raw, "ui"));
        map          = parseMap(section(raw, "map"));
        debug        = parseDebug(section(raw, "debug"));
    }

    public static ConfigLoader load(Path configFile) {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configFile)) {
            Map<String, Object> raw = yaml.load(in);
            return new ConfigLoader(raw);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config from " + configFile, e);
        }
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
            integer(m, "term_days", 7),
            bool(m, "election_enabled", true),
            integer(m, "nomination_days", 2),
            integer(m, "campaign_days", 2),
            integer(m, "voting_days", 1)
        );
    }

    private static OriginModeConfig parseOriginMode(Map<String, Object> m) {
        return new OriginModeConfig(
            str(m, "type", "layer"),
            str(m, "king_origin_id", "server:king_zeus"),
            str(m, "king_layer_id", "server:office")
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
            integer(m, "minimum_playtime_minutes", 60),
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
        return new MapConfig(
            str(m, "provider", "bluemap"),
            bool(m, "show_capital_marker", true)
        );
    }

    private static DebugConfig parseDebug(Map<String, Object> m) {
        return new DebugConfig(
            bool(m, "log_origin_transfers", true),
            bool(m, "log_gui_actions", false)
        );
    }

    public OfficeConfig office()             { return office; }
    public OriginModeConfig originMode()     { return originMode; }
    public OriginRestoreConfig originRestore() { return originRestore; }
    public TransitionConfig transition()     { return transition; }
    public VotingConfig voting()             { return voting; }

    public UiConfig ui()                     { return ui; }
    public MapConfig map()                   { return map; }
    public DebugConfig debug()               { return debug; }
}
