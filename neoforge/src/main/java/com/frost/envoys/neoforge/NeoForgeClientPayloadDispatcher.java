package com.frost.envoys.neoforge;

import com.frost.envoys.network.ClientAnimPayloadHandler;
import com.frost.envoys.network.ClientPayloadHandler;
import com.frost.envoys.network.ClientSkinPayloadHandler;
import com.frost.envoys.network.NetworkContext;
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

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class NeoForgeClientPayloadDispatcher {
    private NeoForgeClientPayloadDispatcher() {
    }

    public static void dispatch(CustomPacketPayload payload, NetworkContext context) {
        if (payload instanceof OpenSettingGuiPayload p) {
            ClientPayloadHandler.handleOpenSettingGui(p, context);
        } else if (payload instanceof OpenDialogPayload p) {
            ClientPayloadHandler.handleOpenDialog(p, context);
        } else if (payload instanceof LuaScriptUploadResultPayload p) {
            ClientPayloadHandler.handleLuaScriptUploadResult(p, context);
        } else if (payload instanceof NpcLuaScriptResponsePayload p) {
            ClientPayloadHandler.handleNpcLuaScriptResponse(p, context);
        } else if (payload instanceof SkinInfoPayload p) {
            ClientSkinPayloadHandler.handleSkinInfo(p, context);
        } else if (payload instanceof SkinDataPayload p) {
            ClientSkinPayloadHandler.handleSkinData(p, context);
        } else if (payload instanceof SkinConfirmedPayload p) {
            ClientSkinPayloadHandler.handleSkinConfirmed(p, context);
        } else if (payload instanceof AnimListPayload p) {
            ClientAnimPayloadHandler.handleAnimList(p, context);
        } else if (payload instanceof AnimDataPayload p) {
            ClientAnimPayloadHandler.handleAnimData(p, context);
        } else if (payload instanceof AnimOpResultPayload p) {
            ClientAnimPayloadHandler.handleAnimOpResult(p, context);
        } else if (payload instanceof AnimRemovedPayload p) {
            ClientAnimPayloadHandler.handleAnimRemoved(p, context);
        } else if (payload instanceof SyncPlayerQuestsPayload p) {
            ClientPayloadHandler.handleSyncPlayerQuests(p, context);
        }
    }
}
