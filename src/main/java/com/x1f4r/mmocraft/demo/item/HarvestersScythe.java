package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.skill.HarvestRallySkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Farming fortune tool to highlight island mini-loops.
 */
public class HarvestersScythe extends CustomItem {

    public static final String ITEM_ID = "harvesters_scythe";

    public HarvestersScythe(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_HOE;
    }

    @Override
    public String getDisplayName() {
        return "&eHarvester's Scythe";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Designed for farming islands and crop commissions.",
                "&7Bolsters farming fortune and mobility."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.FARMING_FORTUNE, 180.0);
        mods.put(Stat.SPEED, 30.0);
        return mods;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.MYTHIC;
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public List<ItemAbilityDescriptor> getAbilityDescriptors() {
        return List.of(new ItemAbilityDescriptor(
                HarvestRallySkill.SKILL_ID,
                HarvestRallySkill.DISPLAY_NAME,
                "Right Click",
                HarvestRallySkill.DESCRIPTION,
                HarvestRallySkill.MANA_COST,
                HarvestRallySkill.COOLDOWN_SECONDS));
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Infusion Altar: empower a diamond hoe with hay bales, pumpkins, and sugar cane from farming islands."
        );
    }
}
