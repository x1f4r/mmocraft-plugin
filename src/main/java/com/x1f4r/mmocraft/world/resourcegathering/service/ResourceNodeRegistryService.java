package com.x1f4r.mmocraft.world.resourcegathering.service;

import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;

import java.util.Collection;
import java.util.Optional;

public interface ResourceNodeRegistryService {

    /**
     * Registers a new type of resource node.
     * If a node type with the same ID already exists, it might be overwritten or an error logged,
     * depending on the implementation.
     *
     * @param nodeType The {@link ResourceNodeType} to register.
     */
    void registerNodeType(ResourceNodeType nodeType);

    /**
     * Retrieves a resource node type by its unique ID.
     *
     * @param typeId The ID of the node type.
     * @return An {@link Optional} containing the {@link ResourceNodeType} if found, or empty otherwise.
     */
    Optional<ResourceNodeType> getNodeType(String typeId);

    /**
     * Retrieves all registered resource node types.
     *
     * @return A collection of all {@link ResourceNodeType}s. The collection might be unmodifiable.
     */
    Collection<ResourceNodeType> getAllNodeTypes();
}
