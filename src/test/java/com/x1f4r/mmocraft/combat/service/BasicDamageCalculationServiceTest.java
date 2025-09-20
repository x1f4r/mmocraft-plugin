package com.x1f4r.mmocraft.combat.service;

import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig.MobScalingSettings;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BasicDamageCalculationServiceTest {

    @Test
    void calculatesFerocityExtraHitsAgainstMobs() {
        PlayerDataService playerDataService = mock(PlayerDataService.class);
        LoggingUtil loggingUtil = mock(LoggingUtil.class);
        MobStatProvider mobStatProvider = mock(MobStatProvider.class);
        GameplayConfigService gameplayConfigService = mock(GameplayConfigService.class);

        RuntimeStatConfig runtimeConfig = RuntimeStatConfig.defaults();
        when(gameplayConfigService.getRuntimeStatConfig()).thenReturn(runtimeConfig);

        BasicDamageCalculationService service = new BasicDamageCalculationService(playerDataService, loggingUtil, mobStatProvider, gameplayConfigService);

        Player attacker = mock(Player.class);
        UUID attackerId = UUID.randomUUID();
        when(attacker.getUniqueId()).thenReturn(attackerId);
        when(attacker.getName()).thenReturn("Attacker");

        LivingEntity victim = mock(LivingEntity.class);
        UUID victimId = UUID.randomUUID();
        when(victim.getUniqueId()).thenReturn(victimId);
        when(victim.getType()).thenReturn(EntityType.ZOMBIE);
        when(victim.hasMetadata(anyString())).thenReturn(false);

        PlayerProfile attackerProfile = mock(PlayerProfile.class);
        when(attackerProfile.getStatValue(Stat.STRENGTH)).thenReturn(50.0);
        when(attackerProfile.getStatValue(Stat.FEROCITY)).thenReturn(200.0);
        when(attackerProfile.getCriticalHitChance()).thenReturn(0.0);
        when(attackerProfile.getCriticalDamageBonus()).thenReturn(2.0);
        when(attackerProfile.getLevel()).thenReturn(1);
        when(playerDataService.getPlayerProfile(attackerId)).thenReturn(attackerProfile);
        when(playerDataService.getPlayerProfile(victimId)).thenReturn(null);

        when(mobStatProvider.getBaseDefense(EntityType.ZOMBIE)).thenReturn(0.0);

        DamageInstance instance = service.calculateDamage(attacker, victim, 100.0, DamageType.PHYSICAL);

        assertEquals(375.0, instance.finalDamage(), 1e-6);
        assertEquals(125.0, instance.baseDamage(), 1e-6);
        assertTrue(instance.mitigationDetails().contains("Ferocity:2x"));
        assertFalse(instance.criticalHit());
        assertFalse(instance.evaded());
    }

    @Test
    void appliesVictimDamageReduction() {
        PlayerDataService playerDataService = mock(PlayerDataService.class);
        LoggingUtil loggingUtil = mock(LoggingUtil.class);
        MobStatProvider mobStatProvider = mock(MobStatProvider.class);
        GameplayConfigService gameplayConfigService = mock(GameplayConfigService.class);

        RuntimeStatConfig runtimeConfig = RuntimeStatConfig.defaults();
        when(gameplayConfigService.getRuntimeStatConfig()).thenReturn(runtimeConfig);

        BasicDamageCalculationService service = new BasicDamageCalculationService(playerDataService, loggingUtil, mobStatProvider, gameplayConfigService);

        Player attacker = mock(Player.class);
        UUID attackerId = UUID.randomUUID();
        when(attacker.getUniqueId()).thenReturn(attackerId);
        when(attacker.getName()).thenReturn("Attacker");

        Player victim = mock(Player.class);
        UUID victimId = UUID.randomUUID();
        when(victim.getUniqueId()).thenReturn(victimId);
        when(victim.getName()).thenReturn("Victim");

        PlayerProfile attackerProfile = mock(PlayerProfile.class);
        when(attackerProfile.getStatValue(Stat.STRENGTH)).thenReturn(50.0);
        when(attackerProfile.getStatValue(Stat.FEROCITY)).thenReturn(0.0);
        when(attackerProfile.getCriticalHitChance()).thenReturn(0.0);
        when(attackerProfile.getCriticalDamageBonus()).thenReturn(2.0);
        when(playerDataService.getPlayerProfile(attackerId)).thenReturn(attackerProfile);

        PlayerProfile victimProfile = mock(PlayerProfile.class);
        when(victimProfile.getPhysicalDamageReduction()).thenReturn(0.4);
        when(victimProfile.getMagicDamageReduction()).thenReturn(0.4);
        when(victimProfile.getEvasionChance()).thenReturn(0.0);
        when(playerDataService.getPlayerProfile(victimId)).thenReturn(victimProfile);

        DamageInstance instance = service.calculateDamage(attacker, victim, 100.0, DamageType.PHYSICAL);

        assertEquals(75.0, instance.finalDamage(), 1e-6);
        assertTrue(instance.mitigationDetails().contains("P.Reduc"));
    }

    @Test
    void scalesMobDamageAgainstPlayerLevel() {
        PlayerDataService playerDataService = mock(PlayerDataService.class);
        LoggingUtil loggingUtil = mock(LoggingUtil.class);
        MobStatProvider mobStatProvider = mock(MobStatProvider.class);
        GameplayConfigService gameplayConfigService = mock(GameplayConfigService.class);

        RuntimeStatConfig runtimeConfig = RuntimeStatConfig.defaults().toBuilder()
                .mobScalingSettings(MobScalingSettings.builder()
                        .healthPerLevelPercent(0.05)
                        .damagePerLevelPercent(0.05)
                        .defensePerLevel(0.5)
                        .maxHealthMultiplier(5.0)
                        .maxDamageMultiplier(3.0)
                        .maxDefenseBonus(200.0))
                .build();
        when(gameplayConfigService.getRuntimeStatConfig()).thenReturn(runtimeConfig);

        BasicDamageCalculationService service = new BasicDamageCalculationService(playerDataService, loggingUtil, mobStatProvider, gameplayConfigService);

        LivingEntity mob = mock(LivingEntity.class);
        UUID mobId = UUID.randomUUID();
        when(mob.getUniqueId()).thenReturn(mobId);
        when(mob.getType()).thenReturn(EntityType.ZOMBIE);
        when(mob.getName()).thenReturn("Zombie");

        Player victim = mock(Player.class);
        UUID victimId = UUID.randomUUID();
        when(victim.getUniqueId()).thenReturn(victimId);
        when(victim.getName()).thenReturn("Victim");

        PlayerProfile victimProfile = mock(PlayerProfile.class);
        when(victimProfile.getLevel()).thenReturn(10);
        when(victimProfile.getEvasionChance()).thenReturn(0.0);
        when(victimProfile.getPhysicalDamageReduction()).thenReturn(0.0);
        when(victimProfile.getMagicDamageReduction()).thenReturn(0.0);

        when(playerDataService.getPlayerProfile(mobId)).thenReturn(null);
        when(playerDataService.getPlayerProfile(victimId)).thenReturn(victimProfile);

        DamageInstance instance = service.calculateDamage(mob, victim, 100.0, DamageType.PHYSICAL);

        assertEquals(145.0, instance.finalDamage(), 1e-6);
        assertTrue(instance.mitigationDetails().contains("MobScale:+45%"));
    }
}
