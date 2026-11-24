package com.frost.envoys.skin.service;

import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.skin.model.SkinMetaData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinCacheServiceTest {

    private static final String EMPTY_SHA =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String ABC_SHA =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    void knownByteVectors() {
        assertEquals(EMPTY_SHA, SkinCacheService.calculateSHA256(new byte[0]));
        assertEquals(ABC_SHA, SkinCacheService.calculateSHA256("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void fileHashMatchesByteHash(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("abc.bin");
        Files.write(file, "abc".getBytes(StandardCharsets.UTF_8));
        assertEquals(ABC_SHA, SkinCacheService.calculateSHA256(file));
    }

    @Test
    void missingFileYieldsEmptyHash(@TempDir Path tempDir) {
        assertEquals("", SkinCacheService.calculateSHA256(tempDir.resolve("missing.bin")));
    }

    @Test
    void directoryYieldsEmptyHash(@TempDir Path tempDir) throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("directory"));
        assertEquals("", SkinCacheService.calculateSHA256(dir));
    }

    @Test
    void saveToCacheWritesPngAndReadableJson(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        byte[] png = {1, 2, 3, 4};
        SkinMetaData meta = new SkinMetaData("key", "hash", "slim", "url", 123L);

        SkinCacheService.saveToCache(cacheDir, "key", png, meta);

        Path pngFile = cacheDir.resolve("key.png");
        Path jsonFile = cacheDir.resolve("key.json");
        assertArrayEquals(png, Files.readAllBytes(pngFile));
        assertFalse(Files.exists(cacheDir.resolve("key.png.tmp")));
        assertFalse(Files.exists(cacheDir.resolve("key.json.tmp")));

        SkinMetaData read = EntityActionAdapter.GSON.fromJson(
                Files.readString(jsonFile, StandardCharsets.UTF_8), SkinMetaData.class);
        assertEquals("key", read.uuid);
        assertEquals("hash", read.hash);
        assertEquals("slim", read.model);
        assertEquals("url", read.source);
        assertEquals(123L, read.updatedAt);
    }

    @Test
    void saveToCacheCreatesMissingDirectory(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("nested/cache");
        SkinCacheService.saveToCache(cacheDir, "key", new byte[]{1},
                new SkinMetaData("key", "hash", "classic", "local", 1L));

        assertTrue(Files.isDirectory(cacheDir));
        assertTrue(Files.isRegularFile(cacheDir.resolve("key.png")));
    }

    @Test
    void saveToCacheStripsPngSuffixCaseInsensitivelyAndPreservesKeyCase(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        SkinCacheService.saveToCache(cacheDir, "AbC.PNG", new byte[]{1},
                new SkinMetaData("AbC", "hash", "classic", "local", 1L));

        assertTrue(Files.isRegularFile(cacheDir.resolve("AbC.png")));
        assertTrue(Files.isRegularFile(cacheDir.resolve("AbC.json")));
    }

    @Test
    void saveToCacheOverwritesExistingFiles(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        SkinCacheService.saveToCache(cacheDir, "key", new byte[]{1},
                new SkinMetaData("key", "first", "classic", "local", 1L));
        SkinCacheService.saveToCache(cacheDir, "key", new byte[]{9, 8},
                new SkinMetaData("key", "second", "slim", "url", 2L));

        assertArrayEquals(new byte[]{9, 8}, Files.readAllBytes(cacheDir.resolve("key.png")));
        SkinMetaData read = EntityActionAdapter.GSON.fromJson(
                Files.readString(cacheDir.resolve("key.json"), StandardCharsets.UTF_8), SkinMetaData.class);
        assertEquals("second", read.hash);
        assertEquals("slim", read.model);
    }

    @Test
    void saveToCacheCleansTemporaryFilesOnFailure(@TempDir Path tempDir) throws Exception {
        Path cacheDir = Files.createDirectories(tempDir.resolve("cache"));
        Files.createDirectories(cacheDir.resolve("key.png.tmp"));

        assertThrows(Exception.class, () -> SkinCacheService.saveToCache(cacheDir, "key", new byte[]{1},
                new SkinMetaData("key", "hash", "classic", "local", 1L)));

        assertFalse(Files.exists(cacheDir.resolve("key.png")));
        assertFalse(Files.exists(cacheDir.resolve("key.png.tmp")));
    }
}
