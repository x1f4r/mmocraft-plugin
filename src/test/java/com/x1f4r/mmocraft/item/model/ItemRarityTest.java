package com.x1f4r.mmocraft.item.model;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemRarityTest {

    @Test
    void getDisplayName_returnsCorrectlyFormattedString() {
        assertEquals("&fCommon", ItemRarity.COMMON.getDisplayName());
        assertEquals("&aUncommon", ItemRarity.UNCOMMON.getDisplayName());
        assertEquals("&9Rare", ItemRarity.RARE.getDisplayName());
        assertEquals("&5Epic", ItemRarity.EPIC.getDisplayName());
        assertEquals("&6Legendary", ItemRarity.LEGENDARY.getDisplayName());
        assertEquals("&cMythic", ItemRarity.MYTHIC.getDisplayName());
        assertEquals("&eUnique", ItemRarity.UNIQUE.getDisplayName());
    }

    @Test
    void getChatColor_returnsCorrectBukkitChatColor() {
        assertEquals(ChatColor.WHITE, ItemRarity.COMMON.getChatColor());
        assertEquals(ChatColor.GREEN, ItemRarity.UNCOMMON.getChatColor());
        assertEquals(ChatColor.BLUE, ItemRarity.RARE.getChatColor());
        assertEquals(ChatColor.DARK_PURPLE, ItemRarity.EPIC.getChatColor());
        assertEquals(ChatColor.GOLD, ItemRarity.LEGENDARY.getChatColor());
        assertEquals(ChatColor.RED, ItemRarity.MYTHIC.getChatColor());
        assertEquals(ChatColor.YELLOW, ItemRarity.UNIQUE.getChatColor());
    }

    @Test
    void getPlainDisplayName_returnsCorrectPlainString() {
        assertEquals("Common", ItemRarity.COMMON.getPlainDisplayName());
        assertEquals("Uncommon", ItemRarity.UNCOMMON.getPlainDisplayName());
        assertEquals("Rare", ItemRarity.RARE.getPlainDisplayName());
        assertEquals("Epic", ItemRarity.EPIC.getPlainDisplayName());
        assertEquals("Legendary", ItemRarity.LEGENDARY.getPlainDisplayName());
        assertEquals("Mythic", ItemRarity.MYTHIC.getPlainDisplayName());
        assertEquals("Unique", ItemRarity.UNIQUE.getPlainDisplayName());
    }
}
