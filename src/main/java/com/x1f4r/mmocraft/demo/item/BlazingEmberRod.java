package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.skill.InfernoBurstSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mythic weapon inspired by Hypixel's Ember Rod.
 */
public class BlazingEmberRod extends CustomItem {

    public static final String ITEM_ID = "blazing_ember_rod";

    public BlazingEmberRod(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.BLAZE_ROD;
    }

    @Override
    public String getDisplayName() {
        return "&cBlazing Ember Rod";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Forged from the heart of ember commissions.",
                "&7Excels at ability-based burst damage."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.STRENGTH, 35.0);
        mods.put(Stat.INTELLIGENCE, 90.0);
        mods.put(Stat.ABILITY_POWER, 55.0);
        mods.put(Stat.MANA_REGEN, 15.0);
        mods.put(Stat.CRITICAL_DAMAGE, 45.0);
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
                InfernoBurstSkill.SKILL_ID,
                InfernoBurstSkill.DISPLAY_NAME,
                "Right Click",
                InfernoBurstSkill.DESCRIPTION,
                InfernoBurstSkill.MANA_COST,
                InfernoBurstSkill.COOLDOWN_SECONDS));
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Infusion Altar: meld blaze rods, magma cream, blaze powder, and ghast tears gathered from ember commissions."
        );
    }
}
