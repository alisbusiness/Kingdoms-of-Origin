package com.example.kingdoms.map;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BlueMapMapIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueMapMapIntegration.class);

    private final boolean showCapitalMarker;
    private final double capitalX;
    private final double capitalY;
    private final double capitalZ;
    private final double electionX;
    private final double electionY;
    private final double electionZ;
    private final String capitalMarkerId;
    private final String electionHallMarkerId;
    private final String markerSetId;

    BlueMapMapIntegration(boolean showCapitalMarker, double capitalX, double capitalY, double capitalZ,
                          double electionX, double electionY, double electionZ, String capitalMarkerId,
                          String electionHallMarkerId, String markerSetId) {
        this.showCapitalMarker = showCapitalMarker;
        this.capitalX = capitalX;
        this.capitalY = capitalY;
        this.capitalZ = capitalZ;
        this.electionX = electionX;
        this.electionY = electionY;
        this.electionZ = electionZ;
        this.capitalMarkerId = capitalMarkerId;
        this.electionHallMarkerId = electionHallMarkerId;
        this.markerSetId = markerSetId;
    }

    void init(String initialRulerName) {
        BlueMapAPI.getInstance().ifPresent(api -> {
            try {
                for (var map : api.getMaps()) {
                    MarkerSet set = map.getMarkerSets().computeIfAbsent(
                        markerSetId,
                        id -> MarkerSet.builder()
                            .label("Kingdoms of Origin")
                            .build()
                    );

                    if (showCapitalMarker) {
                        set.put(capitalMarkerId, POIMarker.builder()
                            .label("Capital")
                            .detail("<b>Capital</b><br>Ruler: " + escapeHtml(initialRulerName))
                            .position(capitalX, capitalY, capitalZ)
                            .build());
                    }

                    set.put(electionHallMarkerId, POIMarker.builder()
                        .label("Election Hall")
                        .detail("<b>Election Hall</b><br>Vote here during elections.")
                        .position(electionX, electionY, electionZ)
                        .build());
                }
            } catch (Exception e) {
                LOGGER.warn("[Kingdoms] BlueMap marker init failed: {}", e.getMessage());
            }
        });
    }

    void updateRuler(String rulerName) {
        BlueMapAPI.getInstance().ifPresent(api -> {
            try {
                for (var map : api.getMaps()) {
                    MarkerSet set = map.getMarkerSets().get(markerSetId);
                    if (set == null) return;

                    Marker existing = set.get(capitalMarkerId);
                    if (!(existing instanceof POIMarker poi)) return;

                    set.put(capitalMarkerId, POIMarker.builder()
                        .label("Capital")
                        .detail("<b>Capital</b><br>Ruler: " + escapeHtml(rulerName))
                        .position(poi.getPosition())
                        .build());
                }
            } catch (Exception e) {
                LOGGER.warn("[Kingdoms] BlueMap ruler update failed: {}", e.getMessage());
            }
        });
    }

    private static String escapeHtml(String s) {
        if (s == null) return "Unknown";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
