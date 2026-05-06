package com.example.kingdoms.db;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.db.repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class PersistenceService {

    private final Path dataDir;
    private Connection connection;

    private PlayerRepository players;
    private OfficeStateRepository officeStates;
    private ElectionRepository elections;
    private CandidateRepository candidates;
    private VoteRepository votes;
    private HistoryRepository history;
    private TrustRepository trust;

    public PersistenceService(Path dataDir) {
        this.dataDir = dataDir;
    }

    public void open() {
        try {
            Files.createDirectories(dataDir);
            Path dbFile = dataDir.resolve("kingdoms.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());

            new SchemaManager(connection).applySchema();

            players     = new PlayerRepository(connection);
            officeStates = new OfficeStateRepository(connection);
            elections   = new ElectionRepository(connection);
            candidates  = new CandidateRepository(connection);
            votes       = new VoteRepository(connection);
            history     = new HistoryRepository(connection);
            trust       = new TrustRepository(connection);

            KingdomsPlugin.LOGGER.info("Database opened at {}", dbFile);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to open database", e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }

    public PlayerRepository players()          { return players; }
    public OfficeStateRepository officeStates() { return officeStates; }
    public ElectionRepository elections()       { return elections; }
    public CandidateRepository candidates()     { return candidates; }
    public VoteRepository votes()               { return votes; }
    public HistoryRepository history()          { return history; }
    public TrustRepository trust()              { return trust; }
}
