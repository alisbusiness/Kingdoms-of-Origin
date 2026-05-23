package com.example.kingdoms.law;

import com.example.kingdoms.db.PersistenceService;
import com.example.kingdoms.db.model.Law;
import com.example.kingdoms.origin.OfficeService;
import net.minecraft.server.network.ServerPlayerEntity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class LawService {
    public static final int MAX_LAWS = 50;
    public static final int MAX_LAW_LENGTH = 180;

    private final PersistenceService persistence;
    private final OfficeService officeService;

    public LawService(PersistenceService persistence, OfficeService officeService) {
        this.persistence = persistence;
        this.officeService = officeService;
    }

    public List<Law> laws() throws SQLException {
        return persistence.laws().findAll();
    }

    public void addLaw(ServerPlayerEntity actor, String text) throws SQLException {
        requireKing(actor);
        List<Law> existing = laws();
        if (existing.size() >= MAX_LAWS) {
            throw new IllegalArgumentException("The law book is full.");
        }
        String law = sanitizeLaw(text);
        if (law.isBlank()) {
            throw new IllegalArgumentException("Law text cannot be empty.");
        }
        persistence.laws().add(law, actor.getUuidAsString());
    }

    public boolean removeLaw(ServerPlayerEntity actor, int position) throws SQLException {
        requireKing(actor);
        return persistence.laws().removeByPosition(position);
    }

    public void replaceFromBook(ServerPlayerEntity actor, String rawText) throws SQLException {
        requireKing(actor);
        List<String> parsed = parseBookText(rawText);
        if (parsed.size() > MAX_LAWS) {
            throw new IllegalArgumentException("The law book can hold up to " + MAX_LAWS + " laws.");
        }
        persistence.laws().replaceAll(parsed, actor.getUuidAsString());
    }

    public long latestUpdatedAt() throws SQLException {
        return persistence.laws().latestUpdatedAt();
    }

    public boolean isKing(ServerPlayerEntity player) {
        String rulerUuid = officeService.getRuler();
        return rulerUuid != null && rulerUuid.equals(player.getUuidAsString());
    }

    public String formatForChat() throws SQLException {
        List<Law> laws = laws();
        if (laws.isEmpty()) return "No royal laws have been written yet.";
        StringBuilder sb = new StringBuilder();
        for (Law law : laws) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(law.getPosition()).append(". ").append(law.getText());
        }
        return sb.toString();
    }

    private void requireKing(ServerPlayerEntity actor) {
        if (!isKing(actor)) {
            throw new IllegalArgumentException("Only the King can change royal laws.");
        }
    }

    private static List<String> parseBookText(String rawText) {
        List<String> parsed = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) return parsed;
        String[] parts = rawText.split("\\|");
        for (String part : parts) {
            String law = sanitizeLaw(part);
            if (!law.isBlank()) parsed.add(law);
        }
        return parsed;
    }

    private static String sanitizeLaw(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= MAX_LAW_LENGTH) return cleaned;
        return cleaned.substring(0, MAX_LAW_LENGTH).trim();
    }
}
