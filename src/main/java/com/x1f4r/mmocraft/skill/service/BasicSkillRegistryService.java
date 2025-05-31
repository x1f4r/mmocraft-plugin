package com.x1f4r.mmocraft.skill.service;

import com.x1f4r.mmocraft.skill.model.Skill;
import com.x1f4r.mmocraft.util.LoggingUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BasicSkillRegistryService implements SkillRegistryService {

    private final LoggingUtil logger;
    private final Map<String, Skill> registeredSkills = new ConcurrentHashMap<>();

    public BasicSkillRegistryService(LoggingUtil logger) {
        this.logger = logger;
        logger.debug("BasicSkillRegistryService initialized.");
    }

    @Override
    public void registerSkill(Skill skill) {
        if (skill == null || skill.getSkillId() == null || skill.getSkillId().trim().isEmpty()) {
            logger.warning("Attempted to register a null skill or a skill with an invalid ID.");
            return;
        }
        Skill existingSkill = registeredSkills.put(skill.getSkillId(), skill);
        if (existingSkill != null) {
            logger.warning("Skill ID '" + skill.getSkillId() + "' was already registered. Overwriting '" +
                           existingSkill.getSkillName() + "' with '" + skill.getSkillName() + "'.");
        } else {
            logger.info("Registered skill: " + skill.getSkillName() + " (ID: " + skill.getSkillId() + ")");
        }
    }

    @Override
    public Optional<Skill> getSkill(String skillId) {
        if (skillId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registeredSkills.get(skillId));
    }

    @Override
    public Collection<Skill> getAllSkills() {
        return Collections.unmodifiableCollection(registeredSkills.values());
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        if (skillId == null) {
            return false;
        }
        Skill removedSkill = registeredSkills.remove(skillId);
        if (removedSkill != null) {
            logger.info("Unregistered skill: " + removedSkill.getSkillName() + " (ID: " + skillId + ")");
            return true;
        }
        return false;
    }
}
