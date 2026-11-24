package com.frost.envoys.skin.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimSyncServiceTest {

    @Test
    void validatesAnimationNames() {
        assertTrue(AnimSyncService.isValidAnimName("walk"));
        assertTrue(AnimSyncService.isValidAnimName("a".repeat(128)));
        assertFalse(AnimSyncService.isValidAnimName("a".repeat(129)));
        assertFalse(AnimSyncService.isValidAnimName(null));
        assertFalse(AnimSyncService.isValidAnimName(""));
        assertFalse(AnimSyncService.isValidAnimName("   "));
        assertFalse(AnimSyncService.isValidAnimName(".."));
        assertFalse(AnimSyncService.isValidAnimName("a/b"));
        assertFalse(AnimSyncService.isValidAnimName("a\\b"));
        assertFalse(AnimSyncService.isValidAnimName("a\u0001b"));
    }

    @Test
    void extractsPrimaryName() {
        assertEquals("Walk", AnimSyncService.extractPrimaryNameFromJson(
                bytes("{\"name\":\"Walk\"}"), "file.json"));
        assertEquals("Walk", AnimSyncService.extractPrimaryNameFromJson(
                bytes("{\"name\":{\"text\":\"Walk\"}}"), "file.json"));
        assertEquals("Fallback", AnimSyncService.extractPrimaryNameFromJson(
                bytes("{\"name\":{\"fallback\":\"Fallback\"}}"), "file.json"));
        assertEquals("Pal", AnimSyncService.extractPrimaryNameFromJson(
                bytes("{\"animations\":{\"key\":{\"player_animation_library\":{\"name\":\"Pal\"}}}}"),
                "file.json"));
        assertEquals("file", AnimSyncService.extractPrimaryNameFromJson(bytes("{}"), "file.json"));
        assertEquals("file", AnimSyncService.extractPrimaryNameFromJson(bytes("{"), "file.json"));
    }

    @Test
    void extractsAllAliasesAndDeduplicates() {
        List<String> names = AnimSyncService.extractAllNamesFromJson(
                bytes("{\"name\":\"Walk\",\"animations\":{\"run\":{},\"jump\":{\"player_animation_library\":{\"name\":\"Pal\"}}}}"),
                "file.json");

        assertTrue(names.containsAll(List.of("Walk", "run", "jump", "Pal", "file")));
        assertEquals(names.size(), new LinkedHashSet<>(names).size());
    }

    @Test
    void extractsFallbackAndTranslate() {
        List<String> names = AnimSyncService.extractAllNamesFromJson(
                bytes("{\"name\":{\"fallback\":\"Fallback\",\"translate\":\"Translate\"}}"),
                "file.json");
        assertTrue(names.contains("Fallback"));
        assertTrue(names.contains("Translate"));
    }

    @Test
    void brokenJsonFallsBackToFileName() {
        assertEquals(List.of("file"), AnimSyncService.extractAllNamesFromJson(bytes("{"), "file.json"));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
