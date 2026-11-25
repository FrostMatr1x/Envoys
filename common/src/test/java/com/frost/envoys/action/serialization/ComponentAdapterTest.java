package com.frost.envoys.action.serialization;

import com.frost.envoys.TestSupport;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.platform.TestAttachments;
import com.frost.envoys.util.ColorUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentAdapterTest {

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
                .registerTypeHierarchyAdapter(Component.class, new ComponentAdapter().nullSafe())
                .create();
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

    @Test
    void nullComponentSerializesToJsonNullAndBack() {
        assertEquals("null", gson.toJson(null, Component.class));
        assertNull(gson.fromJson("null", Component.class));
    }

    @Test
    void literalComponentRoundTrips() {
        Component component = Component.literal("Hello");

        Component restored = gson.fromJson(gson.toJson(component), Component.class);

        assertNotNull(restored);
        assertEquals("Hello", restored.getString());
    }

    @Test
    void legacyColorStringIsParsed() {
        Component restored = gson.fromJson("\"&aGreen\"", Component.class);

        assertNotNull(restored);
        assertEquals("Green", restored.getString());
        assertTrue(ColorUtils.toFormattedString(restored).contains("&#55FF55"));
    }

    @Test
    void legacyHexStringIsParsed() {
        Component restored = gson.fromJson("\"&#FF0000Red\"", Component.class);

        assertNotNull(restored);
        assertEquals("Red", restored.getString());
        assertTrue(ColorUtils.toFormattedString(restored).contains("&#FF0000"));
    }

    @Test
    void translatableComponentRoundTrips() {
        Component component = Component.translatable("envoys.test.key");

        JsonElement tree = gson.toJsonTree(component);
        Component restored = gson.fromJson(tree, Component.class);

        assertNotNull(restored);
        assertEquals(component.getString(), restored.getString());
        assertEquals(tree, gson.toJsonTree(restored));
    }

    @Test
    void invalidComponentJsonReturnsNull() {
        assertNull(gson.fromJson("{\"bogus\":true}", Component.class));
        assertNull(gson.fromJson("[]", Component.class));
    }

    @Test
    void malformedJsonThrows() {
        assertThrows(JsonParseException.class, () -> gson.fromJson("{not json", Component.class));
    }
}
