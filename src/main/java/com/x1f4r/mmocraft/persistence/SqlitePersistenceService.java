package com.x1f4r.mmocraft.persistence;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
// import java.util.logging.Level; // Replaced by LoggingUtil

public class SqlitePersistenceService implements PersistenceService {

    // private final JavaPlugin plugin; // Keep for getDataFolder, or pass LoggingUtil separately
    private final LoggingUtil log;
    private Connection connection;
    private final String dbUrl;

    public SqlitePersistenceService(JavaPlugin plugin) {
        // this.plugin = plugin;
        if (plugin instanceof MMOCraftPlugin) {
            this.log = ((MMOCraftPlugin) plugin).getLoggingUtil();
             if (this.log == null) { // Should not happen if MMOCraftPlugin.onEnable order is correct
                throw new IllegalStateException("LoggingUtil not initialized in MMOCraftPlugin before SqlitePersistenceService.");
            }
        } else {
            this.log = new LoggingUtil(plugin); // Fallback
            this.log.warning("SqlitePersistenceService initialized with generic Plugin instance. Logging may use fallback.");
        }

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "mmocraft_data.db");
        this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try {
            Class.forName("com.x1f4r.mmocraft.lib.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            this.log.severe("SQLite JDBC driver not found after relocation. Shading might have failed.", e);
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                connection = DriverManager.getConnection(dbUrl);
                this.log.info("Connection to SQLite has been established.");
            } catch (SQLException e) {
                this.log.severe("Failed to establish SQLite connection.", e);
                throw e;
            }
        }
        return connection;
    }

    @Override
    public void initDatabase() throws SQLException {
        String createPluginInfoTableSql = "CREATE TABLE IF NOT EXISTS plugin_info (" +
                                          "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                          "info_key TEXT NOT NULL UNIQUE," +
                                          "info_value TEXT NOT NULL" +
                                          ");";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createPluginInfoTableSql);
            this.log.info("Database initialized and 'plugin_info' table created/verified.");

            // Basic Test: Insert and query a dummy record
            String testKey = "db_version";
            Optional<String> existingVersion = executeQuerySingle(
                "SELECT info_value FROM plugin_info WHERE info_key = ?;",
                rs -> rs.getString("info_value"),
                testKey
            );

            if (!existingVersion.isPresent()) {
                executeUpdate("INSERT INTO plugin_info (info_key, info_value) VALUES (?, ?);", testKey, "1.0");
                this.log.info("Inserted initial db_version into plugin_info.");
            }

            Optional<String> queriedVersion = executeQuerySingle(
                "SELECT info_value FROM plugin_info WHERE info_key = ?;",
                rs -> rs.getString("info_value"),
                testKey
            );

            queriedVersion.ifPresent(version -> this.log.info("Successfully queried test data: " + testKey + " = " + version));
            if (!queriedVersion.isPresent()) {
                this.log.warning("Failed to query test data from plugin_info.");
            }

        } catch (SQLException e) {
            this.log.severe("Error initializing database or running basic test.", e);
            throw e;
        }
    }

    @Override
    public <T> Optional<T> executeQuerySingle(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapper.mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> executeQueryList(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.mapRow(rs));
                }
            }
        }
        return results;
    }

    @Override
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, params);
            return pstmt.executeUpdate();
        }
    }

    private void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            this.log.info("SQLite connection closed.");
        }
    }
}
