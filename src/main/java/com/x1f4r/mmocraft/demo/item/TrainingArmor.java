package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight defensive armor used to showcase armor stat modifiers.
 */
public class TrainingArmor extends CustomItem {

    public TrainingArmor(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return "training_chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "&aTraining Chestplate";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Provides minor protection and resilience.",
                "&7Suitable for novice adventurers."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.HEALTH, 25.0);
        mods.put(Stat.DEFENSE, 5.0);
        return mods;
    }

    @Override
    public int getCustomModelData() {
        return 0;
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }
}
