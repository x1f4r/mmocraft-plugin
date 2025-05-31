package com.x1f4r.mmocraft.util;

import org.bukkit.ChatColor;

public class StringUtil {

    /**
     * Translates Minecraft color codes (e.g., &c) in a string.
     *
     * @param text The string to colorize.
     * @return The colorized string.
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // This is the standard Bukkit way to translate color codes.
        // It supports &0-&9, &a-&f, &k-&o, &r.
        // For hex colors (e.g., &#RRGGBB), the server platform (Paper, Spigot, Purpur) needs to support it.
        // Purpur, being a Paper fork, should support hex colors with this method.
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Joins an array of strings with a delimiter.
     * @param args Array of strings to join.
     * @param delimiter Delimiter to use between strings.
     * @param startIndex Index to start joining from.
     * @return The joined string.
     */
    public static String joinString(String[] args, String delimiter, int startIndex) {
        if (args == null || args.length == 0) {
            return "";
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (startIndex >= args.length) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                sb.append(delimiter);
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    /**
     * Strips all color codes from a string.
     * @param text The string to strip color from.
     * @return The string without color codes.
     */
    public static String stripColor(String text) {
        if (text == null) {
            return null;
        }
        return ChatColor.stripColor(colorize(text)); // Colorize first to handle & then strip
    }
}
