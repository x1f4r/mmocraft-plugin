package com.x1f4r.mmocraft.combat.service;

import org.bukkit.entity.EntityType;
import java.util.EnumMap;
import java.util.Map;

public class DefaultMobStatProvider implements MobStatProvider {

    private static class MobStats {
        double health;
        double attackDamage;
        double defense;

        MobStats(double health, double attackDamage, double defense) {
            this.health = health;
            this.attackDamage = attackDamage;
            this.defense = defense;
        }
    }

    private final Map<EntityType, MobStats> mobStatsMap = new EnumMap<>(EntityType.class);

    // Default stats for unlisted mobs
    private final MobStats defaultStats = new MobStats(10.0, 1.0, 0.0);

    public DefaultMobStatProvider() {
        // Populate with some common mob stats
        // Values are examples and should be balanced for actual gameplay
        mobStatsMap.put(EntityType.ZOMBIE, new MobStats(20.0, 3.0, 2.0));
        mobStatsMap.put(EntityType.SKELETON, new MobStats(20.0, 4.0, 0.0)); // Attack is for melee; bow damage separate
        mobStatsMap.put(EntityType.SPIDER, new MobStats(16.0, 2.0, 0.0));
        mobStatsMap.put(EntityType.CREEPER, new MobStats(20.0, 0.0, 0.0)); // Attack damage is via explosion
        mobStatsMap.put(EntityType.ENDERMAN, new MobStats(40.0, 7.0, 1.0));
        mobStatsMap.put(EntityType.PIG, new MobStats(10.0, 0.0, 0.0)); // Passive mob
        mobStatsMap.put(EntityType.COW, new MobStats(10.0, 0.0, 0.0));
        mobStatsMap.put(EntityType.SHEEP, new MobStats(8.0, 0.0, 0.0));
        mobStatsMap.put(EntityType.CHICKEN, new MobStats(4.0, 0.0, 0.0));
        mobStatsMap.put(EntityType.SLIME, new MobStats(16.0, 2.0, 0.0)); // Varies by size
        mobStatsMap.put(EntityType.MAGMA_CUBE, new MobStats(16.0, 3.0, 1.0)); // Varies by size
        mobStatsMap.put(EntityType.BLAZE, new MobStats(20.0, 6.0, 1.0)); // Mixed ranged/melee
        mobStatsMap.put(EntityType.GHAST, new MobStats(10.0, 0.0, 0.0)); // Fireball attack
        mobStatsMap.put(EntityType.VINDICATOR, new MobStats(24.0, 13.0, 1.0)); // With axe
        mobStatsMap.put(EntityType.PILLAGER, new MobStats(24.0, 5.0, 0.0)); // With crossbow
        mobStatsMap.put(EntityType.RAVAGER, new MobStats(100.0, 12.0, 3.0));
        mobStatsMap.put(EntityType.WITHER_SKELETON, new MobStats(20.0, 8.0, 1.0));
        // ... add more mob types as needed
    }

    @Override
    public double getBaseHealth(EntityType type) {
        return mobStatsMap.getOrDefault(type, defaultStats).health;
    }

    @Override
    public double getBaseAttackDamage(EntityType type) {
        // Note: For mobs like Skeletons (bow) or Ghasts (fireball), this "base attack"
        // might be their melee or a generic value if their primary attack is projectile-based.
        // Projectile damage might be handled differently (e.g., defined on the projectile itself
        // or through specific event handling for those projectiles).
        return mobStatsMap.getOrDefault(type, defaultStats).attackDamage;
    }

    @Override
    public double getBaseDefense(EntityType type) {
        // This is a conceptual "defense points" value. How it translates to reduction
        // will be in the DamageCalculationService.
        return mobStatsMap.getOrDefault(type, defaultStats).defense;
    }
}
