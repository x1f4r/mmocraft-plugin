package com.x1f4r.mmocraft.core;

import org.bukkit.plugin.java.JavaPlugin;

public final class MMOCraftPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("MMOCraft core loaded!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MMOCraft shutting down.");
    }
}
