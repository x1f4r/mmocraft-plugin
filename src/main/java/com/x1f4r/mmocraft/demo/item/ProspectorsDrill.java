package com.x1f4r.mmocraft.demo.item;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.skill.ProspectorPulseSkill;
import com.x1f4r.mmocraft.item.model.CustomItem;
import com.x1f4r.mmocraft.item.model.ItemAbilityDescriptor;
import com.x1f4r.mmocraft.item.model.ItemRarity;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * High tier mining tool that accelerates commissions.
 */
public class ProspectorsDrill extends CustomItem {

    public static final String ITEM_ID = "prospectors_drill";

    public ProspectorsDrill(MMOCraftPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getItemId() {
        return ITEM_ID;
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_PICKAXE;
    }

    @Override
    public String getDisplayName() {
        return "&6Prospector's Drill";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "&7Commission-ready drill that extracts ores at blistering speed.",
                "&7Pulse it to gain haste and fortune."
        );
    }

    @Override
    public Map<Stat, Double> getStatModifiers() {
        Map<Stat, Double> mods = new EnumMap<>(Stat.class);
        mods.put(Stat.MINING_SPEED, 250.0);
        mods.put(Stat.MINING_FORTUNE, 150.0);
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
                ProspectorPulseSkill.SKILL_ID,
                ProspectorPulseSkill.DISPLAY_NAME,
                "Right Click",
                ProspectorPulseSkill.DESCRIPTION,
                ProspectorPulseSkill.MANA_COST,
                ProspectorPulseSkill.COOLDOWN_SECONDS));
    }

    @Override
    public List<String> getRecipeHints() {
        return List.of(
                "Infusion Altar: infuse a diamond pickaxe with redstone blocks, gold blocks, and emeralds from mining commissions."
        );
    }
}
