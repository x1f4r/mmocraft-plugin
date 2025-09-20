package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.skill.GaleForceDashSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Speed-oriented boots inspired by SkyBlock footwear.
 */
public class WindrunnerBoots extends CustomItem {

    public static final String ITEM_ID = "windrunner_boots";

    public WindrunnerBoots(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "&bWindrunner Boots";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Lightweight boots woven with phantom membranes.",
                "&7Pairs with a gust dash for rapid traversal."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.SPEED, 90.0);
        mods.put(Stat.EVASION, 15.0);
        mods.put(Stat.DEFENSE, 25.0);
        mods.put(Stat.MAGIC_FIND, 10.0);
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
    public List<ItemAbilityDescriptor> getAbilityDescriptors() {
        return List.of(new ItemAbilityDescriptor(
                GaleForceDashSkill.SKILL_ID,
                GaleForceDashSkill.DISPLAY_NAME,
                "Sneak + Right Click",
                GaleForceDashSkill.DESCRIPTION,
                GaleForceDashSkill.MANA_COST,
                GaleForceDashSkill.COOLDOWN_SECONDS));
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Workbench: stitch leather boots with feathers, sugar, and phantom membranes dropped by wind commissions."
        );
    }
}
