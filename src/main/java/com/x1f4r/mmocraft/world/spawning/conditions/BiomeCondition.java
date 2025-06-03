package com.x1f4r.mmocraft.world.spawning.conditions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BiomeCondition implements SpawnCondition {

    private final Set<Biome> allowedBiomes;

    public BiomeCondition(Set<Biome> allowedBiomes) {
        Set<Biome> set = new HashSet<>(allowedBiomes);
        this.allowedBiomes = Collections.unmodifiableSet(set);
    }

    public BiomeCondition(Biome... biomes) {
        Set<Biome> set = new HashSet<>();
        Collections.addAll(set, biomes);
        this.allowedBiomes = Collections.unmodifiableSet(set);
    }

    @Override
    public boolean check(Location location, World world, Player nearestPlayer) {
        if (location == null || world == null) {
            return false;
        }
        // Ensure the location's world matches the passed world context if necessary,
        // or rely on Bukkit to handle location.getBiome() correctly based on its world.
        // Biome biome = world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ()); // Bukkit API for specific coords
        Biome biome = location.getBlock().getBiome(); // Simpler way
        return allowedBiomes.contains(biome);
    }

    public Set<Biome> getAllowedBiomes() {
        return allowedBiomes;
    }
}
