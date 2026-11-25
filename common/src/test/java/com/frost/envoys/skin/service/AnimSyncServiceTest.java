package com.frost.envoys.skin.service;

import com.frost.envoys.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimSyncServiceTest {

    private static final int MAX_FILE_SIZE = 2 * 1024 * 1024;

    @TempDir
    Path tempDir;

    private Path animDir;

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
        animDir = tempDir.resolve("world/envoys/anim");
        TestSupport.await(AnimSyncService.refreshCache());
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

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

    @Test
    void listIsEmptyForEmptyFolder() {
        assertTrue(TestSupport.await(AnimSyncService.listAnimsAsync()).isEmpty());
    }

    @Test
    void listScansOnlyJsonAndComputesHash() throws Exception {
        write("walk.json", "{\"name\":\"Walk\"}");
        write("notes.txt", "text");

        List<AnimSyncService.AnimListEntry> entries = TestSupport.await(AnimSyncService.listAnimsAsync());

        assertEquals(1, entries.size());
        assertEquals("Walk", entries.get(0).name());
        assertEquals(SkinCacheService.calculateSHA256(bytes("{\"name\":\"Walk\"}")), entries.get(0).hash());
    }

    @Test
    void listUsesFileNameForBrokenJson() throws Exception {
        write("broken.json", "{");

        List<AnimSyncService.AnimListEntry> entries = TestSupport.await(AnimSyncService.listAnimsAsync());

        assertEquals(1, entries.size());
        assertEquals("broken", entries.get(0).name());
    }

    @Test
    void listUsesCacheUntilRefreshExplicitlyRequested() throws Exception {
        write("a.json", "{\"name\":\"A\"}");
        assertEquals(1, TestSupport.await(AnimSyncService.listAnimsAsync()).size());

        write("b.json", "{\"name\":\"B\"}");
        assertEquals(1, TestSupport.await(AnimSyncService.listAnimsAsync()).size());

        assertEquals(2, TestSupport.await(AnimSyncService.refreshCache()));
        assertEquals(2, TestSupport.await(AnimSyncService.listAnimsAsync()).size());
    }

    @Test
    void listIsEmptyWhenAnimPathIsAFile() throws Exception {
        Files.delete(animDir);
        Files.writeString(animDir, "blocker", StandardCharsets.UTF_8);

        assertTrue(TestSupport.await(AnimSyncService.listAnimsAsync()).isEmpty());
    }

    @Test
    void loadFindsAnimationsByPrimaryName() throws Exception {
        byte[] data = bytes("{\"name\":\"Walk\"}");
        write("walk.json", "{\"name\":\"Walk\"}");
        TestSupport.await(AnimSyncService.refreshCache());

        AnimSyncService.AnimLoadResult result = TestSupport.await(AnimSyncService.loadAnimAsync("Walk"));

        assertTrue(result.isSuccess());
        assertEquals("Walk", result.name());
        assertArrayEquals(data, result.jsonData());
        assertEquals(SkinCacheService.calculateSHA256(data), result.hash());
    }

    @Test
    void loadFindsAnimationsByAnimationsKey() throws Exception {
        write("file.json", "{\"animations\":{\"run\":{}}}");
        TestSupport.await(AnimSyncService.refreshCache());

        assertTrue(TestSupport.await(AnimSyncService.loadAnimAsync("run")).isSuccess());
    }

    @Test
    void loadFindsAnimationsByPlayerAnimationLibraryName() throws Exception {
        write("file.json", "{\"animations\":{\"key\":{\"player_animation_library\":{\"name\":\"Pal\"}}}}");
        TestSupport.await(AnimSyncService.refreshCache());

        assertTrue(TestSupport.await(AnimSyncService.loadAnimAsync("Pal")).isSuccess());
    }

    @Test
    void loadFindsAnimationsByFileName() throws Exception {
        write("dance.json", "{}");
        TestSupport.await(AnimSyncService.refreshCache());

        assertTrue(TestSupport.await(AnimSyncService.loadAnimAsync("dance")).isSuccess());
    }

    @Test
    void loadIsCaseInsensitiveAndHandlesUppercaseExtension() throws Exception {
        write("WALK.JSON", "{\"name\":\"Walk\"}");
        TestSupport.await(AnimSyncService.refreshCache());

        assertTrue(TestSupport.await(AnimSyncService.loadAnimAsync("walk")).isSuccess());
    }

    @Test
    void loadRejectsEmptyNames() {
        assertEquals("Empty animation name", TestSupport.await(AnimSyncService.loadAnimAsync(null)).errorMsg());
        assertEquals("Empty animation name", TestSupport.await(AnimSyncService.loadAnimAsync("")).errorMsg());
        assertEquals("Empty animation name", TestSupport.await(AnimSyncService.loadAnimAsync("   ")).errorMsg());
    }

    @Test
    void loadRejectsInvalidNames() {
        for (String name : List.of("..", "a/b", "a\\b", "a\u0001b", "a".repeat(129))) {
            assertEquals("Invalid animation name", TestSupport.await(AnimSyncService.loadAnimAsync(name)).errorMsg());
        }
    }

    @Test
    void loadReportsMissingAnimation() {
        assertEquals("Animation not found on the server",
                TestSupport.await(AnimSyncService.loadAnimAsync("missing")).errorMsg());
    }

    @Test
    void loadReportsMissingFolder() throws Exception {
        Files.delete(animDir);
        Files.writeString(animDir, "blocker", StandardCharsets.UTF_8);

        assertEquals("Animation folder not found",
                TestSupport.await(AnimSyncService.loadAnimAsync("walk")).errorMsg());
    }

    @Test
    void loadRejectsOversizedFiles() throws Exception {
        Files.write(animDir.resolve("big.json"), new byte[MAX_FILE_SIZE + 1]);
        TestSupport.await(AnimSyncService.refreshCache());

        assertEquals("Animation file too large or unreadable",
                TestSupport.await(AnimSyncService.loadAnimAsync("big")).errorMsg());
    }

    @Test
    void loadFindsFileAddedAfterRefresh() throws Exception {
        write("late.json", "{\"name\":\"Late\"}");

        assertTrue(TestSupport.await(AnimSyncService.loadAnimAsync("Late")).isSuccess());
    }

    @Test
    void saveWritesFileAndRefreshesCache() throws Exception {
        byte[] data = bytes("{\"name\":\"Walk\"}");

        AnimSyncService.AnimSaveResult result = TestSupport.await(
                AnimSyncService.saveAnimAsync("Walk", data, SkinCacheService.calculateSHA256(data)));

        assertTrue(result.success());
        assertEquals("envoys.cmd.anim.push_done", result.messageKey());
        assertArrayEquals(data, Files.readAllBytes(animDir.resolve("Walk.json")));
        assertFalse(Files.exists(animDir.resolve("Walk.json.tmp")));
        assertEquals(1, TestSupport.await(AnimSyncService.listAnimsAsync()).size());
    }

    @Test
    void saveNormalizesJsonSuffix() throws Exception {
        byte[] data = bytes("{\"name\":\"Walk\"}");

        AnimSyncService.AnimSaveResult result = TestSupport.await(
                AnimSyncService.saveAnimAsync("Walk.json", data, SkinCacheService.calculateSHA256(data)));

        assertTrue(result.success());
        assertTrue(Files.isRegularFile(animDir.resolve("Walk.json")));
        assertFalse(Files.exists(animDir.resolve("Walk.json.json")));
    }

    @Test
    void saveRejectsUnsafeNames() throws Exception {
        byte[] data = bytes("{}");
        String hash = SkinCacheService.calculateSHA256(data);

        for (String name : List.of("", "   ", "..", "a/b", "a\\b", "a\u0001b", "a".repeat(129))) {
            AnimSyncService.AnimSaveResult result = TestSupport.await(
                    AnimSyncService.saveAnimAsync(name, data, hash));
            assertFalse(result.success());
            assertEquals("envoys.cmd.anim.bad_name", result.messageKey());
        }

        try (var files = Files.list(animDir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void saveRejectsNullAndEmptyData() {
        assertEquals("envoys.cmd.anim.invalid_json",
                TestSupport.await(AnimSyncService.saveAnimAsync("walk", null, "hash")).messageKey());
        assertEquals("envoys.cmd.anim.invalid_json",
                TestSupport.await(AnimSyncService.saveAnimAsync("walk", new byte[0], "hash")).messageKey());
    }

    @Test
    void saveRejectsOversizedData() {
        byte[] data = new byte[MAX_FILE_SIZE + 1];
        assertEquals("envoys.cmd.anim.too_big",
                TestSupport.await(AnimSyncService.saveAnimAsync("walk", data, "hash")).messageKey());
    }

    @Test
    void saveRejectsMalformedJson() {
        byte[] data = bytes("{");
        assertEquals("envoys.cmd.anim.invalid_json",
                TestSupport.await(AnimSyncService.saveAnimAsync("walk", data,
                        SkinCacheService.calculateSHA256(data))).messageKey());
    }

    @Test
    void saveRejectsHashMismatch() {
        byte[] data = bytes("{\"name\":\"Walk\"}");
        assertEquals("envoys.cmd.anim.hash_mismatch",
                TestSupport.await(AnimSyncService.saveAnimAsync("walk", data, null)).messageKey());
        assertEquals("envoys.cmd.anim.hash_mismatch",
                TestSupport.await(AnimSyncService.saveAnimAsync("walk", data, "deadbeef")).messageKey());
    }

    @Test
    void saveOverwritesExistingFile() throws Exception {
        byte[] first = bytes("{\"name\":\"First\"}");
        byte[] second = bytes("{\"name\":\"Second\"}");

        TestSupport.await(AnimSyncService.saveAnimAsync("walk", first, SkinCacheService.calculateSHA256(first)));
        TestSupport.await(AnimSyncService.saveAnimAsync("walk", second, SkinCacheService.calculateSHA256(second)));

        assertArrayEquals(second, Files.readAllBytes(animDir.resolve("walk.json")));
    }

    @Test
    void deleteByFileName() throws Exception {
        write("walk.json", "{\"name\":\"Walk\"}");
        TestSupport.await(AnimSyncService.refreshCache());

        AnimSyncService.AnimSaveResult result = TestSupport.await(AnimSyncService.deleteAnimAsync("walk"));

        assertTrue(result.success());
        assertEquals("envoys.cmd.anim.delete_done", result.messageKey());
        assertFalse(Files.exists(animDir.resolve("walk.json")));
    }

    @Test
    void deleteByPrimaryName() throws Exception {
        write("file.json", "{\"name\":\"Walk\"}");
        TestSupport.await(AnimSyncService.refreshCache());

        assertTrue(TestSupport.await(AnimSyncService.deleteAnimAsync("Walk")).success());
        assertFalse(Files.exists(animDir.resolve("file.json")));
    }

    @Test
    void deleteByAnimationsKey() throws Exception {
        write("file.json", "{\"animations\":{\"run\":{}}}");
        TestSupport.await(AnimSyncService.refreshCache());

        assertTrue(TestSupport.await(AnimSyncService.deleteAnimAsync("run")).success());
        assertFalse(Files.exists(animDir.resolve("file.json")));
    }

    @Test
    void deleteByPlayerAnimationLibraryName() throws Exception {
        write("file.json", "{\"animations\":{\"key\":{\"player_animation_library\":{\"name\":\"Pal\"}}}}");
        TestSupport.await(AnimSyncService.refreshCache());

        assertTrue(TestSupport.await(AnimSyncService.deleteAnimAsync("Pal")).success());
        assertFalse(Files.exists(animDir.resolve("file.json")));
    }

    @Test
    void deleteReportsMissingAnimation() {
        AnimSyncService.AnimSaveResult result = TestSupport.await(AnimSyncService.deleteAnimAsync("missing"));
        assertFalse(result.success());
        assertEquals("envoys.cmd.anim.not_found_server", result.messageKey());
    }

    @Test
    void deleteRejectsUnsafeNames() throws Exception {
        Path outside = tempDir.resolve("world/envoys/evil.json");
        Files.writeString(outside, "{}", StandardCharsets.UTF_8);

        AnimSyncService.AnimSaveResult result = TestSupport.await(AnimSyncService.deleteAnimAsync("../evil"));

        assertFalse(result.success());
        assertEquals("envoys.cmd.anim.bad_name", result.messageKey());
        assertTrue(Files.exists(outside));
    }

    @Test
    void deleteFindsFileAddedAfterRefresh() throws Exception {
        write("late.json", "{}");

        assertTrue(TestSupport.await(AnimSyncService.deleteAnimAsync("late")).success());
        assertFalse(Files.exists(animDir.resolve("late.json")));
    }

    private Path write(String name, String content) throws Exception {
        Files.createDirectories(animDir);
        Path file = animDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

}
