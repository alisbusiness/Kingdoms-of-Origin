package com.example.kingdoms.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaManager {

    private final Connection conn;

    public SchemaManager(Connection conn) {
        this.conn = conn;
    }

    public void applySchema() throws IOException, SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/db/schema.sql")) {
            if (in == null) throw new IOException("schema.sql not found in classpath");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String statement : sql.split(";")) {
            String trimmed = statement.strip();
            if (!trimmed.isEmpty()) {
                try (Statement st = conn.createStatement()) {
                    st.execute(trimmed);
                }
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA user_version = 1");
        }
    }
}
