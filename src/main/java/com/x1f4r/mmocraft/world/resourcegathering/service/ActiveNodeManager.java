package com.x1f4r.mmocraft.world.resourcegathering.service;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;
import com.x1f4r.mmocraft.world.resourcegathering.persistence.ResourceNodeRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveNodeManager {

    private final MMOCraftPlugin plugin;
    private final LoggingUtil logger;
    private final ResourceNodeRegistryService nodeRegistryService;
    private final ResourceNodeRepository resourceNodeRepository;

    private final Map<Location, ActiveResourceNode> activeNodes = new ConcurrentHashMap<>();
    private final Material depletedMaterial = Material.BEDROCK;

    public ActiveNodeManager(MMOCraftPlugin plugin, LoggingUtil logger,
                             ResourceNodeRegistryService nodeRegistryService,
                             ResourceNodeRepository resourceNodeRepository,
                             LootService lootService, CustomItemRegistry customItemRegistry) {
        this.plugin = plugin;
        this.logger = logger;
        this.nodeRegistryService = nodeRegistryService;
        this.resourceNodeRepository = resourceNodeRepository;
        loadNodes();
    }

    private void loadNodes() {
        logger.info("Loading active resource nodes from database...");
        Map<Location, ActiveResourceNode> loadedNodes = resourceNodeRepository.loadAllNodes();
        activeNodes.putAll(loadedNodes);
        logger.info("Finished loading " + activeNodes.size() + " nodes. Verifying world state...");

        runSync(() -> {
            for (ActiveResourceNode node : activeNodes.values()) {
                Optional<ResourceNodeType> nodeTypeOpt = nodeRegistryService.getNodeType(node.getNodeTypeId());
                if (nodeTypeOpt.isEmpty()) {
                    logger.warning("Could not verify world state for node at " + blockLocationToString(node.getLocation()) + " because its type '" + node.getNodeTypeId() + "' is no longer registered. Skipping.");
                    continue;
                }
                ResourceNodeType nodeType = nodeTypeOpt.get();
                Block block = node.getLocation().getBlock();
                Material expectedMaterial = node.isDepleted() ? depletedMaterial : nodeType.getDisplayMaterial();
                if (block.getType() != expectedMaterial) {
                    logger.debug("Correcting block state for node at " + blockLocationToString(node.getLocation()) + ". Was " + block.getType() + ", expected " + expectedMaterial);
                    block.setType(expectedMaterial);
                }
            }
            logger.info("World state verification complete.");
        });
    }

    public void placeNewNode(Location location, String nodeTypeId) {
        if (location == null || location.getWorld() == null) {
            logger.warning("Attempted to place new node at null location or world.");
            return;
        }
        Optional<ResourceNodeType> nodeTypeOpt = nodeRegistryService.getNodeType(nodeTypeId);
        if (nodeTypeOpt.isEmpty()) {
            logger.warning("Attempted to place new node with unknown typeId: " + nodeTypeId);
            return;
        }
        ResourceNodeType nodeType = nodeTypeOpt.get();
        Location blockLocation = location.getBlock().getLocation();

        if (activeNodes.containsKey(blockLocation)) {
            logger.warning("Node already exists at " + blockLocation + ". Cannot place new node: " + nodeTypeId);
            return;
        }

        ActiveResourceNode newNode = new ActiveResourceNode(blockLocation, nodeTypeId);
        activeNodes.put(blockLocation, newNode);
        resourceNodeRepository.saveOrUpdateNode(newNode); // PERSIST

        runSync(() -> {
            blockLocation.getBlock().setType(nodeType.getDisplayMaterial());
            logger.info("Placed new resource node '" + nodeTypeId + "' at " + blockLocationToString(blockLocation));
        });
    }

    public Optional<ActiveResourceNode> getActiveNode(Location location) {
        if (location == null) return Optional.empty();
        return Optional.ofNullable(activeNodes.get(location.getBlock().getLocation()));
    }

    public long countNodesOfType(String nodeTypeId) {
        if (nodeTypeId == null) {
            return 0;
        }
        return activeNodes.values().stream()
                .filter(node -> nodeTypeId.equalsIgnoreCase(node.getNodeTypeId()))
                .count();
    }

    public void depleteNode(ActiveResourceNode node) {
        if (node == null || node.isDepleted()) {
            return;
        }
        Optional<ResourceNodeType> nodeTypeOpt = nodeRegistryService.getNodeType(node.getNodeTypeId());
        if (nodeTypeOpt.isEmpty()) {
            logger.severe("Cannot deplete node: Unknown NodeType ID '" + node.getNodeTypeId() + "' for node at " + blockLocationToString(node.getLocation()));
            return;
        }
        ResourceNodeType nodeType = nodeTypeOpt.get();

        node.setDepleted(true);
        node.setRespawnAtMillis(System.currentTimeMillis() + (nodeType.getRespawnTimeSeconds() * 1000L));
        resourceNodeRepository.saveOrUpdateNode(node); // PERSIST

        runSync(() -> {
            node.getLocation().getBlock().setType(depletedMaterial);
            logger.debug("Depleted node '" + node.getNodeTypeId() + "' at " + blockLocationToString(node.getLocation()) + ". Respawning in " + nodeType.getRespawnTimeSeconds() + "s.");
        });
    }

    public void respawnNode(ActiveResourceNode node) {
        if (node == null || !node.isDepleted()) {
            return;
        }
        Optional<ResourceNodeType> nodeTypeOpt = nodeRegistryService.getNodeType(node.getNodeTypeId());
        if (nodeTypeOpt.isEmpty()) {
            logger.severe("Cannot respawn node: Unknown NodeType ID '" + node.getNodeTypeId() + "' for node at " + blockLocationToString(node.getLocation()));
            return;
        }
        ResourceNodeType nodeType = nodeTypeOpt.get();

        node.setDepleted(false);
        node.setRespawnAtMillis(0);
        resourceNodeRepository.saveOrUpdateNode(node); // PERSIST

        runSync(() -> {
            node.getLocation().getBlock().setType(nodeType.getDisplayMaterial());
            logger.debug("Respawned node '" + node.getNodeTypeId() + "' at " + blockLocationToString(node.getLocation()));
        });
    }

    public void tickNodes() {
        long currentTimeMillis = System.currentTimeMillis();
        for (ActiveResourceNode node : activeNodes.values()) {
            if (node.isDepleted() && node.getRespawnAtMillis() <= currentTimeMillis) {
                respawnNode(node);
            }
        }
    }

    public int removeNodesByType(String nodeTypeId) {
        if (nodeTypeId == null) {
            return 0;
        }
        List<Location> locationsToRemove = new ArrayList<>();
        activeNodes.forEach((location, node) -> {
            if (nodeTypeId.equalsIgnoreCase(node.getNodeTypeId())) {
                locationsToRemove.add(location);
            }
        });
        int removed = 0;
        for (Location location : locationsToRemove) {
            if (removeNode(location)) {
                removed++;
            }
        }
        return removed;
    }

    public boolean removeNode(Location location) {
        Location blockLocation = location.getBlock().getLocation();
        ActiveResourceNode node = activeNodes.remove(blockLocation);
        if (node != null) {
            resourceNodeRepository.deleteNode(node); // PERSIST
            runSync(() -> {
                blockLocation.getBlock().setType(Material.AIR);
                logger.info("Removed resource node '" + node.getNodeTypeId() + "' from " + blockLocationToString(blockLocation));
            });
            return true;
        }
        return false;
    }

    public Map<Location, ActiveResourceNode> getAllActiveNodesView() {
        return Collections.unmodifiableMap(activeNodes);
    }

    private String blockLocationToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return String.format("%s@(%d,%d,%d)", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void shutdown() {
        logger.info("ActiveNodeManager shutting down. Persisting " + activeNodes.size() + " node states...");
        for (ActiveResourceNode node : activeNodes.values()) {
            resourceNodeRepository.saveOrUpdateNode(node);
        }
        logger.info("All active resource node states have been persisted.");
    }

    private void runSync(Runnable task) {
        if (!plugin.isEnabled() || Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
}
