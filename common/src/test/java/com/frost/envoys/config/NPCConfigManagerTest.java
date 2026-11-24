package com.frost.envoys.config;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcClickEvent;
import com.frost.envoys.action.model.ActionStart;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestType;
import com.frost.envoys.util.PathManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NPCConfigManagerTest {

    private static final String QUEST_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String OTHER_QUEST_UUID = "11111111-2222-3333-4444-555555555555";

    @TempDir
    Path tempDir;

    private Path npcDir;
    private Path gameDir;

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
        TestSupport.resetNpcConfigMigration();
        TestSupport.resetQuestState();
        npcDir = tempDir.resolve("world/envoys/npcs");
        gameDir = tempDir.resolve("game");
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

    @Test
    void saveAndLoadRoundTrip() throws Exception {
        UUID id = TestSupport.uuid(1);
        NPCInteractManager manager = managerWithContent(id);

        NPCConfigManager.save(manager);

        Path file = npcDir.resolve(id + ".json");
        assertTrue(Files.isRegularFile(file));
        assertFalse(Files.exists(npcDir.resolve(id + ".json.tmp")));

        TestSupport.resetQuestState();
        NPCInteractManager loaded = NPCConfigManager.load(id).orElseThrow();
        assertEquals("Bob", loaded.passport.npcName);
        assertTrue(loaded.events.containsKey(EventType.CLICK));
        assertEquals(1, loaded.quests.size());
        assertEquals("q", loaded.quests.get(0).localId);

        TestSupport.resetQuestState();
        NPCConfigManager.loadAll();
        assertTrue(NPCInteractManager.byUUID(id).isPresent());
    }

    @Test
    void saveSanitizesQuestsBeforeWriting() throws Exception {
        UUID id = TestSupport.uuid(2);
        NPCInteractManager manager = new NPCInteractManager(id);
        manager.quests.add(quest("valid", QUEST_UUID));
        manager.quests.add(quest("valid", OTHER_QUEST_UUID));
        manager.quests.add(quest("another", QUEST_UUID));
        manager.quests.add(question(""));
        manager.quests.add(null);

        NPCConfigManager.save(manager);

        String json = Files.readString(npcDir.resolve(id + ".json"), StandardCharsets.UTF_8);
        assertEquals(1, occurrences(json, "\"local_id\""));
        assertTrue(json.contains("\"valid\""));
    }

    @Test
    void saveReplacesExistingFileAndLeavesNoTemporary() throws Exception {
        UUID id = TestSupport.uuid(3);
        NPCInteractManager manager = new NPCInteractManager(id);
        manager.passport.npcName = "First";
        NPCConfigManager.save(manager);

        manager.passport.npcName = "Second";
        NPCConfigManager.save(manager);

        assertFalse(Files.exists(npcDir.resolve(id + ".json.tmp")));
        TestSupport.resetQuestState();
        assertEquals("Second", NPCConfigManager.load(id).orElseThrow().passport.npcName);
    }

    @Test
    void failedSaveKeepsOriginalFile() throws Exception {
        UUID id = TestSupport.uuid(4);
        NPCInteractManager manager = new NPCInteractManager(id);
        manager.passport.npcName = "Original";
        NPCConfigManager.save(manager);

        Path tmp = npcDir.resolve(id + ".json.tmp");
        Files.createDirectories(tmp.resolve("blocker"));

        manager.passport.npcName = "Replacement";
        NPCConfigManager.save(manager);

        TestSupport.resetQuestState();
        assertEquals("Original", NPCConfigManager.load(id).orElseThrow().passport.npcName);
    }

    @Test
    void migratesLegacyConfigsWhenNewDirectoryIsEmpty() throws Exception {
        UUID id = TestSupport.uuid(5);
        Path legacy = gameDir.resolve("world/envoys/npcs");
        Files.createDirectories(legacy);
        Files.writeString(legacy.resolve(id + ".json"), configJson(id), StandardCharsets.UTF_8);
        TestSupport.resetNpcConfigMigration();

        List<UUID> uuids = NPCConfigManager.getAllSavedUuids();

        assertTrue(uuids.contains(id));
        assertTrue(Files.isRegularFile(npcDir.resolve(id + ".json")));
    }

    @Test
    void doesNotMigrateWhenNewDirectoryAlreadyHasConfigs() throws Exception {
        UUID existing = TestSupport.uuid(6);
        UUID legacyId = TestSupport.uuid(7);
        Files.createDirectories(npcDir);
        Files.writeString(npcDir.resolve(existing + ".json"), configJson(existing), StandardCharsets.UTF_8);

        Path legacy = gameDir.resolve("world/envoys/npcs");
        Files.createDirectories(legacy);
        Files.writeString(legacy.resolve(legacyId + ".json"), configJson(legacyId), StandardCharsets.UTF_8);
        TestSupport.resetNpcConfigMigration();

        List<UUID> uuids = NPCConfigManager.getAllSavedUuids();

        assertEquals(List.of(existing), uuids);
        assertFalse(Files.exists(npcDir.resolve(legacyId + ".json")));
    }

    @Test
    void migrationWithoutLegacyDirectoryIsSilent() {
        assertDoesNotThrow(NPCConfigManager::getAllSavedUuids);
        assertTrue(NPCConfigManager.getAllSavedUuids().isEmpty());
    }

    @Test
    void loadHandlesMissingMalformedNullAndMismatchedUuid() throws Exception {
        UUID id = TestSupport.uuid(8);
        assertTrue(NPCConfigManager.load(id).isEmpty());

        Files.createDirectories(npcDir);
        Files.writeString(npcDir.resolve(id + ".json"), "{not json", StandardCharsets.UTF_8);
        assertTrue(NPCConfigManager.load(id).isEmpty());

        Files.writeString(npcDir.resolve(id + ".json"), "null", StandardCharsets.UTF_8);
        assertTrue(NPCConfigManager.load(id).isEmpty());

        UUID other = TestSupport.uuid(9);
        Files.writeString(npcDir.resolve(id + ".json"), configJson(other), StandardCharsets.UTF_8);
        NPCInteractManager loaded = NPCConfigManager.load(id).orElseThrow();
        assertEquals(other, loaded.npcUUID);
    }

    @Test
    void deleteIfCorruptOnlyRemovesUnreadableFiles() throws Exception {
        Files.createDirectories(npcDir);

        UUID missing = TestSupport.uuid(10);
        assertFalse(NPCConfigManager.deleteIfCorrupt(missing));

        UUID blank = TestSupport.uuid(11);
        Files.writeString(npcDir.resolve(blank + ".json"), "   ", StandardCharsets.UTF_8);
        assertFalse(NPCConfigManager.deleteIfCorrupt(blank));
        assertTrue(Files.exists(npcDir.resolve(blank + ".json")));

        UUID valid = TestSupport.uuid(12);
        Files.writeString(npcDir.resolve(valid + ".json"), configJson(valid), StandardCharsets.UTF_8);
        assertFalse(NPCConfigManager.deleteIfCorrupt(valid));
        assertTrue(Files.exists(npcDir.resolve(valid + ".json")));

        UUID nullJson = TestSupport.uuid(13);
        Files.writeString(npcDir.resolve(nullJson + ".json"), "null", StandardCharsets.UTF_8);
        assertTrue(NPCConfigManager.deleteIfCorrupt(nullJson));
        assertFalse(Files.exists(npcDir.resolve(nullJson + ".json")));

        UUID malformed = TestSupport.uuid(14);
        Files.writeString(npcDir.resolve(malformed + ".json"), "{", StandardCharsets.UTF_8);
        assertTrue(NPCConfigManager.deleteIfCorrupt(malformed));
        assertFalse(Files.exists(npcDir.resolve(malformed + ".json")));

        UUID directory = TestSupport.uuid(15);
        Files.createDirectories(npcDir.resolve(directory + ".json"));
        assertFalse(NPCConfigManager.deleteIfCorrupt(directory));
    }

    @Test
    void getAllSavedUuidsAndLoadAllIgnoreForeignFiles() throws Exception {
        UUID valid = TestSupport.uuid(16);
        Files.createDirectories(npcDir);
        Files.writeString(npcDir.resolve(valid + ".json"), configJson(valid), StandardCharsets.UTF_8);
        Files.writeString(npcDir.resolve(TestSupport.uuid(17) + ".trades.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(npcDir.resolve("not-a-uuid.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(npcDir.resolve("notes.txt"), "text", StandardCharsets.UTF_8);
        Files.createDirectories(npcDir.resolve("subdir"));

        assertEquals(List.of(valid), NPCConfigManager.getAllSavedUuids());

        TestSupport.resetQuestState();
        NPCConfigManager.loadAll();

        assertEquals(1, NPCInteractManager.SCRIPTS.size());
        assertTrue(NPCInteractManager.byUUID(valid).isPresent());
    }

    @Test
    void loadAllClearsStaleScriptsFromPreviousWorld() {
        UUID stale = TestSupport.uuid(18);
        new NPCInteractManager(stale);
        assertTrue(NPCInteractManager.byUUID(stale).isPresent());

        NPCConfigManager.loadAll();

        assertTrue(NPCInteractManager.byUUID(stale).isEmpty());
    }

    @Test
    void deleteRemovesConfigLuaDirectoryAndRegistration() throws Exception {
        UUID id = TestSupport.uuid(19);
        NPCInteractManager manager = new NPCInteractManager(id);
        NPCConfigManager.save(manager);

        Path luaDir = tempDir.resolve("world/envoys/lua").resolve(id.toString());
        Files.createDirectories(luaDir);
        Files.writeString(luaDir.resolve("main.lua"), "print('x')", StandardCharsets.UTF_8);

        NPCConfigManager.delete(id);

        assertFalse(Files.exists(npcDir.resolve(id + ".json")));
        assertFalse(Files.exists(luaDir));
        assertTrue(NPCInteractManager.byUUID(id).isEmpty());
    }

    @Test
    void deleteWithoutInitializedServerDoesNotThrow() {
        PathManager.clearServer();
        assertDoesNotThrow(() -> NPCConfigManager.delete(TestSupport.uuid(20)));
    }

    @Test
    void saveAllPersistsEveryRegisteredNpc() {
        UUID first = TestSupport.uuid(21);
        UUID second = TestSupport.uuid(22);
        new NPCInteractManager(first);
        new NPCInteractManager(second);

        NPCConfigManager.saveAll();

        assertTrue(Files.isRegularFile(npcDir.resolve(first + ".json")));
        assertTrue(Files.isRegularFile(npcDir.resolve(second + ".json")));
    }

    @Test
    void fallsBackToGameDirWorldWhenServerIsNotInitialized() throws Exception {
        PathManager.clearServer();
        UUID id = TestSupport.uuid(23);
        NPCInteractManager manager = new NPCInteractManager(id);
        manager.passport.npcName = "Fallback";

        NPCConfigManager.save(manager);

        Path fallback = gameDir.resolve("world/envoys/npcs").resolve(id + ".json");
        assertTrue(Files.isRegularFile(fallback));

        TestSupport.resetQuestState();
        assertEquals("Fallback", NPCConfigManager.load(id).orElseThrow().passport.npcName);
    }

    private NPCInteractManager managerWithContent(UUID id) {
        NPCInteractManager manager = new NPCInteractManager(id);
        manager.passport.npcName = "Bob";
        NpcClickEvent click = new NpcClickEvent();
        click.actions().add(new ActionStart("a"));
        manager.putEvent(EventType.CLICK, click);
        manager.quests.add(quest("q", QUEST_UUID));
        return manager;
    }

    private static QuestDefinition quest(String localId, String uuid) {
        QuestDefinition quest = new QuestDefinition();
        quest.localId = localId;
        quest.questUuid = uuid;
        quest.type = QuestType.BOOLEAN;
        quest.requiredCompletions = 1;
        return quest;
    }

    private static QuestDefinition question(String localId) {
        QuestDefinition quest = new QuestDefinition();
        quest.localId = localId;
        return quest;
    }

    private static String configJson(UUID id) {
        return "{\"npcUUID\":\"" + id + "\",\"passport\":{},\"events\":{},\"quests\":[]}";
    }

    private static int occurrences(String value, String needle) {
        return value.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }
}
