package com.x1f4r.mmocraft.playerdata.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class PlayerProfileSkillCooldownTest {

    private PlayerProfile profile;
    private final String testSkillId = "test_fireball";

    @BeforeEach
    void setUp() {
        profile = new PlayerProfile(UUID.randomUUID(), "CooldownTester");
    }

    @Test
    void setSkillCooldown_positiveDuration_setsCooldown() {
        assertFalse(profile.isSkillOnCooldown(testSkillId), "Skill should not be on cooldown initially.");

        profile.setSkillCooldown(testSkillId, 10.0); // 10 seconds

        assertTrue(profile.isSkillOnCooldown(testSkillId), "Skill should now be on cooldown.");
        long remaining = profile.getSkillRemainingCooldown(testSkillId);
        assertTrue(remaining > 9000 && remaining <= 10000, "Remaining cooldown should be close to 10 seconds.");
    }

    @Test
    void setSkillCooldown_zeroOrNegativeDuration_removesCooldown() {
        // First, set a cooldown
        profile.setSkillCooldown(testSkillId, 10.0);
        assertTrue(profile.isSkillOnCooldown(testSkillId), "Skill should be on cooldown before removal attempt.");

        // Remove with zero
        profile.setSkillCooldown(testSkillId, 0.0);
        assertFalse(profile.isSkillOnCooldown(testSkillId), "Skill should not be on cooldown after setting duration to 0.");
        assertEquals(0, profile.getSkillRemainingCooldown(testSkillId));

        // Set it again and remove with negative
        profile.setSkillCooldown(testSkillId, 10.0);
        assertTrue(profile.isSkillOnCooldown(testSkillId));
        profile.setSkillCooldown(testSkillId, -5.0);
        assertFalse(profile.isSkillOnCooldown(testSkillId), "Skill should not be on cooldown after setting negative duration.");
        assertEquals(0, profile.getSkillRemainingCooldown(testSkillId));
    }

    @Test
    void isSkillOnCooldown_nonExistentSkill_returnsFalse() {
        assertFalse(profile.isSkillOnCooldown("non_existent_skill"));
    }

    @Test
    void getSkillRemainingCooldown_nonExistentSkill_returnsZero() {
        assertEquals(0, profile.getSkillRemainingCooldown("non_existent_skill"));
    }

    @Test
    void getSkillRemainingCooldown_afterCooldownExpires_returnsZero() throws InterruptedException {
        profile.setSkillCooldown(testSkillId, 0.1); // 100 milliseconds
        assertTrue(profile.isSkillOnCooldown(testSkillId));

        // Wait for cooldown to expire
        TimeUnit.MILLISECONDS.sleep(150); // Wait a bit longer than 100ms

        assertFalse(profile.isSkillOnCooldown(testSkillId), "Skill should no longer be on cooldown after expiry.");
        assertEquals(0, profile.getSkillRemainingCooldown(testSkillId), "Remaining cooldown should be 0 after expiry.");
    }

    @Test
    void getSkillRemainingCooldown_activeCooldown_returnsPositiveValue() {
        profile.setSkillCooldown(testSkillId, 5.0); // 5 seconds
        long remaining = profile.getSkillRemainingCooldown(testSkillId);
        assertTrue(remaining > 0 && remaining <= 5000);
    }
}
