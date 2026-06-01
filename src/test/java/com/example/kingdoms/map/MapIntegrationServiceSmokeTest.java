package com.example.kingdoms.map;

import com.example.kingdoms.config.ConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

final class MapIntegrationServiceSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void missingBlueMapRuntimeDoesNotCrashMapService() throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            map:
              provider: bluemap
              show_capital_marker: true
            """);

        ConfigLoader config = ConfigLoader.load(configFile);

        assertDoesNotThrow(() -> new MapIntegrationService(config).init());
    }

    @Test
    void missingDynmapRuntimeDoesNotCrashMapService() throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            map:
              provider: dynmap
              show_capital_marker: true
            """);

        ConfigLoader config = ConfigLoader.load(configFile);

        assertDoesNotThrow(() -> new MapIntegrationService(config).init());
    }
}
