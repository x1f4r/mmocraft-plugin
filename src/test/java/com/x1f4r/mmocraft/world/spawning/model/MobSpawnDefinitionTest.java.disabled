package com.x1f4r.mmocraft.world.spawning.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

class MobSpawnDefinitionTest {

    @Test
    void constructor_minimal_setsDefaults() {
        MobSpawnDefinition def = new MobSpawnDefinition("zombie_1", EntityType.ZOMBIE);
        assertEquals("zombie_1", def.getDefinitionId());
        assertEquals(EntityType.ZOMBIE, def.getEntityType());
        assertEquals(Optional.empty(), def.getDisplayName());
        assertEquals(EntityType.ZOMBIE.name(), def.getMobStatKey()); // Defaults to EntityType name
        assertEquals(Optional.empty(), def.getLootTableId());
        assertTrue(def.getEquipment().isEmpty());
    }

    @Test
    void constructor_full_setsAllFields() {
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HAND, new ItemStack(Material.IRON_SWORD));

        MobSpawnDefinition def = new MobSpawnDefinition(
            "armored_skeleton",
            EntityType.SKELETON,
            "&cSkeleton Knight",
            "SKELETON_KNIGHT_STATS",
            "skeleton_knight_loot",
            equipment
        );

        assertEquals("armored_skeleton", def.getDefinitionId());
        assertEquals(EntityType.SKELETON, def.getEntityType());
        assertEquals(Optional.of("&cSkeleton Knight"), def.getDisplayName());
        assertEquals("SKELETON_KNIGHT_STATS", def.getMobStatKey());
        assertEquals(Optional.of("skeleton_knight_loot"), def.getLootTableId());
        assertEquals(1, def.getEquipment().size());
        assertEquals(Material.IRON_SWORD, def.getEquipment().get(EquipmentSlot.HAND).getType());
    }

    @Test
    void constructor_nullRequiredFields_throwsException() {
        assertThrows(NullPointerException.class, () -> new MobSpawnDefinition(null, EntityType.ZOMBIE));
        assertThrows(NullPointerException.class, () -> new MobSpawnDefinition("id", null));
        assertThrows(NullPointerException.class, () -> new MobSpawnDefinition("id", EntityType.ZOMBIE, null, null, null, null));
    }

    @Test
    void getEquipment_returnsUnmodifiableMap() {
         Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HEAD, new ItemStack(Material.IRON_HELMET));
        MobSpawnDefinition def = new MobSpawnDefinition("test", EntityType.ZOMBIE, null, "ZOMBIE", null, equipment);

        Map<EquipmentSlot, ItemStack> retrievedEquipment = def.getEquipment();
        assertThrows(UnsupportedOperationException.class, () -> retrievedEquipment.put(EquipmentSlot.CHEST, new ItemStack(Material.DIAMOND_CHESTPLATE)));
    }

    @Test
    void equalsAndHashCode_basedOnId() {
        MobSpawnDefinition def1 = new MobSpawnDefinition("def_id_1", EntityType.ZOMBIE);
        MobSpawnDefinition def2 = new MobSpawnDefinition("def_id_1", EntityType.SKELETON); // Same ID, different type
        MobSpawnDefinition def3 = new MobSpawnDefinition("def_id_2", EntityType.ZOMBIE); // Different ID

        assertEquals(def1, def2);
        assertEquals(def1.hashCode(), def2.hashCode());
        assertNotEquals(def1, def3);
        assertNotEquals(def1.hashCode(), def3.hashCode());
    }
}
