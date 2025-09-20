package com.x1f4r.mmocraft.world.resourcegathering.listeners;

import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.loot.model.LootTable;
import com.x1f4r.mmocraft.loot.service.LootService;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ActiveResourceNode;
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;
import com.x1f4r.mmocraft.world.resourcegathering.service.ActiveNodeManager;
import com.x1f4r.mmocraft.world.resourcegathering.service.ResourceNodeRegistryService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ResourceNodeInteractionListenerTest {

    @Test
    void dropsFortunedLootAndDepletesNode() {
        MMOCraftPlugin plugin = mock(MMOCraftPlugin.class);
        ActiveNodeManager activeNodeManager = mock(ActiveNodeManager.class);
        ResourceNodeRegistryService nodeRegistryService = mock(ResourceNodeRegistryService.class);
        LootService lootService = mock(LootService.class);
        CustomItemRegistry customItemRegistry = mock(CustomItemRegistry.class);
        PlayerDataService playerDataService = mock(PlayerDataService.class);
        GameplayConfigService gameplayConfigService = mock(GameplayConfigService.class);
        LoggingUtil loggingUtil = mock(LoggingUtil.class);

        RuntimeStatConfig runtimeConfig = RuntimeStatConfig.defaults();
        when(gameplayConfigService.getRuntimeStatConfig()).thenReturn(runtimeConfig);

        ResourceNodeInteractionListener listener = new ResourceNodeInteractionListener(
                plugin,
                activeNodeManager,
                nodeRegistryService,
                lootService,
                customItemRegistry,
                playerDataService,
                gameplayConfigService,
                loggingUtil);

        World world = mock(World.class);
        Location nodeLocation = new Location(world, 10, 64, 10);
        ActiveResourceNode activeNode = new ActiveResourceNode(nodeLocation, "test_node");
        when(activeNodeManager.getActiveNode(any(Location.class))).thenReturn(Optional.of(activeNode));

        ResourceNodeType nodeType = new ResourceNodeType(
                "test_node",
                Material.STONE,
                2.0,
                Set.of(Material.WOODEN_PICKAXE),
                "test_loot",
                30);
        when(nodeRegistryService.getNodeType("test_node")).thenReturn(Optional.of(nodeType));

        LootTable lootTable = mock(LootTable.class);
        ItemStack originalDrop = mock(ItemStack.class);
        ItemStack clonedDrop = mock(ItemStack.class);
        AtomicInteger dropAmount = new AtomicInteger(1);
        when(originalDrop.getAmount()).thenReturn(1);
        when(originalDrop.clone()).thenReturn(clonedDrop);
        when(clonedDrop.clone()).thenReturn(clonedDrop);
        when(clonedDrop.getAmount()).thenAnswer(invocation -> dropAmount.get());
        doAnswer(invocation -> {
            dropAmount.set(invocation.getArgument(0));
            return null;
        }).when(clonedDrop).setAmount(anyInt());
        when(lootTable.generateLoot(eq(customItemRegistry), eq(plugin))).thenReturn(List.of(originalDrop));
        when(lootService.getLootTableById("test_loot")).thenReturn(Optional.of(lootTable));

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Miner");
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack heldTool = mock(ItemStack.class);
        doReturn(Material.WOODEN_PICKAXE).when(heldTool).getType();
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(heldTool);
        when(player.getWorld()).thenReturn(world);
        Location playerLocation = new Location(world, 11, 64, 11);
        when(player.getLocation()).thenReturn(playerLocation);

        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getStatValue(Stat.MINING_SPEED)).thenReturn(160.0);
        when(profile.getStatValue(Stat.MINING_FORTUNE)).thenReturn(500.0);
        when(playerDataService.getPlayerProfile(playerId)).thenReturn(profile);

        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(nodeLocation);

        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);

        doReturn(null).when(world).dropItemNaturally(any(), any());

        listener.onBlockBreak(event);

        verify(event).setCancelled(true);
        verify(event).setDropItems(false);
        verify(lootTable).generateLoot(customItemRegistry, plugin);
        ArgumentCaptor<ItemStack> dropCaptor = ArgumentCaptor.forClass(ItemStack.class);
        verify(world).dropItemNaturally(eq(playerLocation), dropCaptor.capture());
        assertEquals(clonedDrop, dropCaptor.getValue());
        assertEquals(2, dropAmount.get());
        verify(activeNodeManager).depleteNode(activeNode);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(player).sendMessage(messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("successfully"));
    }
}
