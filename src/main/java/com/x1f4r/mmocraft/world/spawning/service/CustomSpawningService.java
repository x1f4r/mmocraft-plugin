package com.x1f4r.mmocraft.world.spawning.service;

import com.x1f4r.mmocraft.world.spawning.model.CustomSpawnRule;
import java.util.List;

/**
 * Service responsible for managing custom mob spawning rules and triggering spawn attempts.
 */
public interface CustomSpawningService {

    /**
     * Registers a new custom spawn rule.
     * @param rule The {@link CustomSpawnRule} to add to the system.
     */
    void registerRule(CustomSpawnRule rule);

    /**
     * Unregisters a custom spawn rule by its ID.
     * @param ruleId The unique ID of the rule to remove.
     * @return True if a rule was removed, false otherwise.
     */
    boolean unregisterRule(String ruleId);

    /**
     * Retrieves all currently registered spawn rules.
     * @return An unmodifiable list of all {@link CustomSpawnRule}s.
     */
    List<CustomSpawnRule> getAllRules();

    /**
     * The main method called periodically by a scheduler to attempt custom mob spawns.
     * This method will iterate through potential spawn locations (e.g., in loaded chunks)
     * and evaluate registered {@link CustomSpawnRule}s to determine if mobs should spawn.
     */
    void attemptSpawns();

    /**
     * Called when the plugin is shutting down to clean up any resources or tasks.
     */
    void shutdown();
}
