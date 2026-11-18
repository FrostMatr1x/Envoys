package com.frost.envoys.fabric;

import com.frost.envoys.network.ClientAnimPayloadHandler;
import com.frost.envoys.network.ClientPayloadHandler;
import com.frost.envoys.network.ClientSkinPayloadHandler;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.network.payload.AnimOpResultPayload;
import com.frost.envoys.network.payload.AnimRemovedPayload;
import com.frost.envoys.network.payload.LuaScriptUploadResultPayload;
import com.frost.envoys.network.payload.NpcLuaScriptResponsePayload;
import com.frost.envoys.network.payload.OpenDialogPayload;
import com.frost.envoys.network.payload.OpenSettingGuiPayload;
import com.frost.envoys.network.payload.SkinConfirmedPayload;
import com.frost.envoys.network.payload.SkinDataPayload;
import com.frost.envoys.network.payload.SkinInfoPayload;
import com.frost.envoys.network.payload.SyncPlayerQuestsPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricClientNetworkRegistry {
    private FabricClientNetworkRegistry() {
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(OpenSettingGuiPayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleOpenSettingGui(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(OpenDialogPayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleOpenDialog(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(LuaScriptUploadResultPayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleLuaScriptUploadResult(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(NpcLuaScriptResponsePayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleNpcLuaScriptResponse(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(SkinInfoPayload.TYPE,
                (payload, context) -> ClientSkinPayloadHandler.handleSkinInfo(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(SkinDataPayload.TYPE,
                (payload, context) -> ClientSkinPayloadHandler.handleSkinData(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(SkinConfirmedPayload.TYPE,
                (payload, context) -> ClientSkinPayloadHandler.handleSkinConfirmed(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(AnimListPayload.TYPE,
                (payload, context) -> ClientAnimPayloadHandler.handleAnimList(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(AnimDataPayload.TYPE,
                (payload, context) -> ClientAnimPayloadHandler.handleAnimData(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(AnimOpResultPayload.TYPE,
                (payload, context) -> ClientAnimPayloadHandler.handleAnimOpResult(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(AnimRemovedPayload.TYPE,
                (payload, context) -> ClientAnimPayloadHandler.handleAnimRemoved(payload, new FabricClientNetworkContext(context)));
        ClientPlayNetworking.registerGlobalReceiver(SyncPlayerQuestsPayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleSyncPlayerQuests(payload, new FabricClientNetworkContext(context)));
    }
}
