package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.service.CustomItemRegistry;
import com.x1f4r.mmocraft.util.LoggingUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemAdminCommandTest {

    @Mock
    private MMOCraftPlugin mockPlugin;
    @Mock
    private CustomItemRegistry mockCustomItemRegistry;
    @Mock
    private LoggingUtil mockLoggingUtil;
    @Mock
    private CommandSender mockSender;
    @Mock
    private Player mockTargetPlayer;
    @Mock
    private PlayerInventory mockPlayerInventory;

    @Captor
    private ArgumentCaptor<Component> componentCaptor;

    private ItemAdminCommand itemAdminCommand;

    @BeforeEach
    void setUp() {
        when(mockPlugin.getCustomItemRegistry()).thenReturn(mockCustomItemRegistry);
        when(mockPlugin.getLoggingUtil()).thenReturn(mockLoggingUtil);
        lenient().when(mockTargetPlayer.getName()).thenReturn("TargetPlayer");
        lenient().when(mockTargetPlayer.getInventory()).thenReturn(mockPlayerInventory);
        lenient().when(mockSender.getName()).thenReturn("AdminSender");

        itemAdminCommand = new ItemAdminCommand(mockPlugin);
    }

    @Test
    void onCommand_withoutBasePermission_informsSender() {
        when(mockSender.hasPermission("mmocraft.admin.item")).thenReturn(false);

        boolean handled = itemAdminCommand.onCommand(mockSender, new String[]{});

        assertTrue(handled, "Command should be considered handled even without permission");
        verify(mockSender).sendMessage(componentCaptor.capture());
        assertEquals(
                Component.text("You do not have permission to use this command.", NamedTextColor.RED),
                componentCaptor.getValue()
        );
    }

    @Test
    void onCommand_giveSubcommand_dispatchesAndGivesItem() {
        when(mockSender.hasPermission("mmocraft.admin.item")).thenReturn(true);
        when(mockSender.hasPermission("mmocraft.admin.item.give")).thenReturn(true);

        SimpleTestItem customItem = new SimpleTestItem(mockPlugin, "test_item", "&aTest Item");
        ItemStack expectedStack = mock(ItemStack.class);
        customItem.setNextStack(expectedStack);

        when(mockCustomItemRegistry.getCustomItem("test_item")).thenReturn(Optional.of(customItem));

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayerExact("TargetPlayer"))
                    .thenReturn(mockTargetPlayer);

            boolean handled = itemAdminCommand.onCommand(
                    mockSender,
                    new String[]{"give", "TargetPlayer", "test_item", "2"}
            );

            assertTrue(handled, "Give subcommand should be handled");
            verify(mockPlayerInventory).addItem(expectedStack);
            verify(mockSender).sendMessage(any(String.class));
            verify(mockTargetPlayer).sendMessage(any(String.class));
        }
    }

    @Test
    void onTabComplete_giveSubcommandProvidesPlayerItemsAndAmounts() {
        when(mockSender.hasPermission("mmocraft.admin.item")).thenReturn(true);
        when(mockSender.hasPermission("mmocraft.admin.item.give")).thenReturn(true);

        SimpleTestItem berserker = new SimpleTestItem(mockPlugin, "berserker_gauntlet", "&dBerserker Gauntlet");
        SimpleTestItem windrunner = new SimpleTestItem(mockPlugin, "windrunner_boots", "&bWindrunner Boots");
        when(mockCustomItemRegistry.getAllItems()).thenReturn(Arrays.asList(berserker, windrunner));

        Player alpha = mock(Player.class);
        when(alpha.getName()).thenReturn("Alpha");
        Player bravo = mock(Player.class);
        when(bravo.getName()).thenReturn("Bravo");

        try (MockedStatic<Bukkit> mockedBukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getOnlinePlayers)
                    .thenReturn(Arrays.asList(alpha, bravo));

            List<String> playerCompletions = itemAdminCommand.onTabComplete(mockSender, new String[]{"give", ""});
            assertEquals(List.of("Alpha", "Bravo"), playerCompletions);

            List<String> itemCompletions = itemAdminCommand.onTabComplete(
                    mockSender,
                    new String[]{"give", "Alpha", "ber"}
            );
            assertEquals(List.of("berserker_gauntlet"), itemCompletions);

            List<String> amountCompletions = itemAdminCommand.onTabComplete(
                    mockSender,
                    new String[]{"give", "Alpha", "berserker_gauntlet", ""}
            );
            assertEquals(List.of("1", "16", "32", "64"), amountCompletions);
        }
    }

    private static class SimpleTestItem extends CustomItem {
        private final String id;
        private final String displayName;
        private ItemStack nextStack;

        private SimpleTestItem(MMOCraftPlugin plugin, String id, String displayName) {
            super(plugin);
            this.id = id;
            this.displayName = displayName;
        }

        void setNextStack(ItemStack nextStack) {
            this.nextStack = nextStack;
        }

        @Override
        public String getItemId() {
            return id;
        }

        @Override
        public Material getMaterial() {
            return Material.STONE;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public List<String> getLore() {
            return Collections.emptyList();
        }

        @Override
        public ItemStack createItemStack(int amount) {
            if (nextStack == null) {
                throw new IllegalStateException("Test stack not configured");
            }
            return nextStack;
        }
    }
}
