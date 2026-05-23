package com.example.kingdoms.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigLoaderSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsBundledConfig() {
        ConfigLoader config = ConfigLoader.load(Path.of("src/main/resources/config.yml"));

        assertEquals("king", config.office().id());
        assertEquals("kingdoms_of_origin:king", config.originMode().kingOriginId());
        assertEquals("bluemap", config.map().provider());
        assertEquals("Crowns", config.treasury().currencyName());
        assertTrue(config.office().termDays() > 0);
    }

    @Test
    void partialAndInvalidConfigFallsBackToSafeValues() throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            office:
              term_days: -20
            map:
              provider: invalid-provider
            treasury:
              tax_caps:
                xp: 999
                trade: -10
            revolt:
              capture_radius: 0.25
            """);

        ConfigLoader config = ConfigLoader.load(configFile);

        assertEquals(1, config.office().termDays());
        assertEquals("none", config.map().provider());
        assertEquals(100, config.treasury().xpTaxCap());
        assertEquals(0, config.treasury().tradeTaxCap());
        assertEquals(1.0, config.revolt().captureRadius());
    }
}
