package com.x1f4r.mmocraft.world.resourcegathering.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ActiveResourceNodeTest {

    @Mock
    private World mockWorld;
    private Location location1, location2, location1Clone;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockWorld.getName()).thenReturn("test_world");

        location1 = new Location(mockWorld, 10.0, 60.0, 20.0);
        location1Clone = new Location(mockWorld, 10.0, 60.0, 20.0); // Bukkit Location equals works on content
        location2 = new Location(mockWorld, 12.0, 60.0, 20.0);
    }

    @Test
    void constructor_initialStateCorrect() {
        ActiveResourceNode node = new ActiveResourceNode(location1, "test_type");

        assertEquals(location1, node.getLocation());
        assertEquals("test_type", node.getNodeTypeId());
        assertFalse(node.isDepleted());
        assertEquals(0, node.getRespawnAtMillis());
        // Check that internal location is a clone
        assertNotSame(location1, node.getInternalLocation(), "Internal location should be a clone.");
        assertEquals(location1, node.getInternalLocation(), "Internal location should be equal to original.");

    }

    @Test
    void setDepleted_and_setRespawnAtMillis_shouldUpdateState() {
        ActiveResourceNode node = new ActiveResourceNode(location1, "test_type");
        node.setDepleted(true);
        node.setRespawnAtMillis(12345L);

        assertTrue(node.isDepleted());
        assertEquals(12345L, node.getRespawnAtMillis());
    }

    @Test
    void getLocation_shouldReturnClone() {
        ActiveResourceNode node = new ActiveResourceNode(location1, "test_type");
        Location retrievedLoc1 = node.getLocation();
        Location retrievedLoc2 = node.getLocation();

        assertNotSame(retrievedLoc1, retrievedLoc2, "getLocation should return a new clone each time.");
        assertEquals(retrievedLoc1, retrievedLoc2);
        assertEquals(location1, retrievedLoc1);
    }

    @Test
    void getInternalLocation_shouldReturnSameInstance() {
        ActiveResourceNode node = new ActiveResourceNode(location1, "test_type");
        Location internalLoc1 = node.getInternalLocation();
        Location internalLoc2 = node.getInternalLocation();
        assertSame(internalLoc1, internalLoc2, "getInternalLocation should return the same instance.");
    }


    @Test
    void equalsAndHashCode_basedOnLocation() {
        ActiveResourceNode node1a = new ActiveResourceNode(location1, "typeA");
        ActiveResourceNode node1b = new ActiveResourceNode(location1Clone, "typeB"); // Same location, different typeId
        ActiveResourceNode node2 = new ActiveResourceNode(location2, "typeA");

        assertEquals(node1a, node1b, "Nodes with same location should be equal, regardless of typeId.");
        assertEquals(node1a.hashCode(), node1b.hashCode(), "HashCodes for nodes with same location should be equal.");

        assertNotEquals(node1a, node2, "Nodes with different locations should not be equal.");
        assertNotEquals(node1a.hashCode(), node2.hashCode(), "HashCodes for nodes with different locations should ideally not be equal.");
    }

    @Test
    void constructor_nullLocation_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ActiveResourceNode(null, "test_type"));
    }

    @Test
    void constructor_nullNodeTypeId_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ActiveResourceNode(location1, null));
    }

    @Test
    void toString_containsRelevantInfo() {
        ActiveResourceNode node = new ActiveResourceNode(location1, "stone_node");
        node.setDepleted(true);
        node.setRespawnAtMillis(System.currentTimeMillis() + 10000);
        String str = node.toString();

        assertTrue(str.contains("test_world(10,60,20)"));
        assertTrue(str.contains("nodeTypeId='stone_node'"));
        assertTrue(str.contains("isDepleted=true"));
        assertTrue(str.contains("respawnAtMillis="));
    }
}
