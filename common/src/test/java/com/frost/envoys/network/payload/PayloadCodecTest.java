package com.frost.envoys.network.payload;

import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.quest.QuestType;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadCodecTest {

    private static final int MAX_STRING = 1024 * 1024;
    private static final UUID NPC_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    record CodecCase(
            String name,
            StreamCodec<? super RegistryFriendlyByteBuf, ?> codec,
            Object value,
            BiConsumer<Object, Object> equality
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    @BeforeAll
    static void setUp() {
        NetworkTestSupport.bootStrap();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("codecCases")
    void roundTripsPayloads(CodecCase testCase) {
        roundTrip(testCase);
    }

    static Stream<CodecCase> codecCases() {
        return Stream.of(
                passportCase("passport full", new SaveNPCPassportPayload(
                        NPC_ID, "Bob", 1.5f, 0.3f, 40.0f, new Vec3(1.0D, 2.0D, 3.0D),
                        true, true, true, false, true, true, "wave")),
                passportCase("passport empty optional strings", new SaveNPCPassportPayload(
                        NPC_ID, "", 0.0f, 0.0f, 0.0f, Vec3.ZERO,
                        false, false, false, false, false, false, "")),

                skinCase("skin empty data", new SaveNPCSkinPayload(NPC_ID, "NICKNAME", "", "classic", new byte[0])),
                skinCase("skin non-empty data", new SaveNPCSkinPayload(
                        NPC_ID, "URL", "https://example.com/skin.png", "slim", new byte[]{1, 2, 3, 4})),
                skinCase("skin null data normalized", new SaveNPCSkinPayload(NPC_ID, "FILE", "file.png", "wide", null)),

                recordCase("lua script full", SaveNpcLuaScriptPayload.CODEC,
                        new SaveNpcLuaScriptPayload(NPC_ID, "main.lua", "print('hi')", true)),
                recordCase("lua script empty strings", SaveNpcLuaScriptPayload.CODEC,
                        new SaveNpcLuaScriptPayload(NPC_ID, "", "", false)),

                questsCase("quests null list", new SyncPlayerQuestsPayload(null), 0),
                questsCase("quests empty list", new SyncPlayerQuestsPayload(List.of()), 0),
                questsCase("quests two entries", new SyncPlayerQuestsPayload(List.of(questEntry("a"), questEntry("b"))), 2),

                dialogCase("dialog literal", new OpenDialogPayload(
                        NPC_ID, "action", "Bob", Component.literal("Hello"), Map.of("Yes", "next"))),
                dialogCase("dialog translatable", new OpenDialogPayload(
                        NPC_ID, "action", "Bob", Component.translatable("envoys.test.line"), Map.of())),
                dialogCase("dialog nulls", new OpenDialogPayload(
                        NPC_ID, null, null, null, null)),
                dialogCase("dialog multiple answers", new OpenDialogPayload(
                        NPC_ID, "", "", Component.literal("Pick"), orderedAnswers("first", "second", "third"))),

                settingCase("setting full", new OpenSettingGuiPayload(
                        true, NPC_ID, "Bob", 1.0f, 0.2f, 20.0f, new Vec3(4.0D, 5.0D, 6.0D),
                        true, false, true, true, false, true, "{\"a\":1}", "wave")),
                settingCase("setting null json", new OpenSettingGuiPayload(
                        false, NPC_ID, "Bob", 1.0f, 0.2f, 20.0f, Vec3.ZERO,
                        false, false, false, false, false, false, null, "")),

                animListCase("anim list null", new AnimListPayload(null), 0),
                animListCase("anim list empty", new AnimListPayload(List.of()), 0),
                animListCase("anim list two", new AnimListPayload(List.of(
                        new AnimListPayload.AnimInfo("walk", "h1"),
                        new AnimListPayload.AnimInfo("", ""))), 2),

                animDataCase("anim data empty", new AnimDataPayload("walk", new byte[0])),
                animDataCase("anim data non-empty", new AnimDataPayload("walk", new byte[]{9, 8, 7})),

                responseCase("lua response defaults", new NpcLuaScriptResponsePayload(
                        NPC_ID, null, null, false, null)),
                responseCase("lua response exists true", new NpcLuaScriptResponsePayload(
                        NPC_ID, "main.lua", "print(1)", true, Component.literal("ok"))),
                responseCase("lua response exists false", new NpcLuaScriptResponsePayload(
                        NPC_ID, "other.lua", "", false, Component.empty())),

                questEntryCase("quest entry full", questEntry("q-1")),
                questEntryCase("quest entry empty strings", questEntry("")),
                questEntryCase("quest entry null strings", null)
        );
    }

    @Test
    void luaScriptAcceptsFileNameAtLimit() {
        roundTrip(recordCase("limit", SaveNpcLuaScriptPayload.CODEC,
                new SaveNpcLuaScriptPayload(NPC_ID, "a".repeat(256), "", false)));
    }

    @Test
    void luaScriptRejectsFileNameOverLimit() {
        assertThrows(EncoderException.class, () -> encode(SaveNpcLuaScriptPayload.CODEC,
                new SaveNpcLuaScriptPayload(NPC_ID, "a".repeat(257), "", false)));
    }

    @Test
    void luaScriptEncodesSourceAtLimit() {
        encode(SaveNpcLuaScriptPayload.CODEC,
                new SaveNpcLuaScriptPayload(NPC_ID, "main.lua", "a".repeat(MAX_STRING), false));
    }

    @Test
    void luaScriptRejectsSourceOverLimit() {
        assertThrows(EncoderException.class, () -> encode(SaveNpcLuaScriptPayload.CODEC,
                new SaveNpcLuaScriptPayload(NPC_ID, "main.lua", "a".repeat(MAX_STRING + 1), false)));
    }

    @Test
    void settingGuiEncodesJsonAtLimit() {
        encode(OpenSettingGuiPayload.CODEC, new OpenSettingGuiPayload(
                false, NPC_ID, "Bob", 1.0f, 1.0f, 1.0f, Vec3.ZERO,
                true, true, true, true, true, true, "a".repeat(MAX_STRING), ""));
    }

    @Test
    void settingGuiRejectsJsonOverLimit() {
        assertThrows(EncoderException.class, () -> encode(OpenSettingGuiPayload.CODEC, new OpenSettingGuiPayload(
                false, NPC_ID, "Bob", 1.0f, 1.0f, 1.0f, Vec3.ZERO,
                true, true, true, true, true, true, "a".repeat(MAX_STRING + 1), "")));
    }

    @Test
    void luaResponseAcceptsFileNameAtLimit() {
        roundTrip(responseCase("limit", new NpcLuaScriptResponsePayload(
                NPC_ID, "a".repeat(256), "", true, Component.empty())));
    }

    @Test
    void luaResponseRejectsFileNameOverLimit() {
        assertThrows(EncoderException.class, () -> encode(NpcLuaScriptResponsePayload.CODEC,
                new NpcLuaScriptResponsePayload(NPC_ID, "a".repeat(257), "", true, Component.empty())));
    }

    @Test
    void luaResponseEncodesSourceAtLimit() {
        encode(NpcLuaScriptResponsePayload.CODEC,
                new NpcLuaScriptResponsePayload(NPC_ID, "main.lua", "a".repeat(MAX_STRING), true, Component.empty()));
    }

    @Test
    void luaResponseRejectsSourceOverLimit() {
        assertThrows(EncoderException.class, () -> encode(NpcLuaScriptResponsePayload.CODEC,
                new NpcLuaScriptResponsePayload(NPC_ID, "main.lua", "a".repeat(MAX_STRING + 1), true, Component.empty())));
    }

    @Test
    void questsAcceptMaximumCollectionSize() {
        List<ClientQuestEntry> entries = java.util.Collections.nCopies(1024, questEntry("q"));
        roundTrip(questsCase("max", new SyncPlayerQuestsPayload(entries), 1024));
    }

    @Test
    void questsRejectTooManyEntries() {
        RegistryFriendlyByteBuf buf = NetworkTestSupport.registryBuffer();
        try {
            buf.writeInt(1025);
            assertThrows(DecoderException.class, () -> SyncPlayerQuestsPayload.CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void questsRejectNegativeSize() {
        RegistryFriendlyByteBuf buf = NetworkTestSupport.registryBuffer();
        try {
            buf.writeInt(-1);
            assertThrows(DecoderException.class, () -> SyncPlayerQuestsPayload.CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void animListAcceptsMaximumCollectionSize() {
        List<AnimListPayload.AnimInfo> infos = java.util.Collections.nCopies(1024,
                new AnimListPayload.AnimInfo("walk", "hash"));
        roundTrip(animListCase("max", new AnimListPayload(infos), 1024));
    }

    @Test
    void animListRejectsTooManyEntries() {
        RegistryFriendlyByteBuf buf = NetworkTestSupport.registryBuffer();
        try {
            buf.writeVarInt(1025);
            assertThrows(DecoderException.class, () -> AnimListPayload.CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void animListRejectsNegativeSize() {
        RegistryFriendlyByteBuf buf = NetworkTestSupport.registryBuffer();
        try {
            buf.writeVarInt(-1);
            assertThrows(DecoderException.class, () -> AnimListPayload.CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void clientQuestEntryFallsBackToBooleanForUnknownType() {
        RegistryFriendlyByteBuf buf = NetworkTestSupport.registryBuffer();
        try {
            buf.writeUtf("q");
            buf.writeUtf("title");
            buf.writeUtf("npc");
            buf.writeBoolean(true);
            buf.writeUtf("NOT_A_TYPE");
            buf.writeUtf("item");
            buf.writeInt(3);
            buf.writeInt(1);
            buf.writeBoolean(false);

            ClientQuestEntry entry = ClientQuestEntry.STREAM_CODEC.decode(buf);

            assertEquals(QuestType.BOOLEAN, entry.type());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object roundTrip(CodecCase testCase) {
        StreamCodec codec = testCase.codec();
        RegistryFriendlyByteBuf buf = NetworkTestSupport.registryBuffer();
        try {
            codec.encode(buf, testCase.value());
            Object decoded = codec.decode(buf);
            assertEquals(0, buf.readableBytes(), testCase.name() + " left unread bytes");
            testCase.equality().accept(testCase.value(), decoded);
            return decoded;
        } finally {
            buf.release();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void encode(StreamCodec<? super RegistryFriendlyByteBuf, ?> codec, Object value) {
        StreamCodec raw = codec;
        RegistryFriendlyByteBuf buf = NetworkTestSupport.registryBuffer();
        try {
            raw.encode(buf, value);
        } finally {
            buf.release();
        }
    }

    private static CodecCase passportCase(String name, SaveNPCPassportPayload value) {
        return new CodecCase(name, SaveNPCPassportPayload.CODEC, value, (expectedValue, decoded) -> {
            SaveNPCPassportPayload expected = (SaveNPCPassportPayload) expectedValue;
            SaveNPCPassportPayload actual = (SaveNPCPassportPayload) decoded;
            assertEquals(expected.npcUuid(), actual.npcUuid());
            assertEquals(expected.name(), actual.name());
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.speed(), actual.speed());
            assertEquals(expected.hp(), actual.hp());
            assertEquals(expected.holdPosition(), actual.holdPosition());
            assertEquals(expected.isVisible(), actual.isVisible());
            assertEquals(expected.isHoldPosEnabled(), actual.isHoldPosEnabled());
            assertEquals(expected.canTakeDamage(), actual.canTakeDamage());
            assertEquals(expected.useGravity(), actual.useGravity());
            assertEquals(expected.creativeTunerOnly(), actual.creativeTunerOnly());
            assertEquals(expected.lookLocked(), actual.lookLocked());
            assertEquals(expected.emote(), actual.emote());
        });
    }

    private static CodecCase skinCase(String name, SaveNPCSkinPayload value) {
        return new CodecCase(name, SaveNPCSkinPayload.CODEC, value, (expectedValue, decoded) -> {
            SaveNPCSkinPayload expected = (SaveNPCSkinPayload) expectedValue;
            SaveNPCSkinPayload actual = (SaveNPCSkinPayload) decoded;
            assertEquals(expected.npcUuid(), actual.npcUuid());
            assertEquals(expected.skinType(), actual.skinType());
            assertEquals(expected.skinValue(), actual.skinValue());
            assertEquals(expected.model(), actual.model());
            assertArrayEquals(expected.pngData(), actual.pngData());
        });
    }

    private static CodecCase questsCase(String name, SyncPlayerQuestsPayload value, int expectedSize) {
        return new CodecCase(name, SyncPlayerQuestsPayload.CODEC, value, (expected, decoded) -> {
            SyncPlayerQuestsPayload actual = (SyncPlayerQuestsPayload) decoded;
            assertNotNull(actual.quests());
            assertEquals(expectedSize, actual.quests().size());
            if (expectedSize > 0) {
                assertEquals(((SyncPlayerQuestsPayload) expected).quests(), actual.quests());
            }
        });
    }

    private static CodecCase dialogCase(String name, OpenDialogPayload value) {
        return new CodecCase(name, OpenDialogPayload.CODEC, value, (expected, decoded) -> {
            OpenDialogPayload input = (OpenDialogPayload) expected;
            OpenDialogPayload actual = (OpenDialogPayload) decoded;
            assertEquals(input.npcUuid(), actual.npcUuid());
            assertEquals(input.actionId() == null ? "" : input.actionId(), actual.actionId());
            assertEquals(input.npcName() == null ? "NPC" : input.npcName(), actual.npcName());
            assertEquals(input.npcMessage() == null ? "" : input.npcMessage().getString(),
                    actual.npcMessage().getString());
            Map<String, String> answers = input.answers() == null ? Map.of() : input.answers();
            assertEquals(answers, actual.answers());
        });
    }

    private static CodecCase settingCase(String name, OpenSettingGuiPayload value) {
        return new CodecCase(name, OpenSettingGuiPayload.CODEC, value, (expected, decoded) -> {
            OpenSettingGuiPayload input = (OpenSettingGuiPayload) expected;
            OpenSettingGuiPayload actual = (OpenSettingGuiPayload) decoded;
            assertEquals(input.isCreativeTuner(), actual.isCreativeTuner());
            assertEquals(input.entityId(), actual.entityId());
            assertEquals(input.name(), actual.name());
            assertEquals(input.size(), actual.size());
            assertEquals(input.speed(), actual.speed());
            assertEquals(input.hp(), actual.hp());
            assertEquals(input.holdPosition(), actual.holdPosition());
            assertEquals(input.isVisible(), actual.isVisible());
            assertEquals(input.isHoldPosEnabled(), actual.isHoldPosEnabled());
            assertEquals(input.canTakeDamage(), actual.canTakeDamage());
            assertEquals(input.useGravity(), actual.useGravity());
            assertEquals(input.creativeTunerOnly(), actual.creativeTunerOnly());
            assertEquals(input.lookLocked(), actual.lookLocked());
            assertEquals(input.jsonScript() == null ? "" : input.jsonScript(), actual.jsonScript());
            assertEquals(input.emote() == null ? "" : input.emote(), actual.emote());
        });
    }

    private static CodecCase animListCase(String name, AnimListPayload value, int expectedSize) {
        return new CodecCase(name, AnimListPayload.CODEC, value, (expected, decoded) -> {
            AnimListPayload actual = (AnimListPayload) decoded;
            assertNotNull(actual.animations());
            assertEquals(expectedSize, actual.animations().size());
            if (expectedSize > 0) {
                assertEquals(((AnimListPayload) expected).animations(), actual.animations());
            }
        });
    }

    private static CodecCase animDataCase(String name, AnimDataPayload value) {
        return new CodecCase(name, AnimDataPayload.CODEC, value, (expected, decoded) -> {
            AnimDataPayload actual = (AnimDataPayload) decoded;
            assertEquals(((AnimDataPayload) expected).name(), actual.name());
            assertArrayEquals(((AnimDataPayload) expected).jsonData(), actual.jsonData());
        });
    }

    private static CodecCase responseCase(String name, NpcLuaScriptResponsePayload value) {
        return new CodecCase(name, NpcLuaScriptResponsePayload.CODEC, value, (expected, decoded) -> {
            NpcLuaScriptResponsePayload input = (NpcLuaScriptResponsePayload) expected;
            NpcLuaScriptResponsePayload actual = (NpcLuaScriptResponsePayload) decoded;
            assertEquals(input.npcUuid(), actual.npcUuid());
            assertEquals(input.fileName() == null ? "main.lua" : input.fileName(), actual.fileName());
            assertEquals(input.source() == null ? "" : input.source(), actual.source());
            assertEquals(input.exists(), actual.exists());
            assertEquals(input.message() == null ? "" : input.message().getString(), actual.message().getString());
        });
    }

    private static CodecCase questEntryCase(String name, ClientQuestEntry value) {
        ClientQuestEntry input = value != null ? value : new ClientQuestEntry(
                null, null, null, false, null, null, 0, 0, false);
        return new CodecCase(name, ClientQuestEntry.STREAM_CODEC, input, (expected, decoded) -> {
            ClientQuestEntry actual = (ClientQuestEntry) decoded;
            assertEquals(input.questUuid() == null ? "" : input.questUuid(), actual.questUuid());
            assertEquals(input.title() == null ? "" : input.title(), actual.title());
            assertEquals(input.npcName() == null ? "" : input.npcName(), actual.npcName());
            assertEquals(input.visibleInGui(), actual.visibleInGui());
            assertEquals(input.type() == null ? QuestType.BOOLEAN : input.type(), actual.type());
            assertEquals(input.itemId() == null ? "" : input.itemId(), actual.itemId());
            assertEquals(input.targetProgress(), actual.targetProgress());
            assertEquals(input.currentProgress(), actual.currentProgress());
            assertEquals(input.completed(), actual.completed());
        });
    }

    private static CodecCase recordCase(String name, StreamCodec<? super RegistryFriendlyByteBuf, ?> codec, Object value) {
        return new CodecCase(name, codec, value, (expected, decoded) -> assertEquals(expected, decoded));
    }

    private static ClientQuestEntry questEntry(String uuid) {
        return new ClientQuestEntry(uuid, "Title", "Bob", true, QuestType.ITEM, "minecraft:stone", 5, 2, false);
    }

    private static Map<String, String> orderedAnswers(String... values) {
        Map<String, String> answers = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            answers.put(values[i], "next-" + i);
        }
        return answers;
    }
}
