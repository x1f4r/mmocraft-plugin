package com.x1f4r.mmocraft.util;

import com.x1f4r.mmocraft.playerdata.model.Stat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Map;

class JsonUtilTest {

    @Test
    void statsMapToJson_emptyMap_shouldReturnEmptyJsonObject() {
        Map<Stat, Double> statsMap = new EnumMap<>(Stat.class);
        assertEquals("{}", JsonUtil.statsMapToJson(statsMap));
    }

    @Test
    void statsMapToJson_nullMap_shouldReturnEmptyJsonObject() {
        assertEquals("{}", JsonUtil.statsMapToJson(null));
    }

    @Test
    void statsMapToJson_singleEntry_shouldSerializeCorrectly() {
        Map<Stat, Double> statsMap = new EnumMap<>(Stat.class);
        statsMap.put(Stat.STRENGTH, 15.5);
        assertEquals("{\"STRENGTH\":15.5}", JsonUtil.statsMapToJson(statsMap));
    }

    @Test
    void statsMapToJson_multipleEntries_shouldSerializeCorrectly() {
        Map<Stat, Double> statsMap = new EnumMap<>(Stat.class);
        statsMap.put(Stat.STRENGTH, 10.0);
        statsMap.put(Stat.AGILITY, 20.5);
        statsMap.put(Stat.VITALITY, 100.0);

        String json = JsonUtil.statsMapToJson(statsMap);
        // Order is not guaranteed in EnumMap's entrySet iteration if not explicitly sorted,
        // but for EnumMap, it's usually enum declaration order. For testing, check for presence.
        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertTrue(json.contains("\"STRENGTH\":10.0"));
        assertTrue(json.contains("\"AGILITY\":20.5"));
        assertTrue(json.contains("\"VITALITY\":100.0"));
        assertEquals(3, json.split(",").length); // 3 entries
    }

    @Test
    void jsonToStatsMap_emptyJsonObject_shouldReturnEmptyMap() {
        Map<Stat, Double> statsMap = JsonUtil.jsonToStatsMap("{}");
        assertTrue(statsMap.isEmpty());
    }

    @Test
    void jsonToStatsMap_nullOrEmptyJson_shouldReturnEmptyMap() {
        assertTrue(JsonUtil.jsonToStatsMap(null).isEmpty());
        assertTrue(JsonUtil.jsonToStatsMap("").isEmpty());
    }

    @Test
    void jsonToStatsMap_malformedJson_shouldReturnEmptyOrPartialMapAndLogError() {
        // The current simple parser might be very sensitive.
        // A proper library would handle this better.
        // Test cases for malformed JSON:
        assertTrue(JsonUtil.jsonToStatsMap("{").isEmpty(), "Should be empty for missing closing brace");
        assertTrue(JsonUtil.jsonToStatsMap("{\"STRENGTH\":10.0,").isEmpty(), "Should be empty for trailing comma (current basic parser might fail)");
        // This test assumes System.err.println is called on parse error.
        // In a real test, you might capture System.err or use a mock logger if JsonUtil used one.
    }


    @Test
    void jsonToStatsMap_singleEntry_shouldDeserializeCorrectly() {
        String json = "{\"INTELLIGENCE\":25.7}";
        Map<Stat, Double> statsMap = JsonUtil.jsonToStatsMap(json);
        assertEquals(1, statsMap.size());
        assertEquals(25.7, statsMap.get(Stat.INTELLIGENCE));
    }

    @Test
    void jsonToStatsMap_multipleEntries_shouldDeserializeCorrectly() {
        String json = "{\"DEFENSE\":30.2,\"WISDOM\":18.0,\"LUCK\":7.5}";
        Map<Stat, Double> statsMap = JsonUtil.jsonToStatsMap(json);
        assertEquals(3, statsMap.size());
        assertEquals(30.2, statsMap.get(Stat.DEFENSE));
        assertEquals(18.0, statsMap.get(Stat.WISDOM));
        assertEquals(7.5, statsMap.get(Stat.LUCK));
    }

    @Test
    void jsonToStatsMap_invalidStatName_shouldSkipEntry() {
        String json = "{\"STRENGTH\":10.0,\"INVALID_STAT\":5.0,\"AGILITY\":12.0}";
        Map<Stat, Double> statsMap = JsonUtil.jsonToStatsMap(json);
        assertEquals(2, statsMap.size());
        assertEquals(10.0, statsMap.get(Stat.STRENGTH));
        assertEquals(12.0, statsMap.get(Stat.AGILITY));
        assertFalse(statsMap.containsKey(Stat.valueOf("INVALID_STAT"))); // This would throw if Stat.valueOf was directly used without check
    }

    @Test
    void jsonToStatsMap_invalidNumberFormat_shouldSkipEntry() {
        String json = "{\"STRENGTH\":10.0,\"VITALITY\":\"not_a_number\",\"AGILITY\":12.0}";
        Map<Stat, Double> statsMap = JsonUtil.jsonToStatsMap(json);
        assertEquals(2, statsMap.size()); // STRENGTH and AGILITY should parse
        assertEquals(10.0, statsMap.get(Stat.STRENGTH));
        assertEquals(12.0, statsMap.get(Stat.AGILITY));
    }

    @Test
    void statsMapToJsonAndBack_shouldBeEquivalent() {
        Map<Stat, Double> originalMap = new EnumMap<>(Stat.class);
        originalMap.put(Stat.STRENGTH, 10.1);
        originalMap.put(Stat.AGILITY, 20.2);
        originalMap.put(Stat.INTELLIGENCE, 30.3);
        originalMap.put(Stat.DEFENSE, 40.4);
        originalMap.put(Stat.VITALITY, 50.5);
        originalMap.put(Stat.WISDOM, 60.6);
        originalMap.put(Stat.LUCK, 70.7);
        originalMap.put(Stat.PERCEPTION, 80.8);

        String json = JsonUtil.statsMapToJson(originalMap);
        Map<Stat, Double> deserializedMap = JsonUtil.jsonToStatsMap(json);

        assertEquals(originalMap.size(), deserializedMap.size());
        for (Stat stat : Stat.values()) { // Iterate through all possible stats
            if (originalMap.containsKey(stat)) {
                assertEquals(originalMap.get(stat), deserializedMap.get(stat), "Value for " + stat.name() + " should match.");
            } else {
                assertNull(deserializedMap.get(stat), "Stat " + stat.name() + " should not be present if not in original.");
            }
        }
    }
}
