package com.example.dialognpc.util;

import java.util.Map;

/**
 * Utility class providing Minecraft's 16 formatting colors as ARGB integers.
 * These match the colors used by formatting codes (§0-§f) in Minecraft.
 */
public class MinecraftColors {

    // ARGB color values matching Minecraft's formatting colors
    public static final int BLACK       = 0xFF000000;
    public static final int DARK_BLUE   = 0xFF0000AA;
    public static final int DARK_GREEN  = 0xFF00AA00;
    public static final int DARK_AQUA   = 0xFF00AAAA;
    public static final int DARK_RED    = 0xFFAA0000;
    public static final int DARK_PURPLE = 0xFFAA00AA;
    public static final int GOLD        = 0xFFFFAA00;
    public static final int GRAY        = 0xFFAAAAAA;
    public static final int DARK_GRAY   = 0xFF555555;
    public static final int BLUE        = 0xFF5555FF;
    public static final int GREEN       = 0xFF55FF55;
    public static final int AQUA        = 0xFF55FFFF;
    public static final int RED         = 0xFFFF5555;
    public static final int LIGHT_PURPLE= 0xFFFF55FF;
    public static final int YELLOW      = 0xFFFFFF55;
    public static final int WHITE       = 0xFFFFFFFF;

    // Name to color mapping for string-based lookup
    private static final Map<String, Integer> NAME_TO_COLOR = Map.ofEntries(
        Map.entry("black", BLACK),
        Map.entry("dark_blue", DARK_BLUE),
        Map.entry("dark_green", DARK_GREEN),
        Map.entry("dark_aqua", DARK_AQUA),
        Map.entry("dark_red", DARK_RED),
        Map.entry("dark_purple", DARK_PURPLE),
        Map.entry("gold", GOLD),
        Map.entry("gray", GRAY),
        Map.entry("dark_gray", DARK_GRAY),
        Map.entry("blue", BLUE),
        Map.entry("green", GREEN),
        Map.entry("aqua", AQUA),
        Map.entry("red", RED),
        Map.entry("light_purple", LIGHT_PURPLE),
        Map.entry("yellow", YELLOW),
        Map.entry("white", WHITE)
    );

    /**
     * Get the ARGB color by name (case-insensitive).
     * Returns WHITE if the name is not recognized.
     */
    public static int getColor(String name) {
        return NAME_TO_COLOR.getOrDefault(name.toLowerCase(), WHITE);
    }

    /**
     * Check if a color name is valid.
     */
    public static boolean isValidColorName(String name) {
        return NAME_TO_COLOR.containsKey(name.toLowerCase());
    }

    /**
     * Get all valid color names.
     */
    public static String[] getColorNames() {
        return NAME_TO_COLOR.keySet().toArray(new String[0]);
    }
}
