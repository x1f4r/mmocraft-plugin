package com.x1f4r.mmocraft.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        return LegacyComponentSerializer.legacySection().serialize(component);
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
        // First, colorize the string using our updated colorize method (which uses Adventure internally)
        // This ensures that legacy codes (including hex if present and supported by legacyAmpersand) are processed.
        String coloredText = colorize(text);
        // Then, deserialize this potentially §-coded string and serialize to plain.
        Component component = LegacyComponentSerializer.legacySection().deserialize(coloredText);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
