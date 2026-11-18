package com.frost.envoys.neoforge;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.network.NetworkContext;
import com.frost.envoys.network.ServerAnimPayloadHandler;
import com.frost.envoys.network.ServerPayloadHandler;
import com.frost.envoys.network.ServerSkinPayloadHandler;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.network.payload.AnimOpResultPayload;
import com.frost.envoys.network.payload.AnimRemovedPayload;
import com.frost.envoys.network.payload.DeleteAnimPayload;
import com.frost.envoys.network.payload.FetchNpcLuaScriptPayload;
import com.frost.envoys.network.payload.LuaScriptUploadResultPayload;
import com.frost.envoys.network.payload.NpcLuaScriptResponsePayload;
import com.frost.envoys.network.payload.OpenDialogPayload;
import com.frost.envoys.network.payload.OpenSettingGuiPayload;
import com.frost.envoys.network.payload.PushAnimPayload;
import com.frost.envoys.network.payload.RequestAnimListPayload;
import com.frost.envoys.network.payload.RequestAnimPayload;
import com.frost.envoys.network.payload.RequestSkinPayload;
import com.frost.envoys.network.payload.SaveNpcLuaScriptPayload;
import com.frost.envoys.network.payload.SaveNPCPassportPayload;
import com.frost.envoys.network.payload.SaveNPCScriptPayload;
import com.frost.envoys.network.payload.SaveNPCSkinPayload;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.network.payload.SkinConfirmedPayload;
import com.frost.envoys.network.payload.SkinDataPayload;
import com.frost.envoys.network.payload.SkinInfoPayload;
import com.frost.envoys.network.payload.SyncPlayerQuestsPayload;
import com.frost.envoys.network.payload.TradeAllPayload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeNetworkEvents {
    private NeoForgeNetworkEvents() {
    }

    @FunctionalInterface
    private interface Handler<T extends CustomPacketPayload> {
        void handle(T payload, NetworkContext context);
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> server(Handler<T> handler) {
        return (payload, context) -> handler.handle(payload, new NeoForgeNetworkContext(context));
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> clientDispatch() {
        return (payload, context) -> {
            if (FMLEnvironment.dist.isClient()) {
                NeoForgeClientPayloadDispatcher.dispatch(payload, new NeoForgeNetworkContext(context));
            }
        };
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(EnvoysConstants.MODID);

        registrar.playToServer(SaveNPCScriptPayload.TYPE, SaveNPCScriptPayload.CODEC,
                server(ServerPayloadHandler::handleSaveNPCScript));
        registrar.playToServer(SaveNpcLuaScriptPayload.TYPE, SaveNpcLuaScriptPayload.CODEC,
                server(ServerPayloadHandler::handleSaveNpcLuaScript));
        registrar.playToServer(FetchNpcLuaScriptPayload.TYPE, FetchNpcLuaScriptPayload.CODEC,
                server(ServerPayloadHandler::handleFetchNpcLuaScript));
        registrar.playToServer(SelectDialogAnswerPayload.TYPE, SelectDialogAnswerPayload.CODEC,
                server(ServerPayloadHandler::handleSelectDialogAnswer));
        registrar.playToServer(SaveNPCPassportPayload.TYPE, SaveNPCPassportPayload.CODEC,
                server(ServerPayloadHandler::handleSaveNPCPassport));
        registrar.playToServer(SaveNPCSkinPayload.TYPE, SaveNPCSkinPayload.CODEC,
                server(ServerSkinPayloadHandler::handleSaveNPCSkin));
        registrar.playToServer(RequestSkinPayload.TYPE, RequestSkinPayload.CODEC,
                server(ServerSkinPayloadHandler::handleRequestSkin));
        registrar.playToServer(TradeAllPayload.TYPE, TradeAllPayload.CODEC,
                server(ServerPayloadHandler::handleTradeAll));
        registrar.playToServer(RequestAnimListPayload.TYPE, RequestAnimListPayload.CODEC,
                server(ServerAnimPayloadHandler::handleRequestAnimList));
        registrar.playToServer(RequestAnimPayload.TYPE, RequestAnimPayload.CODEC,
                server(ServerAnimPayloadHandler::handleRequestAnim));
        registrar.playToServer(PushAnimPayload.TYPE, PushAnimPayload.CODEC,
                server(ServerAnimPayloadHandler::handlePushAnim));
        registrar.playToServer(DeleteAnimPayload.TYPE, DeleteAnimPayload.CODEC,
                server(ServerAnimPayloadHandler::handleDeleteAnim));

        registrar.playToClient(OpenSettingGuiPayload.TYPE, OpenSettingGuiPayload.CODEC, clientDispatch());
        registrar.playToClient(OpenDialogPayload.TYPE, OpenDialogPayload.CODEC, clientDispatch());
        registrar.playToClient(LuaScriptUploadResultPayload.TYPE, LuaScriptUploadResultPayload.CODEC, clientDispatch());
        registrar.playToClient(NpcLuaScriptResponsePayload.TYPE, NpcLuaScriptResponsePayload.CODEC, clientDispatch());
        registrar.playToClient(SkinInfoPayload.TYPE, SkinInfoPayload.CODEC, clientDispatch());
        registrar.playToClient(SkinDataPayload.TYPE, SkinDataPayload.CODEC, clientDispatch());
        registrar.playToClient(SkinConfirmedPayload.TYPE, SkinConfirmedPayload.CODEC, clientDispatch());
        registrar.playToClient(AnimListPayload.TYPE, AnimListPayload.CODEC, clientDispatch());
        registrar.playToClient(AnimDataPayload.TYPE, AnimDataPayload.CODEC, clientDispatch());
        registrar.playToClient(AnimOpResultPayload.TYPE, AnimOpResultPayload.CODEC, clientDispatch());
        registrar.playToClient(AnimRemovedPayload.TYPE, AnimRemovedPayload.CODEC, clientDispatch());
        registrar.playToClient(SyncPlayerQuestsPayload.TYPE, SyncPlayerQuestsPayload.CODEC, clientDispatch());
    }
}
