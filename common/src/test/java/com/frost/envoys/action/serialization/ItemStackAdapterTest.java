package com.frost.envoys.action.serialization;

import com.frost.envoys.TestSupport;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.platform.TestAttachments;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStackAdapterTest {

    @TempDir
    Path tempDir;

    private Gson gson;

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir, new TestAttachments());
        gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackAdapter().nullSafe())
                .create();
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

    @Test
    void emptyStackSerializesToJsonNull() {
        assertEquals("null", gson.toJson(ItemStack.EMPTY));
    }

    @Test
    void jsonNullReadsBackAsNull() {
        assertNull(gson.fromJson("null", ItemStack.class));
    }

    @Test
    void emptyJsonInputReadsAsNull() {
        assertNull(gson.fromJson("", ItemStack.class));
        assertNull(gson.fromJson("   ", ItemStack.class));
    }

    @Test
    void roundTripsPlainStack() {
        ItemStack stack = new ItemStack(Items.STONE, 7);

        ItemStack restored = gson.fromJson(gson.toJson(stack), ItemStack.class);

        assertEquals(Items.STONE, restored.getItem());
        assertEquals(7, restored.getCount());
    }

    @Test
    void roundTripsCustomDataComponents() {
        ItemStack stack = new ItemStack(Items.STONE, 3);
        stack.set(DataComponents.DAMAGE, 5);

        ItemStack restored = gson.fromJson(gson.toJson(stack), ItemStack.class);

        assertEquals(Items.STONE, restored.getItem());
        assertEquals(3, restored.getCount());
        assertEquals(5, restored.get(DataComponents.DAMAGE));
    }

    @Test
    void unknownItemIdThrows() {
        String json = "{\"id\":\"minecraft:definitely_missing\",\"count\":1}";

        assertThrows(JsonParseException.class, () -> gson.fromJson(json, ItemStack.class));
    }

    @Test
    void nonObjectJsonThrows() {
        assertThrows(JsonParseException.class, () -> gson.fromJson("[]", ItemStack.class));
        assertThrows(JsonParseException.class, () -> gson.fromJson("{}", ItemStack.class));
    }

    @Test
    void malformedJsonThrows() {
        assertThrows(JsonParseException.class, () -> gson.fromJson("{not json", ItemStack.class));
    }

    @Test
    void serializedJsonContainsItemIdAndCount() {
        String json = gson.toJson(new ItemStack(Items.EMERALD, 4));

        assertTrue(json.contains("minecraft:emerald"));
        assertTrue(json.contains("\"count\":4"));
    }
}
