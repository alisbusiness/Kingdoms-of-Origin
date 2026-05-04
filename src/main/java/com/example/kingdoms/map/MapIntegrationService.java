package com.example.kingdoms.map;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integrates with BlueMap or Dynmap to display kingdom POI markers.
 * All map API calls are wrapped in try/catch — map integration never crashes the server.
 *
 * Call {@link #init()} once after the server starts, then {@link #onRulerChanged(String)}
 * whenever the officeholder changes.
 */
public final class MapIntegrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapIntegrationService.class);

    // TODO: read these from a locations.yml instead of hardcoded values
    private static final double CAPITAL_X      = 0.0;
    private static final double CAPITAL_Y      = 64.0;
    private static final double CAPITAL_Z      = 0.0;
    private static final double ELECTION_X     = 100.0;
    private static final double ELECTION_Y     = 64.0;
    private static final double ELECTION_Z     = 100.0;

    private static final String CAPITAL_MARKER_ID      = "kingdoms_capital";
    private static final String ELECTION_HALL_MARKER_ID = "kingdoms_election_hall";
    private static final String MARKER_SET_ID           = "kingdoms_of_origin";

    private final ConfigLoader config;
    private final String provider;

    public MapIntegrationService(ConfigLoader config) {
        this.config   = config;
        this.provider = config.map().provider();
    }

    /** Registers initial markers on the map. Call from SERVER_STARTED. */
    public void init() {
        switch (provider) {
            case "bluemap" -> initBlueMap("Unknown");
            case "dynmap"  -> initDynmap("Unknown");
            case "none"    -> { /* no-op */ }
            default        -> LOGGER.warn("[Kingdoms] Unknown map provider '{}'; map integration disabled.", provider);
        }
    }

    /** Updates the CAPITAL marker popup to show the new ruler's name. */
    public void onRulerChanged(String rulerName) {
        switch (provider) {
            case "bluemap" -> updateBlueMapRuler(rulerName);
            case "dynmap"  -> updateDynmapRuler(rulerName);
            case "none"    -> { /* no-op */ }
        }
    }

    // -------------------------------------------------------------------------
    // BlueMap
    // -------------------------------------------------------------------------

    private void initBlueMap(String initialRulerName) {
        try {
            de.bluecolored.bluemap.api.BlueMapAPI.getInstance().ifPresent(api -> {
                try {
                    for (de.bluecolored.bluemap.api.BlueMapMap map : api.getMaps()) {
                        de.bluecolored.bluemap.api.markers.MarkerSet set =
                            map.getMarkerSets().computeIfAbsent(
                                MARKER_SET_ID,
                                id -> de.bluecolored.bluemap.api.markers.MarkerSet.builder()
                                    .label("Kingdoms of Origin")
                                    .build()
                            );

                        if (config.map().showCapitalMarker()) {
                            set.put(CAPITAL_MARKER_ID, de.bluecolored.bluemap.api.markers.POIMarker.builder()
                                .label("Capital")
                                .detail("<b>Capital</b><br>Ruler: " + escapeHtml(initialRulerName))
                                .position(CAPITAL_X, CAPITAL_Y, CAPITAL_Z)
                                .build());
                        }

                        set.put(ELECTION_HALL_MARKER_ID, de.bluecolored.bluemap.api.markers.POIMarker.builder()
                            .label("Election Hall")
                            .detail("<b>Election Hall</b><br>Vote here during elections.")
                            .position(ELECTION_X, ELECTION_Y, ELECTION_Z)
                            .build());
                    }
                } catch (Exception e) {
                    LOGGER.warn("[Kingdoms] BlueMap marker init failed: {}", e.getMessage());
                }
            });
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.warn("[Kingdoms] BlueMap is not present; skipping map markers.");
        }
    }

    private void updateBlueMapRuler(String rulerName) {
        try {
            de.bluecolored.bluemap.api.BlueMapAPI.getInstance().ifPresent(api -> {
                try {
                    for (de.bluecolored.bluemap.api.BlueMapMap map : api.getMaps()) {
                        de.bluecolored.bluemap.api.markers.MarkerSet set =
                            map.getMarkerSets().get(MARKER_SET_ID);
                        if (set == null) return;

                        de.bluecolored.bluemap.api.markers.Marker existing =
                            set.get(CAPITAL_MARKER_ID);
                        if (!(existing instanceof de.bluecolored.bluemap.api.markers.POIMarker poi)) return;

                        set.put(CAPITAL_MARKER_ID, de.bluecolored.bluemap.api.markers.POIMarker.builder()
                            .label("Capital")
                            .detail("<b>Capital</b><br>Ruler: " + escapeHtml(rulerName))
                            .position(poi.getPosition())
                            .build());
                    }
                } catch (Exception e) {
                    LOGGER.warn("[Kingdoms] BlueMap ruler update failed: {}", e.getMessage());
                }
            });
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.warn("[Kingdoms] BlueMap is not present; skipping ruler marker update.");
        }
    }

    // -------------------------------------------------------------------------
    // Dynmap
    // -------------------------------------------------------------------------

    private void initDynmap(String initialRulerName) {
        try {
            org.dynmap.DynmapCommonAPIListener.register(new org.dynmap.DynmapCommonAPIListener() {
                @Override
                public void apiEnabled(org.dynmap.DynmapCommonAPI api) {
                    try {
                        org.dynmap.markers.MarkerAPI markerApi = api.getMarkerAPI();
                        if (markerApi == null) {
                            LOGGER.warn("[Kingdoms] Dynmap MarkerAPI is null; skipping markers.");
                            return;
                        }

                        org.dynmap.markers.MarkerSet set = markerApi.getMarkerSet(MARKER_SET_ID);
                        if (set == null) {
                            set = markerApi.createMarkerSet(
                                MARKER_SET_ID, "Kingdoms of Origin", null, false);
                        }
                        if (set == null) {
                            LOGGER.warn("[Kingdoms] Could not create Dynmap marker set.");
                            return;
                        }

                        if (config.map().showCapitalMarker()) {
                            org.dynmap.markers.Marker capital =
                                set.findMarker(CAPITAL_MARKER_ID);
                            if (capital == null) {
                                set.createMarker(CAPITAL_MARKER_ID, "Capital",
                                    "world", CAPITAL_X, CAPITAL_Y, CAPITAL_Z,
                                    markerApi.getMarkerIcon("default"), false);
                            }
                            if (capital != null) {
                                capital.setDescription("<b>Capital</b><br>Ruler: " + escapeHtml(initialRulerName));
                            }
                        }

                        org.dynmap.markers.Marker hall = set.findMarker(ELECTION_HALL_MARKER_ID);
                        if (hall == null) {
                            hall = set.createMarker(ELECTION_HALL_MARKER_ID, "Election Hall",
                                "world", ELECTION_X, ELECTION_Y, ELECTION_Z,
                                markerApi.getMarkerIcon("default"), false);
                        }
                        if (hall != null) {
                            hall.setDescription("<b>Election Hall</b><br>Vote here during elections.");
                        }
                    } catch (Exception e) {
                        LOGGER.warn("[Kingdoms] Dynmap marker init failed: {}", e.getMessage());
                    }
                }
            });
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.warn("[Kingdoms] Dynmap is not present; skipping map markers.");
        }
    }

    private void updateDynmapRuler(String rulerName) {
        try {
            // Dynmap markers are live objects — look up the set and update description directly.
            // DynmapCommonAPI must already be registered (done in initDynmap).
            // This is a best-effort update; if Dynmap reloaded the API will re-init markers.
            LOGGER.debug("[Kingdoms] Dynmap ruler update requested for '{}'", rulerName);
            // Full implementation requires storing the API reference from apiEnabled() callback.
            // TODO: store api reference in a field set inside the DynmapCommonAPIListener above
            //       and call capital.setDescription(...) here.
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.warn("[Kingdoms] Dynmap ruler update failed: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String escapeHtml(String s) {
        if (s == null) return "Unknown";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
