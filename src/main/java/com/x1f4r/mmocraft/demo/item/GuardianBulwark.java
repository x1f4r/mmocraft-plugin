package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Defensive legendary chestplate showcasing mitigation stats.
 */
public class GuardianBulwark extends CustomItem {

    public static final String ITEM_ID = "guardian_bulwark";

    public GuardianBulwark(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "&9Guardian Bulwark";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Anvil-reinforced plate favored by stalwart tanks.",
                "&7Massively boosts survivability stats."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.HEALTH, 220.0);
        mods.put(Stat.DEFENSE, 160.0);
        mods.put(Stat.TRUE_DEFENSE, 45.0);
        return mods;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Workbench: surround a diamond chestplate with iron blocks and diamonds to forge a fortified bulwark."
        );
    }
}
