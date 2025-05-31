package com.x1f4r.mmocraft.util;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void colorize_shouldTranslateAmpersandColorCodes() {
        assertEquals(ChatColor.RED + "Hello" + ChatColor.GREEN + " World", StringUtil.colorize("&cHello&a World"));
        assertEquals(ChatColor.BOLD + "Bold" + ChatColor.RESET + " Normal", StringUtil.colorize("&lBold&r Normal"));
        assertEquals("No codes here", StringUtil.colorize("No codes here"));
        assertEquals(ChatColor.RED.toString() + ChatColor.GREEN.toString() + ChatColor.BLUE.toString(), StringUtil.colorize("&c&a&b"));
    }

    @Test
    void colorize_shouldHandleEmptyAndNullStrings() {
        assertEquals("", StringUtil.colorize(""));
        assertEquals("", StringUtil.colorize(null));
    }

    @Test
    void colorize_shouldHandleHexColorsIfServerSupportsIt() {
        // Purpur (Paper fork) supports hex via ChatColor.translateAlternateColorCodes
        // This test assumes the underlying Bukkit implementation (or server) handles it.
        // Example: &#RRGGBB -> ChatColor.of("#RRGGBB") internal conversion
        String hexRed = "&#FF0000";
        String expectedBukkitHexRed = ChatColor.COLOR_CHAR + "x" +
                                      ChatColor.COLOR_CHAR + "F" + ChatColor.COLOR_CHAR + "F" +
                                      ChatColor.COLOR_CHAR + "0" + ChatColor.COLOR_CHAR + "0" +
                                      ChatColor.COLOR_CHAR + "0" + ChatColor.COLOR_CHAR + "0";
        assertEquals(expectedBukkitHexRed + "Hello", StringUtil.colorize(hexRed + "Hello"));

        String mixed = "&cHello &#00FF00World"; // Red Hello, Green World
        String expectedMixed = ChatColor.RED + "Hello" +
                               ChatColor.COLOR_CHAR + "x" +
                               ChatColor.COLOR_CHAR + "0" + ChatColor.COLOR_CHAR + "0" +
                               ChatColor.COLOR_CHAR + "F" + ChatColor.COLOR_CHAR + "F" +
                               ChatColor.COLOR_CHAR + "0" + ChatColor.COLOR_CHAR + "0" +
                               "World";
        assertEquals(expectedMixed, StringUtil.colorize(mixed));
    }

    @Test
    void joinString_shouldJoinStringsCorrectly() {
        String[] args = {"one", "two", "three", "four"};
        assertEquals("one two three four", StringUtil.joinString(args, " ", 0));
        assertEquals("two three four", StringUtil.joinString(args, " ", 1));
        assertEquals("three-four", StringUtil.joinString(args, "-", 2));
        assertEquals("four", StringUtil.joinString(args, " ", 3));
    }

    @Test
    void joinString_shouldHandleEdgeCases() {
        assertEquals("", StringUtil.joinString(new String[]{"one"}, " ", 1));
        assertEquals("", StringUtil.joinString(new String[]{}, " ", 0));
        assertEquals("one", StringUtil.joinString(new String[]{"one"}, " ", 0));
        assertEquals("", StringUtil.joinString(null, " ", 0));
        assertEquals("", StringUtil.joinString(new String[]{"one", "two"}, " ", 5)); // startIndex out of bounds
        assertEquals("one two", StringUtil.joinString(new String[]{"one", "two"}, " ", -1)); // startIndex negative
    }

    @Test
    void stripColor_shouldRemoveAmpersandAndSectionColorCodes() {
        assertEquals("Hello World", StringUtil.stripColor("&cHello &aWorld"));
        assertEquals("Bold Normal", StringUtil.stripColor("&lBold&r Normal"));
        // Assuming hex colorization works and then is stripped
        assertEquals("Hex Color", StringUtil.stripColor("&#FF0000Hex &#00FF00Color"));
        assertEquals("No codes", StringUtil.stripColor("No codes"));
    }

    @Test
    void stripColor_shouldHandleEmptyAndNullStrings() {
        assertEquals(null, StringUtil.stripColor(null)); // ChatColor.stripColor returns null for null input
        assertEquals("", StringUtil.stripColor(""));
    }
}
