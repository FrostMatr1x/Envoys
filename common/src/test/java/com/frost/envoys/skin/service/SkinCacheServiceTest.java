package com.frost.envoys.skin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
