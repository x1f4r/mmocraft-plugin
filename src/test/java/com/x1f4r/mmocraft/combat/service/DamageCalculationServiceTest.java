package com.x1f4r.mmocraft.combat.service;

import com.x1f4r.mmocraft.combat.model.DamageInstance;
import com.x1f4r.mmocraft.combat.model.DamageType;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow; // For projectile testing
import org.bukkit.projectiles.ProjectileSource; // For projectile testing

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DamageCalculationServiceTest {

    @Mock private PlayerDataService mockPlayerDataService;
    @Mock private LoggingUtil mockLogger;

    @Mock private Player mockAttackerPlayer;
    @Mock private PlayerProfile mockAttackerProfile;
    @Mock private Player mockVictimPlayer;
    @Mock private PlayerProfile mockVictimProfile;
    @Mock private LivingEntity mockMobAttacker; // Generic mob
    @Mock private LivingEntity mockMobVictim;   // Generic mob
    @Mock private Arrow mockArrow; // For projectile testing
    @Mock private Player mockProjectileShooterPlayer; // Player shooting a projectile
    @Mock private PlayerProfile mockProjectileShooterProfile;


    private BasicDamageCalculationService damageCalcService;

    private final UUID attackerPlayerUUID = UUID.randomUUID();
    private final UUID victimPlayerUUID = UUID.randomUUID();
    private final UUID projectileShooterUUID = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        damageCalcService = new BasicDamageCalculationService(mockPlayerDataService, mockLogger);

        // Setup common player/profile mocks
        when(mockAttackerPlayer.getUniqueId()).thenReturn(attackerPlayerUUID);
        when(mockVictimPlayer.getUniqueId()).thenReturn(victimPlayerUUID);

        // When PlayerDataService is asked for these UUIDs, return the mock profiles
        lenient().when(mockPlayerDataService.getPlayerProfile(attackerPlayerUUID)).thenReturn(mockAttackerProfile);
        lenient().when(mockPlayerDataService.getPlayerProfile(victimPlayerUUID)).thenReturn(mockVictimProfile);

        // Projectile related mocks
        lenient().when(mockArrow.getShooter()).thenReturn(mockProjectileShooterPlayer);
        lenient().when(mockProjectileShooterPlayer.getUniqueId()).thenReturn(projectileShooterUUID);
        lenient().when(mockPlayerDataService.getPlayerProfile(projectileShooterUUID)).thenReturn(mockProjectileShooterProfile);
    }

    @Test
    void calculateDamage_playerVsPlayer_physical_noCritNoEvasion() {
        when(mockAttackerProfile.getStatValue(Stat.STRENGTH)).thenReturn(20.0); // Contributes 20*0.5 = 10 damage
        when(mockAttackerProfile.getCriticalHitChance()).thenReturn(0.0); // No crit
        when(mockVictimProfile.getEvasionChance()).thenReturn(0.0); // No evasion
        when(mockVictimProfile.getPhysicalDamageReduction()).thenReturn(0.1); // 10% reduction

        double baseWeaponDamage = 10.0;
        DamageInstance result = damageCalcService.calculateDamage(mockAttackerPlayer, mockVictimPlayer, baseWeaponDamage, DamageType.PHYSICAL);

        assertEquals(attackerPlayerUUID, result.attackerId());
        assertEquals(victimPlayerUUID, result.victimId());
        assertEquals(mockAttackerProfile, result.attackerProfile());
        assertEquals(mockVictimProfile, result.victimProfile());
        assertEquals(DamageType.PHYSICAL, result.type());
        assertFalse(result.criticalHit());
        assertFalse(result.evaded());

        double expectedBaseDamage = baseWeaponDamage + (20.0 * 0.5); // 10 + 10 = 20
        assertEquals(expectedBaseDamage, result.baseDamage(), 0.01);

        double expectedFinalDamage = expectedBaseDamage * (1.0 - 0.1); // 20 * 0.9 = 18
        assertEquals(expectedFinalDamage, result.finalDamage(), 0.01);
    }

    @Test
    void calculateDamage_playerVsPlayer_physical_criticalHit() {
        when(mockAttackerProfile.getStatValue(Stat.STRENGTH)).thenReturn(10.0); // 10 * 0.5 = 5 bonus
        when(mockAttackerProfile.getCriticalHitChance()).thenReturn(1.0); // Guaranteed crit
        when(mockAttackerProfile.getCriticalDamageBonus()).thenReturn(1.5); // 150% damage
        when(mockVictimProfile.getEvasionChance()).thenReturn(0.0);
        when(mockVictimProfile.getPhysicalDamageReduction()).thenReturn(0.2); // 20% reduction

        double baseWeaponDamage = 5.0;
        DamageInstance result = damageCalcService.calculateDamage(mockAttackerPlayer, mockVictimPlayer, baseWeaponDamage, DamageType.PHYSICAL);

        assertTrue(result.criticalHit());
        double damageAfterStrength = baseWeaponDamage + (10.0 * 0.5); // 5 + 5 = 10
        double damageAfterCrit = damageAfterStrength * 1.5; // 10 * 1.5 = 15
        assertEquals(damageAfterCrit, result.baseDamage(), 0.01, "BaseDamage in DamageInstance should be after crit for this test def");

        double expectedFinalDamage = damageAfterCrit * (1.0 - 0.2); // 15 * 0.8 = 12
        assertEquals(expectedFinalDamage, result.finalDamage(), 0.01);
    }

    @Test
    void calculateDamage_playerVsPlayer_magical_criticalHit() {
        when(mockAttackerProfile.getStatValue(Stat.INTELLIGENCE)).thenReturn(20.0); // 20 * 0.7 = 14 bonus
        when(mockAttackerProfile.getCriticalHitChance()).thenReturn(1.0); // Guaranteed crit
        when(mockAttackerProfile.getCriticalDamageBonus()).thenReturn(2.0); // 200% damage
        when(mockVictimProfile.getEvasionChance()).thenReturn(0.0);
        when(mockVictimProfile.getMagicDamageReduction()).thenReturn(0.1); // 10% magic reduction

        double baseSpellDamage = 10.0;
        DamageInstance result = damageCalcService.calculateDamage(mockAttackerPlayer, mockVictimPlayer, baseSpellDamage, DamageType.MAGICAL);

        assertTrue(result.criticalHit());
        double damageAfterInt = baseSpellDamage + (20.0 * 0.7); // 10 + 14 = 24
        double damageAfterCrit = damageAfterInt * 2.0; // 24 * 2.0 = 48
        assertEquals(damageAfterCrit, result.baseDamage(), 0.01);

        double expectedFinalDamage = damageAfterCrit * (1.0 - 0.1); // 48 * 0.9 = 43.2
        assertEquals(expectedFinalDamage, result.finalDamage(), 0.01);
    }

    @Test
    void calculateDamage_playerVsPlayer_evaded() {
        when(mockAttackerProfile.getStatValue(Stat.STRENGTH)).thenReturn(10.0);
        when(mockAttackerProfile.getCriticalHitChance()).thenReturn(0.0);
        when(mockVictimProfile.getEvasionChance()).thenReturn(1.0); // Guaranteed evasion

        DamageInstance result = damageCalcService.calculateDamage(mockAttackerPlayer, mockVictimPlayer, 10.0, DamageType.PHYSICAL);

        assertTrue(result.evaded());
        assertEquals(0, result.finalDamage(), 0.01);
    }

    @Test
    void calculateDamage_mobVsPlayer_noSpecialStatsForMob() {
        // Mob attacker, no profile
        when(mockVictimProfile.getEvasionChance()).thenReturn(0.0);
        when(mockVictimProfile.getPhysicalDamageReduction()).thenReturn(0.25); // 25% reduction

        double mobBaseDamage = 8.0;
        DamageInstance result = damageCalcService.calculateDamage(mockMobAttacker, mockVictimPlayer, mobBaseDamage, DamageType.PHYSICAL);

        assertNull(result.attackerProfile());
        assertEquals(mockVictimProfile, result.victimProfile());
        assertFalse(result.criticalHit()); // Mobs don't crit by default in this service
        assertFalse(result.evaded());
        assertEquals(mobBaseDamage, result.baseDamage(), 0.01); // Mob base damage is unchanged by stats

        double expectedFinalDamage = mobBaseDamage * (1.0 - 0.25); // 8.0 * 0.75 = 6.0
        assertEquals(expectedFinalDamage, result.finalDamage(), 0.01);
    }

    @Test
    void calculateDamage_playerVsMob_noSpecialStatsForMobVictim() {
        when(mockAttackerProfile.getStatValue(Stat.STRENGTH)).thenReturn(15.0); // 15 * 0.5 = 7.5 bonus
        when(mockAttackerProfile.getCriticalHitChance()).thenReturn(0.0);

        double baseWeaponDamage = 12.0;
        DamageInstance result = damageCalcService.calculateDamage(mockAttackerPlayer, mockMobVictim, baseWeaponDamage, DamageType.PHYSICAL);

        assertEquals(mockAttackerProfile, result.attackerProfile());
        assertNull(result.victimProfile()); // Mob victim has no profile

        double expectedDamage = baseWeaponDamage + (15.0 * 0.5); // 12 + 7.5 = 19.5
        assertEquals(expectedDamage, result.baseDamage(), 0.01);
        assertEquals(expectedDamage, result.finalDamage(), 0.01); // Mob takes full damage after attacker bonuses
    }

    @Test
    void calculateDamage_trueDamage_ignoresReductions() {
        when(mockAttackerProfile.getStatValue(Stat.STRENGTH)).thenReturn(0.0); // No bonus for simplicity
        when(mockAttackerProfile.getCriticalHitChance()).thenReturn(0.0);
        when(mockVictimProfile.getEvasionChance()).thenReturn(0.0);
        // These should be ignored for TRUE damage
        when(mockVictimProfile.getPhysicalDamageReduction()).thenReturn(0.5);
        when(mockVictimProfile.getMagicDamageReduction()).thenReturn(0.5);

        double baseTrueDamage = 20.0;
        DamageInstance result = damageCalcService.calculateDamage(mockAttackerPlayer, mockVictimPlayer, baseTrueDamage, DamageType.TRUE);

        assertEquals(baseTrueDamage, result.baseDamage(), 0.01);
        assertEquals(baseTrueDamage, result.finalDamage(), 0.01); // Final damage should be same as base for TRUE type
        assertTrue(result.mitigationDetails().isEmpty()); // No reduction details
    }

    @Test
    void calculateDamage_projectileFromPlayer_usesShooterProfile() {
        when(mockProjectileShooterProfile.getStatValue(Stat.STRENGTH)).thenReturn(30.0); // 30 * 0.5 = 15 bonus
        when(mockProjectileShooterProfile.getCriticalHitChance()).thenReturn(0.0);
        when(mockVictimProfile.getEvasionChance()).thenReturn(0.0);
        when(mockVictimProfile.getPhysicalDamageReduction()).thenReturn(0.1); // 10% reduction

        double arrowBaseDamage = 5.0; // Base damage of the arrow itself
        DamageInstance result = damageCalcService.calculateDamage(mockArrow, mockVictimPlayer, arrowBaseDamage, DamageType.PHYSICAL);

        assertEquals(projectileShooterUUID, result.attackerId());
        assertEquals(mockProjectileShooterProfile, result.attackerProfile());

        double expectedBaseDamage = arrowBaseDamage + (30.0 * 0.5); // 5 + 15 = 20
        assertEquals(expectedBaseDamage, result.baseDamage(), 0.01);

        double expectedFinalDamage = expectedBaseDamage * (1.0 - 0.1); // 20 * 0.9 = 18
        assertEquals(expectedFinalDamage, result.finalDamage(), 0.01);
    }
}
