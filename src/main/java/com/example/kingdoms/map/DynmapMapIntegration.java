package com.example.kingdoms.map;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DynmapMapIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynmapMapIntegration.class);

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

    DynmapMapIntegration(boolean showCapitalMarker, double capitalX, double capitalY, double capitalZ,
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
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI api) {
                createMarkers(api, initialRulerName);
            }
        });
    }

    private void createMarkers(DynmapCommonAPI api, String initialRulerName) {
        try {
            MarkerAPI markerApi = api.getMarkerAPI();
            if (markerApi == null) {
                LOGGER.warn("[Kingdoms] Dynmap MarkerAPI is null; skipping markers.");
                return;
            }

            MarkerSet set = markerApi.getMarkerSet(markerSetId);
            if (set == null) {
                set = markerApi.createMarkerSet(markerSetId, "Kingdoms of Origin", null, false);
            }
            if (set == null) {
                LOGGER.warn("[Kingdoms] Could not create Dynmap marker set.");
                return;
            }

            if (showCapitalMarker) {
                Marker capital = set.findMarker(capitalMarkerId);
                if (capital == null) {
                    capital = set.createMarker(capitalMarkerId, "Capital", "world",
                        capitalX, capitalY, capitalZ, markerApi.getMarkerIcon("default"), false);
                }
                if (capital != null) {
                    capital.setDescription("<b>Capital</b><br>Ruler: " + escapeHtml(initialRulerName));
                }
            }

            Marker hall = set.findMarker(electionHallMarkerId);
            if (hall == null) {
                hall = set.createMarker(electionHallMarkerId, "Election Hall", "world",
                    electionX, electionY, electionZ, markerApi.getMarkerIcon("default"), false);
            }
            if (hall != null) {
                hall.setDescription("<b>Election Hall</b><br>Vote here during elections.");
            }
        } catch (Exception e) {
            LOGGER.warn("[Kingdoms] Dynmap marker init failed: {}", e.getMessage());
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "Unknown";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
