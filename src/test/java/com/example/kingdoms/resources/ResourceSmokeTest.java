package com.example.kingdoms.resources;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceSmokeTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void allJsonResourcesParse() throws IOException {
        List<Path> jsonFiles;
        try (var stream = Files.walk(RESOURCES)) {
            jsonFiles = stream
                .filter(path -> path.toString().endsWith(".json"))
                .sorted()
                .toList();
        }

        assertFalse(jsonFiles.isEmpty(), "Expected JSON resources to smoke-test");
        for (Path jsonFile : jsonFiles) {
            JsonParser.parseString(Files.readString(jsonFile));
        }
    }

    @Test
    void coreDatapackResourcesExist() {
        assertTrue(Files.exists(RESOURCES.resolve("fabric.mod.json")));
        assertTrue(Files.exists(RESOURCES.resolve("config.yml")));
        assertTrue(Files.exists(RESOURCES.resolve("db/schema.sql")));
        assertTrue(Files.exists(RESOURCES.resolve("data/origins/origin_layers/origin.json")));
        assertTrue(Files.exists(RESOURCES.resolve("data/kingdoms_of_origin/origins/king.json")));
        assertTrue(Files.exists(RESOURCES.resolve("data/kingdoms_of_origin/origins/human.json")));
    }
}
