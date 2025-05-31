package com.x1f4r.mmocraft.skill.service;

import com.x1f4r.mmocraft.skill.model.Skill;
import java.util.Collection;
import java.util.Optional;

/**
 * Service responsible for managing and providing access to all available skills in the game.
 * Skills are typically registered at startup.
 */
public interface SkillRegistryService {

    /**
     * Registers a skill with the service.
     * If a skill with the same ID is already registered, it might be overwritten or log a warning,
     * depending on the implementation.
     *
     * @param skill The {@link Skill} instance to register.
     */
    void registerSkill(Skill skill);

    /**
     * Retrieves a skill by its unique ID.
     *
     * @param skillId The unique identifier of the skill.
     * @return An {@link Optional} containing the {@link Skill} if found, or an empty Optional otherwise.
     */
    Optional<Skill> getSkill(String skillId);

    /**
     * Retrieves a collection of all registered skills.
     *
     * @return A collection of all {@link Skill} instances. This collection might be unmodifiable.
     */
    Collection<Skill> getAllSkills();

    /**
     * Unregisters a skill by its unique ID.
     * Primarily useful for dynamic skill management or testing.
     *
     * @param skillId The unique identifier of the skill to unregister.
     * @return True if a skill was removed, false otherwise.
     */
    boolean unregisterSkill(String skillId);
}
