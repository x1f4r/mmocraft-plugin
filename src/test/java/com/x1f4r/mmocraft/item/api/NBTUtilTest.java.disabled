package com.x1f4r.mmocraft.item.api;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NBTUtilTest {

    @Mock private MMOCraftPlugin mockPlugin;
    @Mock private ItemStack mockItemStack;
    @Mock private ItemMeta mockItemMeta;
    @Mock private PersistentDataContainer mockPdc;

    private final String testKeyString = "test_string_key";
    private final String testValueString = "TestValue";
    private final String testKeyInt = "test_int_key";
    private final int testValueInt = 12345;
    private final String testKeyDouble = "test_double_key";
    private final double testValueDouble = 123.456;
    private final String testKeyBoolean = "test_boolean_key";

    private NamespacedKey namespacedKeyString;
    private NamespacedKey namespacedKeyInt;
    private NamespacedKey namespacedKeyDouble;
    private NamespacedKey namespacedKeyBoolean;


    @BeforeEach
    void setUp() {
        // Common mocking setup
        when(mockItemStack.getItemMeta()).thenReturn(mockItemMeta);
        when(mockItemMeta.getPersistentDataContainer()).thenReturn(mockPdc);

        // Mock plugin name for NamespacedKey
        lenient().when(mockPlugin.getName()).thenReturn("MMOCraftTestPlugin"); // Or your actual plugin name if fixed

        namespacedKeyString = new NamespacedKey(mockPlugin, testKeyString);
        namespacedKeyInt = new NamespacedKey(mockPlugin, testKeyInt);
        namespacedKeyDouble = new NamespacedKey(mockPlugin, testKeyDouble);
        namespacedKeyBoolean = new NamespacedKey(mockPlugin, testKeyBoolean);

    }

    // --- String NBT Tests ---
    @Test
    void setString_validItem_setsTag() {
        NBTUtil.setString(mockItemStack, testKeyString, testValueString, mockPlugin);
        verify(mockPdc).set(eq(namespacedKeyString), eq(PersistentDataType.STRING), eq(testValueString));
        verify(mockItemStack).setItemMeta(mockItemMeta);
    }

    @Test
    void getString_tagExists_returnsValue() {
        when(mockPdc.get(eq(namespacedKeyString), eq(PersistentDataType.STRING))).thenReturn(testValueString);
        String value = NBTUtil.getString(mockItemStack, testKeyString, mockPlugin);
        assertEquals(testValueString, value);
    }

    @Test
    void getString_tagDoesNotExist_returnsNull() {
        when(mockPdc.get(eq(namespacedKeyString), eq(PersistentDataType.STRING))).thenReturn(null);
        String value = NBTUtil.getString(mockItemStack, testKeyString, mockPlugin);
        assertNull(value);
    }

    // --- Integer NBT Tests ---
    @Test
    void setInt_validItem_setsTag() {
        NBTUtil.setInt(mockItemStack, testKeyInt, testValueInt, mockPlugin);
        verify(mockPdc).set(eq(namespacedKeyInt), eq(PersistentDataType.INTEGER), eq(testValueInt));
        verify(mockItemStack).setItemMeta(mockItemMeta);
    }

    @Test
    void getInt_tagExists_returnsValue() {
        when(mockPdc.get(eq(namespacedKeyInt), eq(PersistentDataType.INTEGER))).thenReturn(testValueInt);
        int value = NBTUtil.getInt(mockItemStack, testKeyInt, 0, mockPlugin);
        assertEquals(testValueInt, value);
    }

    @Test
    void getInt_tagDoesNotExist_returnsDefault() {
        when(mockPdc.get(eq(namespacedKeyInt), eq(PersistentDataType.INTEGER))).thenReturn(null);
        int value = NBTUtil.getInt(mockItemStack, testKeyInt, 99, mockPlugin);
        assertEquals(99, value);
    }

    // --- Double NBT Tests ---
    @Test
    void setDouble_validItem_setsTag() {
        NBTUtil.setDouble(mockItemStack, testKeyDouble, testValueDouble, mockPlugin);
        verify(mockPdc).set(eq(namespacedKeyDouble), eq(PersistentDataType.DOUBLE), eq(testValueDouble));
        verify(mockItemStack).setItemMeta(mockItemMeta);
    }

    @Test
    void getDouble_tagExists_returnsValue() {
        when(mockPdc.get(eq(namespacedKeyDouble), eq(PersistentDataType.DOUBLE))).thenReturn(testValueDouble);
        double value = NBTUtil.getDouble(mockItemStack, testKeyDouble, 0.0, mockPlugin);
        assertEquals(testValueDouble, value, 0.001);
    }

    // --- Boolean NBT Tests ---
     @Test
    void setBoolean_true_setsByteOne() {
        NBTUtil.setBoolean(mockItemStack, testKeyBoolean, true, mockPlugin);
        verify(mockPdc).set(eq(namespacedKeyBoolean), eq(PersistentDataType.BYTE), eq((byte)1));
    }

    @Test
    void setBoolean_false_setsByteZero() {
        NBTUtil.setBoolean(mockItemStack, testKeyBoolean, false, mockPlugin);
        verify(mockPdc).set(eq(namespacedKeyBoolean), eq(PersistentDataType.BYTE), eq((byte)0));
    }

    @Test
    void getBoolean_byteOne_returnsTrue() {
        when(mockPdc.get(eq(namespacedKeyBoolean), eq(PersistentDataType.BYTE))).thenReturn((byte)1);
        assertTrue(NBTUtil.getBoolean(mockItemStack, testKeyBoolean, mockPlugin));
    }

    @Test
    void getBoolean_byteZero_returnsFalse() {
        when(mockPdc.get(eq(namespacedKeyBoolean), eq(PersistentDataType.BYTE))).thenReturn((byte)0);
        assertFalse(NBTUtil.getBoolean(mockItemStack, testKeyBoolean, mockPlugin));
    }

    @Test
    void getBoolean_tagMissing_returnsFalse() {
         when(mockPdc.get(eq(namespacedKeyBoolean), eq(PersistentDataType.BYTE))).thenReturn(null);
        assertFalse(NBTUtil.getBoolean(mockItemStack, testKeyBoolean, mockPlugin));
    }


    // --- hasTag and removeTag Tests ---
    @Test
    void hasTag_tagExists_returnsTrue() {
        // Test with one type, assuming others work similarly if PersistentDataContainer.has works
        when(mockPdc.has(eq(namespacedKeyString), eq(PersistentDataType.STRING))).thenReturn(true);
        assertTrue(NBTUtil.hasTag(mockItemStack, testKeyString, mockPlugin));
    }

    @Test
    void hasTag_tagDoesNotExist_returnsFalse() {
        when(mockPdc.has(any(NamespacedKey.class), any(PersistentDataType.class))).thenReturn(false);
        assertFalse(NBTUtil.hasTag(mockItemStack, "non_existent_key", mockPlugin));
    }

    @Test
    void removeTag_tagExists_removesTag() {
        NBTUtil.removeTag(mockItemStack, testKeyString, mockPlugin);
        verify(mockPdc).remove(eq(namespacedKeyString));
        verify(mockItemStack).setItemMeta(mockItemMeta);
    }

    // --- Edge Case Tests for all setters/getters ---
    @Test
    void setString_nullItem_returnsNull() {
        assertNull(NBTUtil.setString(null, testKeyString, testValueString, mockPlugin));
    }

    @Test
    void setString_airItem_returnsSameItem() {
        ItemStack airItem = new ItemStack(Material.AIR);
        // NBTUtil should ideally not modify AIR item, or Bukkit might throw error when getting meta
        // For safety, our NBTUtil returns the item as is if it's AIR or meta is null.
        ItemStack result = NBTUtil.setString(airItem, testKeyString, testValueString, mockPlugin);
        assertSame(airItem, result);
    }

    @Test
    void getString_nullItem_returnsNull() {
        assertNull(NBTUtil.getString(null, testKeyString, mockPlugin));
    }

    @Test
    void getString_nullMeta_returnsNull() {
        when(mockItemStack.getItemMeta()).thenReturn(null);
        assertNull(NBTUtil.getString(mockItemStack, testKeyString, mockPlugin));
    }
}
