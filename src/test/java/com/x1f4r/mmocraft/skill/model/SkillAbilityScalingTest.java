package com.x1f4r.mmocraft.skill.model;

import com.x1f4r.mmocraft.config.gameplay.GameplayConfigService;
import com.x1f4r.mmocraft.config.gameplay.RuntimeStatConfig;
import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkillAbilityScalingTest {

    @Test
    void abilityStatsModifyManaCostAndCooldown() {
        MMOCraftPlugin plugin = mock(MMOCraftPlugin.class);
        GameplayConfigService gameplayConfigService = mock(GameplayConfigService.class);
        when(plugin.getGameplayConfigService()).thenReturn(gameplayConfigService);

        RuntimeStatConfig runtimeConfig = RuntimeStatConfig.defaults().toBuilder()
                .abilitySettings(RuntimeStatConfig.AbilitySettings.builder()
                        .cooldownReductionPerAttackSpeedPoint(0.01)
                        .cooldownReductionPerIntelligencePoint(0.005)
                        .minimumCooldownSeconds(0.5)
                        .manaCostReductionPerIntelligencePoint(0.002)
                        .manaCostReductionPerAbilityPowerPoint(0.001)
                        .minimumManaCostMultiplier(0.3)
                        .minimumManaCost(2.0))
                .build();
        when(gameplayConfigService.getRuntimeStatConfig()).thenReturn(runtimeConfig);

        TestSkill skill = new TestSkill(plugin, 100.0, 10.0);

        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getStatValue(Stat.INTELLIGENCE)).thenReturn(100.0);
        when(profile.getStatValue(Stat.ABILITY_POWER)).thenReturn(50.0);
        when(profile.getStatValue(Stat.ATTACK_SPEED)).thenReturn(20.0);
        when(profile.getCurrentMana()).thenReturn(70L, 80L);
        when(profile.isSkillOnCooldown("test_skill")).thenReturn(false);

        double effectiveManaCost = skill.getEffectiveManaCost(profile);
        assertEquals(75.0, effectiveManaCost, 1e-6);
        assertFalse(skill.canUse(profile));
        assertTrue(skill.canUse(profile));

        long spent = skill.applyManaCost(profile);
        assertEquals(75L, spent);
        verify(profile).consumeMana(75L);

        double effectiveCooldown = skill.getEffectiveCooldownSeconds(profile);
        assertEquals(3.0, effectiveCooldown, 1e-6);
        skill.onCooldown(profile);
        ArgumentCaptor<Double> cooldownCaptor = ArgumentCaptor.forClass(Double.class);
        verify(profile).setSkillCooldown(eq("test_skill"), cooldownCaptor.capture());
        assertEquals(3.0, cooldownCaptor.getValue(), 1e-6);
    }

    private static final class TestSkill extends Skill {
        private TestSkill(MMOCraftPlugin plugin, double manaCost, double cooldownSeconds) {
            super(plugin, "test_skill", "Test Skill", "A skill for testing.", manaCost, cooldownSeconds, 0.0, SkillType.ACTIVE_SELF);
        }

        @Override
        public void execute(PlayerProfile casterProfile, Entity targetEntity, Location targetLocation) {
            // No-op for testing.
        }
    }
}
