package com.x1f4r.mmocraft.world.resourcegathering.service;

import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BasicResourceNodeRegistryService implements ResourceNodeRegistryService {

    private final LoggingUtil logger;
    private final Map<String, ResourceNodeType> nodeTypes = new ConcurrentHashMap<>();

    public BasicResourceNodeRegistryService(LoggingUtil logger) {
        this.logger = logger;
    }

    @Override
    public void registerNodeType(ResourceNodeType nodeType) {
        if (nodeType == null) {
            logger.warning("Attempted to register a null ResourceNodeType.");
            return;
        }
        if (nodeTypes.containsKey(nodeType.getTypeId())) {
            logger.warning("ResourceNodeType with ID '" + nodeType.getTypeId() + "' is being overwritten.");
        }
        nodeTypes.put(nodeType.getTypeId(), nodeType);
        logger.debug("Registered ResourceNodeType: " + nodeType.getTypeId());
    }

    @Override
    public Optional<ResourceNodeType> getNodeType(String typeId) {
        if (typeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodeTypes.get(typeId));
    }

    @Override
    public Collection<ResourceNodeType> getAllNodeTypes() {
        return Collections.unmodifiableCollection(nodeTypes.values());
    }
}
