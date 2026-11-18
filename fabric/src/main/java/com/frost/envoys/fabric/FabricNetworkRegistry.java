package com.frost.envoys.fabric;

import com.frost.envoys.network.ServerAnimPayloadHandler;
import com.frost.envoys.network.ServerPayloadHandler;
import com.frost.envoys.network.ServerSkinPayloadHandler;
import com.frost.envoys.network.payload.DeleteAnimPayload;
import com.frost.envoys.network.payload.FetchNpcLuaScriptPayload;
import com.frost.envoys.network.payload.PushAnimPayload;
import com.frost.envoys.network.payload.RequestAnimListPayload;
import com.frost.envoys.network.payload.RequestAnimPayload;
import com.frost.envoys.network.payload.RequestSkinPayload;
import com.frost.envoys.network.payload.SaveNpcLuaScriptPayload;
import com.frost.envoys.network.payload.SaveNPCPassportPayload;
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
import com.frost.envoys.network.payload.SaveNPCScriptPayload;
import com.frost.envoys.network.payload.SaveNPCSkinPayload;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.network.payload.TradeAllPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FabricNetworkRegistry {
    private FabricNetworkRegistry() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(SaveNPCScriptPayload.TYPE, SaveNPCScriptPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveNpcLuaScriptPayload.TYPE, SaveNpcLuaScriptPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FetchNpcLuaScriptPayload.TYPE, FetchNpcLuaScriptPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectDialogAnswerPayload.TYPE, SelectDialogAnswerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveNPCPassportPayload.TYPE, SaveNPCPassportPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveNPCSkinPayload.TYPE, SaveNPCSkinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestSkinPayload.TYPE, RequestSkinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TradeAllPayload.TYPE, TradeAllPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestAnimListPayload.TYPE, RequestAnimListPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestAnimPayload.TYPE, RequestAnimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PushAnimPayload.TYPE, PushAnimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteAnimPayload.TYPE, DeleteAnimPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(OpenSettingGuiPayload.TYPE, OpenSettingGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenDialogPayload.TYPE, OpenDialogPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LuaScriptUploadResultPayload.TYPE, LuaScriptUploadResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NpcLuaScriptResponsePayload.TYPE, NpcLuaScriptResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkinInfoPayload.TYPE, SkinInfoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkinDataPayload.TYPE, SkinDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkinConfirmedPayload.TYPE, SkinConfirmedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimListPayload.TYPE, AnimListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimDataPayload.TYPE, AnimDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimOpResultPayload.TYPE, AnimOpResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimRemovedPayload.TYPE, AnimRemovedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPlayerQuestsPayload.TYPE, SyncPlayerQuestsPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SaveNPCScriptPayload.TYPE,
                (payload, context) -> ServerPayloadHandler.handleSaveNPCScript(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(SaveNpcLuaScriptPayload.TYPE,
                (payload, context) -> ServerPayloadHandler.handleSaveNpcLuaScript(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(FetchNpcLuaScriptPayload.TYPE,
                (payload, context) -> ServerPayloadHandler.handleFetchNpcLuaScript(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(SelectDialogAnswerPayload.TYPE,
                (payload, context) -> ServerPayloadHandler.handleSelectDialogAnswer(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(SaveNPCPassportPayload.TYPE,
                (payload, context) -> ServerPayloadHandler.handleSaveNPCPassport(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(SaveNPCSkinPayload.TYPE,
                (payload, context) -> ServerSkinPayloadHandler.handleSaveNPCSkin(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(RequestSkinPayload.TYPE,
                (payload, context) -> ServerSkinPayloadHandler.handleRequestSkin(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(TradeAllPayload.TYPE,
                (payload, context) -> ServerPayloadHandler.handleTradeAll(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(RequestAnimListPayload.TYPE,
                (payload, context) -> ServerAnimPayloadHandler.handleRequestAnimList(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(RequestAnimPayload.TYPE,
                (payload, context) -> ServerAnimPayloadHandler.handleRequestAnim(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(PushAnimPayload.TYPE,
                (payload, context) -> ServerAnimPayloadHandler.handlePushAnim(payload, new FabricServerNetworkContext(context)));
        ServerPlayNetworking.registerGlobalReceiver(DeleteAnimPayload.TYPE,
                (payload, context) -> ServerAnimPayloadHandler.handleDeleteAnim(payload, new FabricServerNetworkContext(context)));
    }
}
