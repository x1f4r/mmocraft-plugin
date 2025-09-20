package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.skill.TidalSurgeSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fishing fortune rod to showcase aquatic professions.
 */
public class AnglersTidalRod extends CustomItem {

    public static final String ITEM_ID = "anglers_tidal_rod";

    public AnglersTidalRod(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.FISHING_ROD;
    }

    @Override
    public String getDisplayName() {
        return "&3Angler's Tidal Rod";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Commission reward from the tidal fishing outpost.",
                "&7Ride the currents to secure treasure catches."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.FISHING_FORTUNE, 200.0);
        mods.put(Stat.MAGIC_FIND, 15.0);
        return mods;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public List<ItemAbilityDescriptor> getAbilityDescriptors() {
        return List.of(new ItemAbilityDescriptor(
                TidalSurgeSkill.SKILL_ID,
                TidalSurgeSkill.DISPLAY_NAME,
                "Right Click",
                TidalSurgeSkill.DESCRIPTION,
                TidalSurgeSkill.MANA_COST,
                TidalSurgeSkill.COOLDOWN_SECONDS));
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Infusion Altar: refine a fishing rod with prismarine shards, crystals, and nautilus shells from tidal commissions."
        );
    }
}
