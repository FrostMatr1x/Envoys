package com.frost.envoys.npc.merchant;

import com.frost.envoys.TestSupport;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.util.PathManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeCounterStoreTest {

    @TempDir
    Path tempDir;

    private Path tradesDir;
    private Path legacyDir;
    private UUID npc;
    private UUID player;

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
        TestSupport.resetTradeCounterState();
        tradesDir = tempDir.resolve("world/envoys/trades");
        legacyDir = tempDir.resolve("config/envoys/npcs");
        npc = TestSupport.uuid(1);
        player = TestSupport.uuid(2);
    }

    @AfterEach
    void tearDown() {
        TestSupport.resetTradeCounterState();
        TestSupport.clearEnvironment();
    }

    @Test
    void unlimitedWhenTradeIsNullOrLimitNonPositive() {
        assertEquals(Integer.MAX_VALUE, TradeCounterStore.remainingUses(npc, player, 0, null));
        assertEquals(Integer.MAX_VALUE, TradeCounterStore.remainingUses(npc, player, 0, trade(0, -1)));
        assertEquals(Integer.MAX_VALUE, TradeCounterStore.remainingUses(npc, player, 0, trade(-5, -1)));
    }

    @Test
    void remainingUsesAccumulatesCompletedTrades() {
        NPCTrade trade = trade(3, -1);
        assertEquals(3, TradeCounterStore.remainingUses(npc, player, 0, trade));

        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 2);
        assertEquals(2, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(1, TradeCounterStore.remainingUses(npc, player, 0, trade));

        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 5);
        assertEquals(7, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(0, TradeCounterStore.remainingUses(npc, player, 0, trade));
    }

    @Test
    void completedTradesIgnoreNullTradeAndNonPositiveTimes() {
        NPCTrade trade = trade(3, -1);
        TradeCounterStore.onTradeCompleted(npc, player, 0, null, 1);
        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 0);
        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, -2);

        assertEquals(0, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(3, TradeCounterStore.remainingUses(npc, player, 0, trade));
    }

    @Test
    void playersAndOffersHaveIndependentCounters() {
        NPCTrade trade = trade(5, -1);
        UUID secondPlayer = TestSupport.uuid(3);

        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 2);
        TradeCounterStore.onTradeCompleted(npc, secondPlayer, 0, trade, 1);
        TradeCounterStore.onTradeCompleted(npc, player, 1, trade, 4);

        assertEquals(2, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(1, TradeCounterStore.getCompletedTrades(npc, secondPlayer, 0, trade));
        assertEquals(4, TradeCounterStore.getCompletedTrades(npc, player, 1, trade));
        assertEquals(0, TradeCounterStore.getCompletedTrades(npc, secondPlayer, 1, trade));
    }

    @Test
    void saveCreatesTradeCounterFile() {
        NPCTrade trade = trade(3, -1);
        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 1);

        Path file = tradesDir.resolve(npc + ".trades.json");
        assertTrue(Files.isRegularFile(file));
        assertFalse(Files.exists(tradesDir.resolve(npc + ".trades.json.tmp")));
    }

    @Test
    void saveAllWritesEveryKnownNpc() throws Exception {
        NPCTrade trade = trade(3, -1);
        UUID otherNpc = TestSupport.uuid(4);
        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 1);
        TradeCounterStore.onTradeCompleted(otherNpc, player, 0, trade, 1);
        Files.deleteIfExists(tradesDir.resolve(npc + ".trades.json"));
        Files.deleteIfExists(tradesDir.resolve(otherNpc + ".trades.json"));

        TradeCounterStore.saveAll();

        assertTrue(Files.isRegularFile(tradesDir.resolve(npc + ".trades.json")));
        assertTrue(Files.isRegularFile(tradesDir.resolve(otherNpc + ".trades.json")));
    }

    @Test
    void saveForUnknownNpcDoesNotCreateFile() {
        TradeCounterStore.save(TestSupport.uuid(5));
        assertFalse(Files.exists(tradesDir.resolve(TestSupport.uuid(5) + ".trades.json")));
    }

    @Test
    void loadAllRestoresCountersAfterRestart() {
        NPCTrade trade = trade(4, -1);
        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 3);

        TestSupport.resetTradeCounterState();
        TradeCounterStore.loadAll();

        assertEquals(3, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(1, TradeCounterStore.remainingUses(npc, player, 0, trade));
    }

    @Test
    void invalidFileNamesAreIgnoredOnLoad() throws Exception {
        Files.createDirectories(tradesDir);
        Files.writeString(tradesDir.resolve(npc + ".trades.json"), counterJson(player, 0, 2, 0), StandardCharsets.UTF_8);
        Files.writeString(tradesDir.resolve("not-a-uuid.trades.json"), counterJson(player, 0, 9, 0), StandardCharsets.UTF_8);
        Files.writeString(tradesDir.resolve(npc + ".json"), counterJson(player, 0, 7, 0), StandardCharsets.UTF_8);

        TestSupport.resetTradeCounterState();
        TradeCounterStore.loadAll();

        assertEquals(2, TradeCounterStore.getCompletedTrades(npc, player, 0, trade(5, -1)));
    }

    @Test
    void resetTimeExpiresOldCounter() throws Exception {
        long oldTimestamp = System.currentTimeMillis() - (60L * 1000L + 5_000L);
        writeCounter(tradesDir, npc, counterJson(player, 0, 2, oldTimestamp));
        TradeCounterStore.loadAll();

        NPCTrade trade = trade(3, 60);
        assertEquals(0, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(3, TradeCounterStore.remainingUses(npc, player, 0, trade));
    }

    @Test
    void resetTimeKeepsFreshCounter() throws Exception {
        long freshTimestamp = System.currentTimeMillis() - 1_000L;
        writeCounter(tradesDir, npc, counterJson(player, 0, 2, freshTimestamp));
        TradeCounterStore.loadAll();

        NPCTrade trade = trade(3, 60);
        assertEquals(2, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(1, TradeCounterStore.remainingUses(npc, player, 0, trade));
    }

    @Test
    void resetTimeIsIgnoredForNonPositiveZeroAndFutureValues() throws Exception {
        UUID zeroReset = TestSupport.uuid(10);
        UUID negativeReset = TestSupport.uuid(11);
        UUID zeroTimestamp = TestSupport.uuid(12);
        UUID futureTimestamp = TestSupport.uuid(13);
        long oldTimestamp = System.currentTimeMillis() - 3_600_000L;

        writeCounter(tradesDir, zeroReset, counterJson(player, 0, 2, oldTimestamp));
        writeCounter(tradesDir, negativeReset, counterJson(player, 0, 2, oldTimestamp));
        writeCounter(tradesDir, zeroTimestamp, counterJson(player, 0, 2, 0L));
        writeCounter(tradesDir, futureTimestamp, counterJson(player, 0, 2, System.currentTimeMillis() + 60_000L));

        TestSupport.resetTradeCounterState();
        TradeCounterStore.loadAll();

        assertEquals(2, TradeCounterStore.getCompletedTrades(zeroReset, player, 0, trade(3, 0)));
        assertEquals(2, TradeCounterStore.getCompletedTrades(negativeReset, player, 0, trade(3, -10)));
        assertEquals(2, TradeCounterStore.getCompletedTrades(zeroTimestamp, player, 0, trade(3, 60)));
        assertEquals(2, TradeCounterStore.getCompletedTrades(futureTimestamp, player, 0, trade(3, 60)));
    }

    @Test
    void loadHandlesNullAndMalformedFiles() throws Exception {
        UUID nullFile = TestSupport.uuid(14);
        UUID nullPlayers = TestSupport.uuid(15);
        UUID malformed = TestSupport.uuid(16);
        Files.createDirectories(tradesDir);
        Files.writeString(tradesDir.resolve(nullFile + ".trades.json"), "null", StandardCharsets.UTF_8);
        Files.writeString(tradesDir.resolve(nullPlayers + ".trades.json"), "{\"players\":null}", StandardCharsets.UTF_8);
        Files.writeString(tradesDir.resolve(malformed + ".trades.json"), "{not json", StandardCharsets.UTF_8);

        TestSupport.resetTradeCounterState();
        TradeCounterStore.loadAll();

        NPCTrade trade = trade(3, -1);
        assertEquals(0, TradeCounterStore.getCompletedTrades(nullFile, player, 0, trade));
        assertEquals(0, TradeCounterStore.getCompletedTrades(nullPlayers, player, 0, trade));
        assertEquals(0, TradeCounterStore.getCompletedTrades(malformed, player, 0, trade));
    }

    @Test
    void loadSkipsMalformedEntriesButKeepsValidOnes() throws Exception {
        UUID secondPlayer = TestSupport.uuid(17);
        String json = "{\"players\":[null,"
                + "{\"player\":null,\"offers\":[{\"index\":0,\"used\":5,\"lastTradeMillis\":0}]},"
                + "{\"player\":\"not-a-uuid\",\"offers\":[{\"index\":0,\"used\":5,\"lastTradeMillis\":0}]},"
                + "{\"player\":\"" + player + "\",\"offers\":null},"
                + "{\"player\":\"" + secondPlayer + "\",\"offers\":[null,"
                + "{\"index\":0,\"used\":1,\"lastTradeMillis\":0}]}]}";
        writeCounter(tradesDir, npc, json);

        TestSupport.resetTradeCounterState();
        TradeCounterStore.loadAll();

        NPCTrade trade = trade(3, -1);
        assertEquals(0, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(1, TradeCounterStore.getCompletedTrades(npc, secondPlayer, 0, trade));
    }

    @Test
    void migratesLegacyCountersWhenNewDirectoryIsEmpty() throws Exception {
        writeCounter(legacyDir, npc, counterJson(player, 0, 2, 0));
        TestSupport.resetTradeCounterState();

        TradeCounterStore.loadAll();

        assertTrue(Files.isRegularFile(tradesDir.resolve(npc + ".trades.json")));
        assertEquals(2, TradeCounterStore.getCompletedTrades(npc, player, 0, trade(5, -1)));
    }

    @Test
    void doesNotMigrateWhenNewDirectoryHasCounters() throws Exception {
        UUID legacyNpc = TestSupport.uuid(18);
        writeCounter(tradesDir, npc, counterJson(player, 0, 2, 0));
        writeCounter(legacyDir, legacyNpc, counterJson(player, 0, 4, 0));
        TestSupport.resetTradeCounterState();

        TradeCounterStore.loadAll();

        NPCTrade trade = trade(5, -1);
        assertEquals(2, TradeCounterStore.getCompletedTrades(npc, player, 0, trade));
        assertEquals(0, TradeCounterStore.getCompletedTrades(legacyNpc, player, 0, trade));
        assertFalse(Files.exists(tradesDir.resolve(legacyNpc + ".trades.json")));
    }

    @Test
    void migrationWithoutLegacyDirectoryIsSilent() {
        assertDoesNotThrow(TradeCounterStore::loadAll);
        assertEquals(0, TradeCounterStore.getCompletedTrades(npc, player, 0, trade(1, -1)));
    }

    @Test
    void fallsBackToConfigDirWhenServerIsNotInitialized() {
        PathManager.clearServer();
        NPCTrade trade = trade(3, -1);

        TradeCounterStore.onTradeCompleted(npc, player, 0, trade, 1);

        Path fallback = tempDir.resolve("config/envoys/npcs").resolve(npc + ".trades.json");
        assertTrue(Files.isRegularFile(fallback));
    }

    private static NPCTrade trade(int maxTrades, int resetTime) {
        NPCTrade trade = new NPCTrade();
        trade.maxTrades = maxTrades;
        trade.resetTime = resetTime;
        return trade;
    }

    private static String counterJson(UUID playerUuid, int index, int used, long lastTradeMillis) {
        return "{\"players\":[{\"player\":\"" + playerUuid + "\",\"offers\":["
                + "{\"index\":" + index + ",\"used\":" + used
                + ",\"lastTradeMillis\":" + lastTradeMillis + "}]}]}";
    }

    private static void writeCounter(Path dir, UUID npcUuid, String json) throws Exception {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(npcUuid + ".trades.json"), json, StandardCharsets.UTF_8);
    }
}
