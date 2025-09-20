package com.x1f4r.mmocraft.world.resourcegathering.listeners;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry; // For potential custom tool interactions
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.playerdata.PlayerDataService; // For player stats/skills affecting gathering
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.util.StringUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ResourceNodeInteractionListener implements Listener {

    private final MMOCraftPlugin plugin;
    private final ActiveNodeManager activeNodeManager;
    private final ResourceNodeRegistryService nodeRegistryService;
    private final LootService lootService;
    private final CustomItemRegistry customItemRegistry; // Currently unused, for future expansion
    private final PlayerDataService playerDataService;   // Currently unused, for future expansion
    private final GameplayConfigService gameplayConfigService;
    private final LoggingUtil logger;
    private final Map<UUID, Long> gatherCooldowns = new ConcurrentHashMap<>();

    public ResourceNodeInteractionListener(MMOCraftPlugin plugin, ActiveNodeManager activeNodeManager,
                                           ResourceNodeRegistryService nodeRegistryService, LootService lootService,
                                           CustomItemRegistry customItemRegistry, PlayerDataService playerDataService,
                                           GameplayConfigService gameplayConfigService, LoggingUtil logger) {
        this.plugin = plugin;
        this.activeNodeManager = activeNodeManager;
        this.nodeRegistryService = nodeRegistryService;
        this.lootService = lootService;
        this.customItemRegistry = customItemRegistry;
        this.playerDataService = playerDataService;
        this.gameplayConfigService = gameplayConfigService;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location blockLocation = event.getBlock().getLocation();

        Optional<ActiveResourceNode> activeNodeOpt = activeNodeManager.getActiveNode(blockLocation);

        if (activeNodeOpt.isEmpty()) {
            // Not a custom resource node, let vanilla handling take over or other plugins.
            return;
        }

        // It's a custom resource node, so we take control.
        event.setCancelled(true); // Prevent vanilla block breaking behavior.
        // Set drop to 0 to ensure no vanilla drops if not cancelled fully or by other plugins.
        event.setDropItems(false);

        ActiveResourceNode activeNode = activeNodeOpt.get();

        if (activeNode.isDepleted()) {
            player.sendMessage(StringUtil.colorize("&cThis resource node is currently depleted."));
            return;
        }

        Optional<ResourceNodeType> nodeTypeOpt = nodeRegistryService.getNodeType(activeNode.getNodeTypeId());
        if (nodeTypeOpt.isEmpty()) {
            logger.severe("ActiveResourceNode at " + blockLocation + " has unknown ResourceNodeTypeId: " + activeNode.getNodeTypeId());
            player.sendMessage(StringUtil.colorize("&cError: Resource node type unknown. Please report to an admin."));
            return;
        }

        ResourceNodeType nodeType = nodeTypeOpt.get();

        PlayerProfile profile = playerDataService.getPlayerProfile(player.getUniqueId());
        RuntimeStatConfig.GatheringSettings gatheringSettings = gameplayConfigService.getRuntimeStatConfig().getGatheringSettings();
        double miningSpeed = profile != null ? profile.getStatValue(Stat.MINING_SPEED) : 0.0;
        double divisor = gatheringSettings.getMiningSpeedDelayDivisor();
        double speedFactor = divisor <= 0 ? 0.0 : (miningSpeed / divisor);
        double effectiveDelaySeconds = nodeType.getBreakTimeSeconds() / Math.max(1.0, 1.0 + speedFactor);
        effectiveDelaySeconds = Math.max(gatheringSettings.getMinimumGatherDelaySeconds(), effectiveDelaySeconds);

        long now = System.currentTimeMillis();
        long cooldownUntil = gatherCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (cooldownUntil > now) {
            double remainingSeconds = (cooldownUntil - now) / 1000.0;
            player.sendActionBar(StringUtil.colorize(String.format("&cGathering... %.1fs remaining", remainingSeconds)));
            return;
        }
        gatherCooldowns.put(player.getUniqueId(), now + (long) (effectiveDelaySeconds * 1000));

        // 1. Check Tool Requirements
        if (!nodeType.getRequiredToolTypes().isEmpty()) {
            ItemStack toolInHand = player.getInventory().getItemInMainHand();
            if (toolInHand == null || !nodeType.getRequiredToolTypes().contains(toolInHand.getType())) {
                // TODO: Make this message more descriptive (e.g., list required tools)
                player.sendMessage(StringUtil.colorize("&cYou do not have the required tool to gather from this node. (" + toolInHand.getType() + ")"));
                return;
            }
        }

        // TODO: 2. Implement Break Time / Interaction Logic
        // For now, assume instant break if tool is correct.
        // Future: This might involve initiating a "breaking" state, tracking progress,
        // and then calling depleteNode and loot distribution upon completion.
        // This could be managed by a new service or within ActiveNodeManager.

        // 3. Distribute Loot
        lootService.getLootTableById(nodeType.getLootTableId()).ifPresentOrElse(lootTable -> {
            double fortuneMultiplier = computeFortuneMultiplier(profile, nodeType, gatheringSettings);
            lootTable.generateLoot(customItemRegistry, plugin).forEach(itemStack -> {
                if (itemStack == null || itemStack.getAmount() <= 0) {
                    return;
                }
                ItemStack adjusted = applyFortune(itemStack, fortuneMultiplier);
                if (adjusted.getAmount() > 0) {
                    player.getWorld().dropItemNaturally(player.getLocation(), adjusted);
                }
            });
            player.sendMessage(StringUtil.colorize("&aYou successfully gathered resources from &e" + nodeType.getEffectiveName() + "&a!"));
            logger.debug("Player " + player.getName() + " gathered from node " + nodeType.getTypeId() + " at " + blockLocation);

            // 4. Deplete Node
            activeNodeManager.depleteNode(activeNode);

        }, () -> {
            logger.warning("LootTableId '" + nodeType.getLootTableId() + "' not found for ResourceNodeType '" + nodeType.getTypeId() + "'. No loot dropped.");
            player.sendMessage(StringUtil.colorize("&cNo loot configured for this node. Please report to an admin."));
            // Still deplete the node even if loot is missing, to prevent farming an empty node.
             activeNodeManager.depleteNode(activeNode);
        });
    }

    private double computeFortuneMultiplier(PlayerProfile profile, ResourceNodeType nodeType, RuntimeStatConfig.GatheringSettings settings) {
        Stat fortuneStat = resolveFortuneStat(nodeType);
        double perPoint = settings.getFortunePerPoint(fortuneStat);
        double statValue = profile != null ? profile.getStatValue(fortuneStat) : 0.0;
        return Math.max(1.0, 1.0 + (statValue * perPoint));
    }

    private Stat resolveFortuneStat(ResourceNodeType nodeType) {
        Material material = nodeType.getDisplayMaterial();
        if (material == null) {
            return Stat.MINING_FORTUNE;
        }
        String name = material.name();
        if (FARMING_MATERIALS.contains(material) || name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEETROOT")) {
            return Stat.FARMING_FORTUNE;
        }
        if (name.contains("LOG") || name.contains("STEM") || name.contains("HYPHAE")) {
            return Stat.FORAGING_FORTUNE;
        }
        if (name.contains("FISH") || name.contains("COD") || name.contains("SALMON") || name.contains("TROPICAL")) {
            return Stat.FISHING_FORTUNE;
        }
        return Stat.MINING_FORTUNE;
    }

    private ItemStack applyFortune(ItemStack original, double multiplier) {
        ItemStack clone = original.clone();
        double scaledAmount = clone.getAmount() * multiplier;
        int guaranteed = (int) Math.floor(scaledAmount);
        double fractional = scaledAmount - guaranteed;
        int bonus = ThreadLocalRandom.current().nextDouble() < fractional ? 1 : 0;
        int finalAmount = Math.max(0, guaranteed + bonus);
        clone.setAmount(finalAmount);
        return clone;
    }

    private static final Set<Material> FARMING_MATERIALS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.SWEET_BERRY_BUSH, Material.BAMBOO,
            Material.MELON, Material.PUMPKIN, Material.KELP
    );
}
