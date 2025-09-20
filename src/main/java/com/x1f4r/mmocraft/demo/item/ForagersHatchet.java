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
 * Foraging fortune tool to complete gathering stat coverage.
 */
public class ForagersHatchet extends CustomItem {

    public static final String ITEM_ID = "foragers_hatchet";

    public ForagersHatchet(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND_AXE;
    }

    @Override
    public String getDisplayName() {
        return "&aForager's Hatchet";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Balanced axe tuned for grove commissions.",
                "&7Improves foraging fortune while adding a touch of power."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.FORAGING_FORTUNE, 160.0);
        mods.put(Stat.STRENGTH, 20.0);
        mods.put(Stat.SPEED, 10.0);
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
                "Workbench: combine a diamond axe with freshly cut logs and bone meal from grove commissions."
        );
    }
}
