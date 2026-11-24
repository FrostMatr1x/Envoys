package com.frost.envoys.network.payload;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadTypeTest {

    private static final UUID NPC_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private record PayloadCase(CustomPacketPayload payload, CustomPacketPayload.Type<?> type) {
    }

    @Test
    void everyPayloadExposesItsOwnEnvoysType() {
        for (PayloadCase testCase : allPayloads()) {
            String simpleName = testCase.payload().getClass().getSimpleName();
            assertSame(testCase.type(), testCase.payload().type(), simpleName);
            ResourceLocation id = testCase.payload().type().id();
            assertEquals("envoys", id.getNamespace(), simpleName);
            assertFalse(id.getPath().isBlank(), simpleName);
        }
    }

    @Test
    void payloadTypeIdsAreUnique() {
        Set<ResourceLocation> seen = new HashSet<>();
        for (PayloadCase testCase : allPayloads()) {
            assertTrue(seen.add(testCase.payload().type().id()),
                    "duplicate id " + testCase.payload().type().id());
        }
        assertEquals(allPayloads().size(), seen.size());
    }

    private static <T extends CustomPacketPayload> PayloadCase of(T payload, CustomPacketPayload.Type<T> type) {
        return new PayloadCase(payload, type);
    }

    private static List<PayloadCase> allPayloads() {
        List<PayloadCase> payloads = new ArrayList<>();
        payloads.add(of(new AnimDataPayload("walk", new byte[0]), AnimDataPayload.TYPE));
        payloads.add(of(new AnimListPayload(List.of()), AnimListPayload.TYPE));
        payloads.add(of(new AnimOpResultPayload(false, "push", "walk", "message"), AnimOpResultPayload.TYPE));
        payloads.add(of(new AnimRemovedPayload("walk"), AnimRemovedPayload.TYPE));
        payloads.add(of(new DeleteAnimPayload("walk"), DeleteAnimPayload.TYPE));
        payloads.add(of(new FetchNpcLuaScriptPayload(NPC_ID), FetchNpcLuaScriptPayload.TYPE));
        payloads.add(of(new LuaScriptUploadResultPayload(false, Component.empty(), "", ""),
                LuaScriptUploadResultPayload.TYPE));
        payloads.add(of(new NPCInteractPayload(NPC_ID), NPCInteractPayload.TYPE));
        payloads.add(of(new NpcLuaScriptResponsePayload(NPC_ID, "main.lua", "", false, Component.empty()),
                NpcLuaScriptResponsePayload.TYPE));
        payloads.add(of(new OpenDialogPayload(NPC_ID, "", "", Component.empty(), Map.of()), OpenDialogPayload.TYPE));
        payloads.add(of(new OpenSettingGuiPayload(false, NPC_ID, "Bob", 1f, 1f, 1f, Vec3.ZERO,
                true, true, true, true, true, true, "", ""), OpenSettingGuiPayload.TYPE));
        payloads.add(of(new PushAnimPayload("walk", new byte[0], "hash"), PushAnimPayload.TYPE));
        payloads.add(of(new RequestAnimListPayload(), RequestAnimListPayload.TYPE));
        payloads.add(of(new RequestAnimPayload("walk"), RequestAnimPayload.TYPE));
        payloads.add(of(new RequestSkinPayload(NPC_ID), RequestSkinPayload.TYPE));
        payloads.add(of(new SaveNpcLuaScriptPayload(NPC_ID, "main.lua", "", false), SaveNpcLuaScriptPayload.TYPE));
        payloads.add(of(new SaveNPCPassportPayload(NPC_ID, "Bob", 1f, 1f, 1f, Vec3.ZERO,
                true, true, true, true, true, true, ""), SaveNPCPassportPayload.TYPE));
        payloads.add(of(new SaveNPCScriptPayload(NPC_ID, "{}"), SaveNPCScriptPayload.TYPE));
        payloads.add(of(new SaveNPCSkinPayload(NPC_ID, "NICKNAME", "Steve", "classic", new byte[0]),
                SaveNPCSkinPayload.TYPE));
        payloads.add(of(new SelectDialogAnswerPayload(NPC_ID, "next"), SelectDialogAnswerPayload.TYPE));
        payloads.add(of(new SkinConfirmedPayload(NPC_ID, "hash"), SkinConfirmedPayload.TYPE));
        payloads.add(of(new SkinDataPayload(NPC_ID, new byte[0]), SkinDataPayload.TYPE));
        payloads.add(of(new SkinInfoPayload(NPC_ID, "", "hash", "classic"), SkinInfoPayload.TYPE));
        payloads.add(of(new SyncPlayerQuestsPayload(List.of()), SyncPlayerQuestsPayload.TYPE));
        payloads.add(of(new TradeAllPayload(0), TradeAllPayload.TYPE));
        return payloads;
    }
}
