package com.frost.envoys.skin.service;

import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.model.SkinIndexEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinLocalServiceTest {

    @Test
    void safeFileNames() {
        assertTrue(SkinLocalService.isSafeSkinFileName("a.png"));
        assertTrue(SkinLocalService.isSafeSkinFileName("a-b_c.1.png"));
        assertFalse(SkinLocalService.isSafeSkinFileName(".hidden.png"));
        assertFalse(SkinLocalService.isSafeSkinFileName("../a.png"));
        assertFalse(SkinLocalService.isSafeSkinFileName("a.txt"));
        assertFalse(SkinLocalService.isSafeSkinFileName(null));
    }

    @Test
    void readIndexMissingCorruptAndFiltered(@TempDir Path tempDir) throws Exception {
        File missing = tempDir.resolve("missing.json").toFile();
        assertTrue(SkinLocalService.readIndex(missing).skins.isEmpty());

        File corrupt = tempDir.resolve("corrupt.json").toFile();
        Files.writeString(corrupt.toPath(), "{not json", StandardCharsets.UTF_8);
        assertNull(SkinLocalService.readIndex(corrupt));

        File filtered = tempDir.resolve("filtered.json").toFile();
        Files.writeString(filtered.toPath(),
                "{\"skins\":[null,{\"uuid\":null,\"path\":null},{\"uuid\":\"u\",\"path\":\"p.png\"}]}",
                StandardCharsets.UTF_8);
        SkinIndexData data = SkinLocalService.readIndex(filtered);
        assertEquals(1, data.skins.size());
        assertEquals("u", data.skins.get(0).uuid);
    }

    @Test
    void readIndexWithNullSkinsBecomesEmpty(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("null.json").toFile();
        Files.writeString(file.toPath(), "{\"skins\":null}", StandardCharsets.UTF_8);
        assertTrue(SkinLocalService.readIndex(file).skins.isEmpty());
    }

    @Test
    void addOrUpdateDeduplicatesCaseInsensitive(@TempDir Path tempDir) {
        File index = tempDir.resolve("index.json").toFile();

        assertFalse(SkinLocalService.addOrUpdateIndex(index, null));
        assertFalse(SkinLocalService.addOrUpdateIndex(index, new SkinIndexEntry()));
        assertTrue(SkinLocalService.addOrUpdateIndex(index, entry("ABC", "a.png")));
        assertTrue(SkinLocalService.addOrUpdateIndex(index, entry("abc", "b.png")));

        SkinIndexData data = SkinLocalService.readIndex(index);
        assertEquals(1, data.skins.size());
        assertEquals("b.png", data.skins.get(0).path);
    }

    @Test
    void saveIndexRoundTrip(@TempDir Path tempDir) {
        File index = tempDir.resolve("index.json").toFile();
        SkinIndexData data = new SkinIndexData();
        data.skins.add(entry("u", "u.png"));
        SkinLocalService.saveIndex(index, data);

        SkinIndexData read = SkinLocalService.readIndex(index);
        assertEquals(1, read.skins.size());
        assertEquals("u", read.skins.get(0).uuid);
        assertEquals("u.png", read.skins.get(0).path);
        assertEquals("hash", read.skins.get(0).hash);
    }

    @Test
    void findsSkinRecursivelyIgnoringCache(@TempDir Path tempDir) throws Exception {
        Path root = tempDir.resolve("skins");
        Path cache = root.resolve("cache");
        Path sub = root.resolve("sub");
        Files.createDirectories(cache);
        Files.createDirectories(sub);
        Files.write(cache.resolve("target.png"), new byte[]{1});
        Files.write(sub.resolve("target.png"), new byte[]{2});

        Optional<Path> found = SkinLocalService.findSkinRecursively(root, "target");
        assertTrue(found.isPresent());
        assertEquals(sub.resolve("target.png"), found.get());

        assertTrue(SkinLocalService.findSkinRecursively(root, "target.png").isPresent());
        assertTrue(SkinLocalService.findSkinRecursively(root.resolve("missing"), "target").isEmpty());
    }

    private static SkinIndexEntry entry(String uuid, String path) {
        return new SkinIndexEntry(uuid, path, "classic", "local", "hash", 1L);
    }
}
