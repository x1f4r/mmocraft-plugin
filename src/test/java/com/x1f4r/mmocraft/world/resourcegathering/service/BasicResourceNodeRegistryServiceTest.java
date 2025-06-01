package com.x1f4r.mmocraft.world.resourcegathering.service;

import com.x1f4r.mmocraft.util.LoggingUtil;
import com.x1f4r.mmocraft.world.resourcegathering.model.ResourceNodeType;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BasicResourceNodeRegistryServiceTest {

    @Mock
    private LoggingUtil mockLogger;

    private BasicResourceNodeRegistryService registryService;
    private ResourceNodeType nodeType1;
    private ResourceNodeType nodeType2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registryService = new BasicResourceNodeRegistryService(mockLogger);

        nodeType1 = new ResourceNodeType("type1", Material.STONE, 5.0, Set.of(), "loot1", 60);
        nodeType2 = new ResourceNodeType("type2", Material.IRON_ORE, 10.0, Set.of(Material.STONE_PICKAXE), "loot2", 120);
    }

    @Test
    void registerNodeType_newType_shouldStoreIt() {
        registryService.registerNodeType(nodeType1);
        Optional<ResourceNodeType> retrieved = registryService.getNodeType("type1");
        assertTrue(retrieved.isPresent());
        assertEquals(nodeType1, retrieved.get());
        verify(mockLogger, times(1)).debug("Registered ResourceNodeType: type1");
    }

    @Test
    void registerNodeType_duplicateId_shouldOverwriteAndLogWarning() {
        registryService.registerNodeType(nodeType1);
        ResourceNodeType newNodeType1SameId = new ResourceNodeType("type1", Material.COAL_ORE, 3.0, Set.of(), "loot_new", 30);
        registryService.registerNodeType(newNodeType1SameId);

        Optional<ResourceNodeType> retrieved = registryService.getNodeType("type1");
        assertTrue(retrieved.isPresent());
        assertEquals(newNodeType1SameId, retrieved.get(), "The new node type should overwrite the old one.");
        assertNotEquals(nodeType1.getDisplayMaterial(), retrieved.get().getDisplayMaterial()); // Verify it's the new one

        verify(mockLogger, times(1)).warning("ResourceNodeType with ID 'type1' is being overwritten.");
        verify(mockLogger, times(2)).debug(startsWith("Registered ResourceNodeType:")); // Called for both registrations
    }

    @Test
    void registerNodeType_nullType_shouldLogWarningAndNotStore() {
        registryService.registerNodeType(null);
        assertTrue(registryService.getAllNodeTypes().isEmpty());
        verify(mockLogger, times(1)).warning("Attempted to register a null ResourceNodeType.");
        verify(mockLogger, never()).debug(anyString());
    }

    @Test
    void getNodeType_existingId_shouldReturnNode() {
        registryService.registerNodeType(nodeType1);
        registryService.registerNodeType(nodeType2);

        Optional<ResourceNodeType> retrieved = registryService.getNodeType("type2");
        assertTrue(retrieved.isPresent());
        assertEquals(nodeType2, retrieved.get());
    }

    @Test
    void getNodeType_nonExistingId_shouldReturnEmpty() {
        registryService.registerNodeType(nodeType1);
        Optional<ResourceNodeType> retrieved = registryService.getNodeType("non_existent_id");
        assertFalse(retrieved.isPresent());
    }

    @Test
    void getNodeType_nullId_shouldReturnEmpty() {
        Optional<ResourceNodeType> retrieved = registryService.getNodeType(null);
        assertFalse(retrieved.isPresent());
    }

    @Test
    void getAllNodeTypes_shouldReturnAllRegisteredTypes() {
        registryService.registerNodeType(nodeType1);
        registryService.registerNodeType(nodeType2);

        Collection<ResourceNodeType> allTypes = registryService.getAllNodeTypes();
        assertEquals(2, allTypes.size());
        assertTrue(allTypes.contains(nodeType1));
        assertTrue(allTypes.contains(nodeType2));
    }

    @Test
    void getAllNodeTypes_emptyRegistry_shouldReturnEmptyCollection() {
        Collection<ResourceNodeType> allTypes = registryService.getAllNodeTypes();
        assertTrue(allTypes.isEmpty());
    }

    @Test
    void getAllNodeTypes_returnedCollectionShouldBeUnmodifiable() {
        registryService.registerNodeType(nodeType1);
        Collection<ResourceNodeType> allTypes = registryService.getAllNodeTypes();
        assertThrows(UnsupportedOperationException.class, () -> allTypes.add(nodeType2));
    }
}
