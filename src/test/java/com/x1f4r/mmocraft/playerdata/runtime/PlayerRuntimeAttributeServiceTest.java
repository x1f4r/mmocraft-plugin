package com.x1f4r.mmocraft.playerdata.runtime;

import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlayerRuntimeAttributeServiceTest {

    @Test
    void syncPlayerAppliesAttributesAndCachesSnapshot() {
        PlayerDataService playerDataService = mock(PlayerDataService.class);
        LoggingUtil loggingUtil = mock(LoggingUtil.class);
        RuntimeStatConfig runtimeConfig = RuntimeStatConfig.defaults();
        TestHasteEffectApplier hasteApplier = new TestHasteEffectApplier(false);
        AttributeInstance maxHealthAttribute = mock(AttributeInstance.class);
        AttributeInstance attackSpeedAttribute = mock(AttributeInstance.class);
        TestAttributeResolver attributeResolver = new TestAttributeResolver(maxHealthAttribute, attackSpeedAttribute);
        PlayerRuntimeAttributeService service = new PlayerRuntimeAttributeService(playerDataService, runtimeConfig, loggingUtil, hasteApplier, attributeResolver);

        UUID playerId = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(true);
        when(player.getName()).thenReturn("Tester");
        when(player.getHealth()).thenReturn(140.0, 150.0, 150.0);

        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getMaxHealth()).thenReturn(200L);
        when(profile.getCurrentHealth()).thenReturn(150L);
        when(profile.getStatValue(Stat.SPEED)).thenReturn(150.0);
        when(profile.getStatValue(Stat.ATTACK_SPEED)).thenReturn(50.0);
        when(profile.getStatValue(Stat.MINING_SPEED)).thenReturn(0.0);
        when(playerDataService.getPlayerProfile(playerId)).thenReturn(profile);

        service.syncPlayer(player);
        service.syncPlayer(player);
        service.clearCache(playerId);
        service.syncPlayer(player);

        verify(maxHealthAttribute, times(2)).setBaseValue(200.0);
        verify(player, times(2)).setWalkSpeed(0.3f);
        verify(attackSpeedAttribute, times(2)).setBaseValue(5.0);
        verify(player, times(1)).setHealth(150.0);
        verify(profile, times(3)).setCurrentHealth(150L);
    }

    @Test
    void updateRuntimeConfigChangesAppliedValues() {
        PlayerDataService playerDataService = mock(PlayerDataService.class);
        LoggingUtil loggingUtil = mock(LoggingUtil.class);
        RuntimeStatConfig initialConfig = RuntimeStatConfig.defaults();
        TestHasteEffectApplier hasteApplier = new TestHasteEffectApplier(true);
        AttributeInstance maxHealthAttribute = mock(AttributeInstance.class);
        AttributeInstance attackSpeedAttribute = mock(AttributeInstance.class);
        TestAttributeResolver attributeResolver = new TestAttributeResolver(maxHealthAttribute, attackSpeedAttribute);
        PlayerRuntimeAttributeService service = new PlayerRuntimeAttributeService(playerDataService, initialConfig, loggingUtil, hasteApplier, attributeResolver);

        UUID playerId = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(true);
        when(player.getName()).thenReturn("Tester");
        when(player.getHealth()).thenReturn(140.0, 150.0, 150.0);
        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getMaxHealth()).thenReturn(200L);
        when(profile.getCurrentHealth()).thenReturn(150L);
        when(profile.getStatValue(Stat.SPEED)).thenReturn(150.0);
        when(profile.getStatValue(Stat.ATTACK_SPEED)).thenReturn(50.0);
        when(profile.getStatValue(Stat.MINING_SPEED)).thenReturn(160.0);
        when(playerDataService.getPlayerProfile(playerId)).thenReturn(profile);

        service.syncPlayer(player);

        RuntimeStatConfig updatedConfig = initialConfig.toBuilder()
                .movementSettings(RuntimeStatConfig.MovementSettings.builder()
                        .baseWalkSpeed(0.4)
                        .maxWalkSpeed(0.9)
                        .minWalkSpeed(0.05)
                        .speedBaseline(100.0))
                .combatSettings(RuntimeStatConfig.CombatSettings.builder()
                        .baseAttackSpeed(2.0)
                        .attackSpeedPerPoint(0.05)
                        .maxAttackSpeed(10.0)
                        .strengthPhysicalScaling(initialConfig.getCombatSettings().getStrengthPhysicalScaling())
                        .intelligenceMagicalScaling(initialConfig.getCombatSettings().getIntelligenceMagicalScaling())
                        .abilityPowerPercentPerPoint(initialConfig.getCombatSettings().getAbilityPowerPercentPerPoint())
                        .ferocityPerExtraHit(initialConfig.getCombatSettings().getFerocityPerExtraHit())
                        .ferocityMaxExtraHits(initialConfig.getCombatSettings().getFerocityMaxExtraHits())
                        .mobDefenseReductionFactor(initialConfig.getCombatSettings().getMobDefenseReductionFactor()))
                .gatheringSettings(RuntimeStatConfig.GatheringSettings.builder()
                        .baseGatherDelaySeconds(initialConfig.getGatheringSettings().getBaseGatherDelaySeconds())
                        .minimumGatherDelaySeconds(initialConfig.getGatheringSettings().getMinimumGatherDelaySeconds())
                        .miningSpeedDelayDivisor(initialConfig.getGatheringSettings().getMiningSpeedDelayDivisor())
                        .miningSpeedHastePerTier(40.0)
                        .miningSpeedMaxHasteTier(5))
                .build();

        service.updateRuntimeConfig(updatedConfig);
        service.syncPlayer(player);

        verify(player).setWalkSpeed(0.3f);
        verify(player).setWalkSpeed(0.6f);
        verify(attackSpeedAttribute).setBaseValue(5.0);
        verify(attackSpeedAttribute).setBaseValue(4.5);
        assertEquals(List.of(1, 3), hasteApplier.getRecordedAmplifiers());
    }

    private static final class TestHasteEffectApplier implements PlayerRuntimeAttributeService.HasteEffectApplier {
        private final boolean hasteAvailable;
        private final List<Integer> recordedAmplifiers = new java.util.ArrayList<>();

        private TestHasteEffectApplier(boolean hasteAvailable) {
            this.hasteAvailable = hasteAvailable;
        }

        @Override
        public void apply(Player player, int desiredAmplifier, int previousAmplifier) {
            if (!hasteAvailable || desiredAmplifier < 0) {
                return;
            }
            recordedAmplifiers.add(desiredAmplifier);
        }

        private List<Integer> getRecordedAmplifiers() {
            return recordedAmplifiers;
        }
    }

    private static final class TestAttributeResolver implements PlayerRuntimeAttributeService.AttributeResolver {
        private final AttributeInstance maxHealth;
        private final AttributeInstance attackSpeed;

        private TestAttributeResolver(AttributeInstance maxHealth, AttributeInstance attackSpeed) {
            this.maxHealth = maxHealth;
            this.attackSpeed = attackSpeed;
        }

        @Override
        public AttributeInstance getMaxHealth(Player player) {
            return maxHealth;
        }

        @Override
        public AttributeInstance getAttackSpeed(Player player) {
            return attackSpeed;
        }
    }
}
