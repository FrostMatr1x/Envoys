package com.frost.envoys.skin.service;

import com.frost.envoys.TestSupport;
import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.model.SkinIndexEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinSyncServiceTest {

    private static final int MAX_SKIN_BYTES = 2 * 1024 * 1024;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

    @Test
    void resolveFileKeyFallsBackToUnknown() {
        assertEquals("unknown", SkinSyncService.resolveFileKey(null, "URL"));
        assertEquals("unknown", SkinSyncService.resolveFileKey("", "URL"));
        assertEquals("unknown", SkinSyncService.resolveFileKey("   ", "URL"));
    }

    @Test
    void resolveFileKeyAddsSchemeForUrlType() {
        String expected = SkinSyncService.sanitizeFileName("https://example.com/skin.png");
        assertEquals(expected, SkinSyncService.resolveFileKey("example.com/skin.png", "URL"));
        assertEquals(expected, SkinSyncService.resolveFileKey("https://example.com/skin.png", "URL"));
    }

    @Test
    void resolveFileKeyTrimsAndStripsPngSuffix() {
        assertEquals("steve", SkinSyncService.resolveFileKey("  steve.png  ", "LOCAL"));
        assertEquals("steve", SkinSyncService.resolveFileKey("steve", "LOCAL"));
    }

    @Test
    void resolveFileKeySanitizesUnsafeCharacters() {
        assertEquals("a_b_c", SkinSyncService.resolveFileKey("a b/c", "LOCAL"));
    }

    @Test
    void sanitizeFileNameHashesUrlsDeterministically() {
        String first = SkinSyncService.sanitizeFileName("https://example.com/a.png");
        String second = SkinSyncService.sanitizeFileName("https://example.com/a.png");
        String other = SkinSyncService.sanitizeFileName("https://example.com/b.png");

        assertEquals(first, second);
        assertFalse(first.equals(other));
        assertTrue(first.matches("[0-9a-f-]{36}"));
    }

    @Test
    void sanitizeFileNameHandlesPlainNames() {
        assertEquals("AbC", SkinSyncService.sanitizeFileName("AbC.PNG"));
        assertEquals("a_b", SkinSyncService.sanitizeFileName("a b"));
        assertEquals("unknown", SkinSyncService.sanitizeFileName(null));
        assertEquals("unknown", SkinSyncService.sanitizeFileName("  "));
    }

    @Test
    void resolveStorageUsesServerWorldDirectories() {
        SkinSyncService.Storage storage = SkinSyncService.resolveStorage(false, null);

        assertEquals(tempDir.resolve("world/envoys/skins/cache"), storage.cacheDir());
        assertEquals(tempDir.resolve("world/envoys/skin/index.json").toFile(), storage.indexFile());
        assertEquals(tempDir.resolve("world/envoys/skins"), storage.searchDir());
    }

    @Test
    void resolveStorageUsesClientCacheDirectories() {
        SkinSyncService.Storage storage = SkinSyncService.resolveStorage(true, "srv");

        assertEquals(tempDir.resolve("game/envoysCache/srv/skins/cache"), storage.cacheDir());
        assertEquals(tempDir.resolve("game/envoysCache/srv/skin/index.json").toFile(), storage.indexFile());
        assertEquals(tempDir.resolve("game/envoysCache/srv"), storage.searchDir());
    }

    @Test
    void cacheHitReturnsWithoutCallingGateway() throws Exception {
        SkinSyncService.Storage storage = storage();
        byte[] png = TestSupport.png64(false);
        String hash = SkinCacheService.calculateSHA256(png);
        Files.write(storage.cacheDir().resolve("steve.png"), png);
        SkinLocalService.addOrUpdateIndex(storage.indexFile(),
                new SkinIndexEntry("steve", "steve.png", "classic", "local", hash, 1L));
        FakeGateway gateway = new FakeGateway();

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("steve", "LOCAL", storage, gateway));

        assertTrue(result.isSuccess());
        assertArrayEquals(png, result.pngData());
        assertEquals(hash, result.hash());
        assertEquals("classic", result.model());
        assertEquals("local", result.source());
        assertTrue(gateway.downloaded.isEmpty());
    }

    @Test
    void cacheHashMismatchFallsThroughToDownload() throws Exception {
        SkinSyncService.Storage storage = storage();
        String key = SkinSyncService.resolveFileKey("https://8.8.8.8/skin", "URL");
        Files.write(storage.cacheDir().resolve(key + ".png"), new byte[]{1, 2, 3});
        SkinLocalService.addOrUpdateIndex(storage.indexFile(),
                new SkinIndexEntry(key, key + ".png", "classic", "local", "stale-hash", 1L));
        byte[] png = TestSupport.png64(false);
        FakeGateway gateway = new FakeGateway();
        gateway.downloadResult = png;

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("https://8.8.8.8/skin", "URL", storage, gateway));

        assertTrue(result.isSuccess());
        assertEquals("url", result.source());
        assertEquals(1, gateway.downloaded.size());
    }

    @Test
    void urlFlowDownloadsCachesAndIndexes() throws Exception {
        SkinSyncService.Storage storage = storage();
        byte[] png = TestSupport.png64(false);
        FakeGateway gateway = new FakeGateway();
        gateway.downloadResult = png;

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("https://8.8.8.8/skin", "URL", storage, gateway));

        assertTrue(result.isSuccess());
        assertEquals("url", result.source());
        assertEquals("default", result.model());
        assertEquals(List.of("https://8.8.8.8/skin"), gateway.downloaded);

        String key = SkinSyncService.resolveFileKey("https://8.8.8.8/skin", "URL");
        assertTrue(Files.isRegularFile(storage.cacheDir().resolve(key + ".png")));
        SkinIndexData index = SkinLocalService.readIndex(storage.indexFile());
        assertEquals(1, index.skins.size());
        assertEquals("url", index.skins.get(0).source);
        assertEquals(SkinCacheService.calculateSHA256(png), index.skins.get(0).hash);
    }

    @Test
    void urlTypeWithoutSchemeIsUpgraded() throws Exception {
        SkinSyncService.Storage storage = storage();
        FakeGateway gateway = new FakeGateway();
        gateway.downloadResult = TestSupport.png64(false);

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("8.8.8.8/skin.png", "URL", storage, gateway));

        assertTrue(result.isSuccess());
        assertEquals("https://8.8.8.8/skin.png", gateway.downloaded.get(0));
    }

    @Test
    void downloadExceptionIsReportedAsNetworkError() throws Exception {
        SkinSyncService.Storage storage = storage();
        FakeGateway gateway = new FakeGateway();
        gateway.downloadError = new IOException("boom");

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("https://8.8.8.8/skin", "URL", storage, gateway));

        assertFalse(result.isSuccess());
        assertEquals("Network or download error", result.errorMsg());
    }

    @Test
    void illegalArgumentMessageIsSurfacedVerbatim() throws Exception {
        SkinSyncService.Storage storage = storage();
        FakeGateway gateway = new FakeGateway();
        gateway.downloadError = new IllegalArgumentException("bad url");

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("https://8.8.8.8/skin", "URL", storage, gateway));

        assertFalse(result.isSuccess());
        assertEquals("bad url", result.errorMsg());
    }

    @Test
    void localFlowReadsCachesAndIndexes() throws Exception {
        SkinSyncService.Storage storage = storage();
        byte[] png = TestSupport.png64(false);
        Files.write(storage.searchDir().resolve("steve.png"), png);
        FakeGateway gateway = new FakeGateway();

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("steve", "LOCAL", storage, gateway));

        assertTrue(result.isSuccess());
        assertEquals("local", result.source());
        assertArrayEquals(png, result.pngData());
        assertTrue(Files.isRegularFile(storage.cacheDir().resolve("steve.png")));
        assertEquals(1, SkinLocalService.readIndex(storage.indexFile()).skins.size());
        assertTrue(gateway.downloaded.isEmpty());
    }

    @Test
    void localOversizedFileIsRejected() throws Exception {
        SkinSyncService.Storage storage = storage();
        Files.write(storage.searchDir().resolve("steve.png"), new byte[MAX_SKIN_BYTES + 1]);

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("steve", "LOCAL", storage, new FakeGateway()));

        assertEquals("Skin not found", result.errorMsg());
    }

    @Test
    void localEmptyFileIsRejected() throws Exception {
        SkinSyncService.Storage storage = storage();
        Files.write(storage.searchDir().resolve("steve.png"), new byte[0]);

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("steve", "LOCAL", storage, new FakeGateway()));

        assertEquals("Skin not found", result.errorMsg());
    }

    @Test
    void localSymlinkEscapingSearchRootIsRejected() throws Exception {
        SkinSyncService.Storage storage = storage();
        Path outside = tempDir.resolve("outside.png");
        Files.write(outside, TestSupport.png64(false));
        Path link = storage.searchDir().resolve("steve.png");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Filesystem does not support symlinks");
        }

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("steve", "LOCAL", storage, new FakeGateway()));

        assertEquals("Skin not found", result.errorMsg());
    }

    @Test
    void nicknameFlowUsesMojangModelAndTexture() throws Exception {
        SkinSyncService.Storage storage = storage();
        byte[] png = TestSupport.png64(false);
        FakeGateway gateway = new FakeGateway();
        gateway.mojangResult = Optional.of(new SkinRemoteService.MojangSkinResult(
                "https://8.8.8.8/texture", "slim"));
        gateway.downloadResult = png;

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("Notch", "NICKNAME", storage, gateway));

        assertTrue(result.isSuccess());
        assertEquals("mojang", result.source());
        assertEquals("slim", result.model());
        assertEquals(List.of("Notch"), gateway.mojangLookups);
        assertEquals(List.of("https://8.8.8.8/texture"), gateway.downloaded);
    }

    @Test
    void nicknameWithoutMojangResultIsNotFound() throws Exception {
        SkinSyncService.Storage storage = storage();
        FakeGateway gateway = new FakeGateway();

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("Notch", "NICKNAME", storage, gateway));

        assertEquals("Skin not found", result.errorMsg());
        assertTrue(gateway.downloaded.isEmpty());
    }

    @Test
    void emptyQueryIsRejected() throws Exception {
        SkinSyncService.Storage storage = storage();

        assertEquals("Empty request", TestSupport.await(
                SkinSyncService.loadSkinAsync(null, "LOCAL", storage, new FakeGateway())).errorMsg());
        assertEquals("Empty request", TestSupport.await(
                SkinSyncService.loadSkinAsync("   ", "LOCAL", storage, new FakeGateway())).errorMsg());
    }

    @Test
    void corruptIndexDoesNotAbortSuccessfulLocalFlow() throws Exception {
        SkinSyncService.Storage storage = storage();
        Files.writeString(storage.indexFile().toPath(), "{not json", StandardCharsets.UTF_8);
        Files.write(storage.searchDir().resolve("steve.png"), TestSupport.png64(false));

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("steve", "LOCAL", storage, new FakeGateway()));

        assertTrue(result.isSuccess());
        assertEquals("{not json", Files.readString(storage.indexFile().toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void unsafeIndexPathIsIgnored() throws Exception {
        SkinSyncService.Storage storage = storage();
        String key = SkinSyncService.resolveFileKey("https://8.8.8.8/skin", "URL");
        SkinLocalService.addOrUpdateIndex(storage.indexFile(),
                new SkinIndexEntry(key, "../evil.png", "classic", "local", "hash", 1L));
        FakeGateway gateway = new FakeGateway();
        gateway.downloadResult = TestSupport.png64(false);

        SkinSyncService.SkinFetchResult result = TestSupport.await(
                SkinSyncService.loadSkinAsync("https://8.8.8.8/skin", "URL", storage, gateway));

        assertTrue(result.isSuccess());
        assertEquals("url", result.source());
        assertEquals(1, gateway.downloaded.size());
    }

    private SkinSyncService.Storage storage() throws Exception {
        Path cache = Files.createDirectories(tempDir.resolve("cache"));
        Path search = Files.createDirectories(tempDir.resolve("search"));
        File index = tempDir.resolve("index.json").toFile();
        return new SkinSyncService.Storage(cache, index, search);
    }

    private static final class FakeGateway implements SkinSyncService.RemoteGateway {
        private byte[] downloadResult;
        private Exception downloadError;
        private Optional<SkinRemoteService.MojangSkinResult> mojangResult = Optional.empty();
        private final List<String> downloaded = new ArrayList<>();
        private final List<String> mojangLookups = new ArrayList<>();

        @Override
        public byte[] download(String url) throws Exception {
            downloaded.add(url);
            if (downloadError != null) {
                throw downloadError;
            }
            return downloadResult;
        }

        @Override
        public Optional<SkinRemoteService.MojangSkinResult> fetchMojang(String username) {
            mojangLookups.add(username);
            return mojangResult;
        }
    }
}
