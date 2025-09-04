package com.x1f4r.mmocraft.world.spawning.conditions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections; // Added
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiomeConditionTest {

    @Mock private Location mockLocation;
    @Mock private World mockWorld;
    @Mock private Player mockPlayer; // nearestPlayer, not directly used by BiomeCondition
    @Mock private Block mockBlock;

    @BeforeEach
    void setUp() {
        when(mockLocation.getBlock()).thenReturn(mockBlock);
        // Link location to world for getBiome(x,y,z) if that was used, but getBlock().getBiome() is simpler.
        lenient().when(mockLocation.getWorld()).thenReturn(mockWorld);
    }

    @Test
    void check_biomeInAllowedSet_returnsTrue() {
        when(mockBlock.getBiome()).thenReturn(Biome.PLAINS);
        BiomeCondition condition = new BiomeCondition(Set.of(Biome.PLAINS, Biome.FOREST));
        assertTrue(condition.check(mockLocation, mockWorld, mockPlayer));
    }

    @Test
    void check_biomeNotInAllowedSet_returnsFalse() {
        when(mockBlock.getBiome()).thenReturn(Biome.DESERT);
        BiomeCondition condition = new BiomeCondition(Set.of(Biome.PLAINS, Biome.FOREST));
        assertFalse(condition.check(mockLocation, mockWorld, mockPlayer));
    }

    @Test
    void check_emptyAllowedSet_returnsFalse() {
        when(mockBlock.getBiome()).thenReturn(Biome.PLAINS);
        BiomeCondition condition = new BiomeCondition(Collections.emptySet()); // Corrected to use Collections.emptySet()
        assertFalse(condition.check(mockLocation, mockWorld, mockPlayer));
    }

    @Test
    void check_varArgsConstructor_works() {
        when(mockBlock.getBiome()).thenReturn(Biome.TAIGA);
        BiomeCondition condition = new BiomeCondition(Biome.TAIGA, Biome.SNOWY_PLAINS);
        assertTrue(condition.check(mockLocation, mockWorld, mockPlayer));
    }


    @Test
    void check_nullLocation_returnsFalse() {
        BiomeCondition condition = new BiomeCondition(Set.of(Biome.PLAINS));
        assertFalse(condition.check(null, mockWorld, mockPlayer));
    }

    @Test
    void check_nullWorld_returnsFalse() {
        // This depends on how getBiome is called. If location.getBlock().getBiome() is used,
        // world parameter to check might not be strictly needed if location is self-contained.
        // However, good practice to ensure world context is valid.
        // The current BiomeCondition uses location.getBlock().getBiome(), so world param is not directly used in that path.
        // If it used world.getBiome(x,y,z), this test would be more critical.
        when(mockBlock.getBiome()).thenReturn(Biome.PLAINS);
        BiomeCondition condition = new BiomeCondition(Set.of(Biome.PLAINS));
        assertFalse(condition.check(mockLocation, null, mockPlayer)); // Testing robustness
    }
}
