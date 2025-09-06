package com.x1f4r.mmocraft.world.resourcegathering.persistence;

import com.x1f4r.mmocraft.persistence.PersistenceService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResourceNodeRepository {

    private static final String TABLE_NAME = "active_resource_nodes";
    private final PersistenceService persistenceService;
    private final LoggingUtil loggingUtil;

    public ResourceNodeRepository(PersistenceService persistenceService, LoggingUtil loggingUtil) {
        this.persistenceService = persistenceService;
        this.loggingUtil = loggingUtil;
    }

    public void initDatabaseSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "world_uid TEXT NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "node_type_id TEXT NOT NULL, " +
                "is_depleted INTEGER NOT NULL, " +
                "respawn_at_millis INTEGER NOT NULL, " +
                "PRIMARY KEY (world_uid, x, y, z)" +
                ");";
        try (Connection conn = persistenceService.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            loggingUtil.info("'" + TABLE_NAME + "' table schema initialized successfully.");
        } catch (SQLException e) {
            loggingUtil.severe("Failed to initialize '" + TABLE_NAME + "' table schema.", e);
        }
    }

    public void saveOrUpdateNode(ActiveResourceNode node) {
        String sql = "REPLACE INTO " + TABLE_NAME + " (world_uid, x, y, z, node_type_id, is_depleted, respawn_at_millis) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = persistenceService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Location loc = node.getInternalLocation();
            pstmt.setString(1, loc.getWorld().getUID().toString());
            pstmt.setInt(2, loc.getBlockX());
            pstmt.setInt(3, loc.getBlockY());
            pstmt.setInt(4, loc.getBlockZ());
            pstmt.setString(5, node.getNodeTypeId());
            pstmt.setInt(6, node.isDepleted() ? 1 : 0);
            pstmt.setLong(7, node.getRespawnAtMillis());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            loggingUtil.severe("Failed to save or update resource node at " + node.getLocation(), e);
        }
    }

    public Map<Location, ActiveResourceNode> loadAllNodes() {
        Map<Location, ActiveResourceNode> nodes = new HashMap<>();
        String sql = "SELECT * FROM " + TABLE_NAME;

        try (Connection conn = persistenceService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                try {
                    UUID worldUid = UUID.fromString(rs.getString("world_uid"));
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    String nodeTypeId = rs.getString("node_type_id");
                    boolean isDepleted = rs.getInt("is_depleted") == 1;
                    long respawnAtMillis = rs.getLong("respawn_at_millis");

                    if (Bukkit.getWorld(worldUid) == null) {
                        loggingUtil.warning("Could not load resource node in world with UID " + worldUid + " because the world is not loaded. Skipping.");
                        continue;
                    }

                    Location location = new Location(Bukkit.getWorld(worldUid), x, y, z);
                    ActiveResourceNode node = new ActiveResourceNode(location, nodeTypeId);
                    node.setDepleted(isDepleted);
                    node.setRespawnAtMillis(respawnAtMillis);

                    nodes.put(location, node);
                } catch (IllegalArgumentException e) {
                    loggingUtil.warning("Could not parse world UID from database. Skipping resource node record.", e);
                }
            }
            loggingUtil.info("Successfully loaded " + nodes.size() + " active resource nodes from the database.");
        } catch (SQLException e) {
            loggingUtil.severe("Failed to load resource nodes from database.", e);
            // Return empty map on failure
            return new HashMap<>();
        }
        return nodes;
    }

    public void deleteNode(ActiveResourceNode node) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE world_uid = ? AND x = ? AND y = ? AND z = ?";

        try (Connection conn = persistenceService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Location loc = node.getInternalLocation();
            pstmt.setString(1, loc.getWorld().getUID().toString());
            pstmt.setInt(2, loc.getBlockX());
            pstmt.setInt(3, loc.getBlockY());
            pstmt.setInt(4, loc.getBlockZ());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                loggingUtil.debug("Successfully deleted node at " + node.getLocation() + " from the database.");
            }
        } catch (SQLException e) {
            loggingUtil.severe("Failed to delete resource node at " + node.getLocation(), e);
        }
    }
}
