package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.skill.LuckySpriteSummonSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Magic find trinket analogous to SkyBlock accessories.
 */
public class LuckyCharmTalisman extends CustomItem {

    public static final String ITEM_ID = "lucky_charm_talisman";
    public LuckyCharmTalisman(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.EMERALD;
    }

    @Override
    public String getDisplayName() {
        return "&aLucky Charm";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Carry in your inventory to boost rare drop luck.",
                "&7Right click to beckon a Lucky Sprite companion."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.MAGIC_FIND, 50.0);
        mods.put(Stat.PET_LUCK, 40.0);
        mods.put(Stat.MANA_REGEN, 5.0);
        return mods;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public List<ItemAbilityDescriptor> getAbilityDescriptors() {
        return List.of(new ItemAbilityDescriptor(
                LuckySpriteSummonSkill.SKILL_ID,
                LuckySpriteSummonSkill.DISPLAY_NAME,
                "Right Click",
                LuckySpriteSummonSkill.DESCRIPTION,
                LuckySpriteSummonSkill.MANA_COST,
                LuckySpriteSummonSkill.COOLDOWN_SECONDS));
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Workbench: craft using gold ingots, emeralds, rabbit feet, and lapis from farming and mining loops."
        );
    }
}
