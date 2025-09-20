package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.skill.BerserkerRageSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ferocious gauntlet emphasizing melee burst.
 */
public class BerserkerGauntlet extends CustomItem {

    public static final String ITEM_ID = "berserker_gauntlet";

    public BerserkerGauntlet(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "&dBerserker Gauntlet";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7A gauntlet that channels molten fury into each swing.",
                "&7Unlocks a rage window to shred foes."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.STRENGTH, 60.0);
        mods.put(Stat.ATTACK_SPEED, 35.0);
        mods.put(Stat.FEROCITY, 25.0);
        mods.put(Stat.CRITICAL_CHANCE, 15.0);
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
                BerserkerRageSkill.SKILL_ID,
                BerserkerRageSkill.DISPLAY_NAME,
                "Right Click",
                BerserkerRageSkill.DESCRIPTION,
                BerserkerRageSkill.MANA_COST,
                BerserkerRageSkill.COOLDOWN_SECONDS));
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Infusion Altar: combine a diamond axe, magma cream, rabbit feet, and netherite scrap from mining commissions."
        );
    }
}
