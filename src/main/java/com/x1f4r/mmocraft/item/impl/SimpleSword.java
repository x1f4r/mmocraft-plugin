package com.x1f4r.mmocraft.item.impl;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SimpleSword extends CustomItem {

    public SimpleSword(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return "simple_sword";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "&fSimple Iron Sword";
    }

    @Override
    public List<String> getLore() {
        return List.of(
            "&7A basic, reliable sword."
            // Stat modifiers will be added automatically by CustomItem.createItemStack()
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.STRENGTH, 5.0);
        // This sword could also give a small amount of agility or other stats
        // mods.put(Stat.AGILITY, 1.0);
        return mods;
    }

    @Override
    public boolean isUnbreakable() {
        return true; // Example: Make this custom item unbreakable
    }

    @Override
    public com.x1f4r.mmocraft.item.model.ItemRarity getRarity() {
        return com.x1f4r.mmocraft.item.model.ItemRarity.UNCOMMON;
    }
}
