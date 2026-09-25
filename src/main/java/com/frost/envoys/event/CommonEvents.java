package com.frost.envoys.event;

import com.frost.envoys.Envoys;
import com.frost.envoys.network.ClientAnimPayloadHandler;
import com.frost.envoys.network.ClientPayloadHandler;
import com.frost.envoys.network.ClientSkinPayloadHandler;
import com.frost.envoys.network.ServerAnimPayloadHandler;
import com.frost.envoys.network.ServerPayloadHandler;
import com.frost.envoys.network.ServerSkinPayloadHandler;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.network.payload.FetchNpcLuaScriptPayload;
import com.frost.envoys.network.payload.LuaScriptUploadResultPayload;
import com.frost.envoys.network.payload.NpcLuaScriptResponsePayload;
import com.frost.envoys.network.payload.OpenDialogPayload;
import com.frost.envoys.network.payload.OpenSettingGuiPayload;
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

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Envoys.MODID)
public class CommonEvents {
    
    @SubscribeEvent
    public static void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Envoys.MODID);
        
        registrar.playToClient(
            OpenSettingGuiPayload.TYPE,
            OpenSettingGuiPayload.CODEC,
            FMLEnvironment.dist.isClient() ? ClientPayloadHandler::handleOpenSettingGui : (payload, context) -> {}
        );

        registrar.playToClient(
            OpenDialogPayload.TYPE,
            OpenDialogPayload.CODEC,
            FMLEnvironment.dist.isClient() ? ClientPayloadHandler::handleOpenDialog : (payload, context) -> {}
        );

        registrar.playToServer(
            SaveNPCScriptPayload.TYPE,
            SaveNPCScriptPayload.CODEC,
            ServerPayloadHandler::handleSaveNPCScript
        );

        registrar.playToServer(
            SaveNpcLuaScriptPayload.TYPE,
            SaveNpcLuaScriptPayload.CODEC,
            ServerPayloadHandler::handleSaveNpcLuaScript
        );

        registrar.playToClient(
            LuaScriptUploadResultPayload.TYPE,
            LuaScriptUploadResultPayload.CODEC,
            FMLEnvironment.dist.isClient() ? ClientPayloadHandler::handleLuaScriptUploadResult : (payload, context) -> {}
        );

        registrar.playToServer(
            FetchNpcLuaScriptPayload.TYPE,
            FetchNpcLuaScriptPayload.CODEC,
            ServerPayloadHandler::handleFetchNpcLuaScript
        );

        registrar.playToClient(
            NpcLuaScriptResponsePayload.TYPE,
            NpcLuaScriptResponsePayload.CODEC,
            FMLEnvironment.dist.isClient() ? ClientPayloadHandler::handleNpcLuaScriptResponse : (payload, context) -> {}
        );

        registrar.playToServer(
            SelectDialogAnswerPayload.TYPE,
            SelectDialogAnswerPayload.CODEC,
            ServerPayloadHandler::handleSelectDialogAnswer
        );

        registrar.playToServer(
            SaveNPCPassportPayload.TYPE,
            SaveNPCPassportPayload.CODEC,
            ServerPayloadHandler::handleSaveNPCPassport
        );
        
        registrar.playToServer(
                SaveNPCSkinPayload.TYPE,
                SaveNPCSkinPayload.CODEC,
                ServerSkinPayloadHandler::handleSaveNPCSkin
        );

        registrar.playToServer(
                RequestSkinPayload.TYPE,
                RequestSkinPayload.CODEC,
                ServerSkinPayloadHandler::handleRequestSkin
        );
        
        registrar.playToClient(
                SkinInfoPayload.TYPE,
                SkinInfoPayload.CODEC,
                ClientSkinPayloadHandler::handleSkinInfo
        );

        registrar.playToClient(
                SkinDataPayload.TYPE,
                SkinDataPayload.CODEC,
                ClientSkinPayloadHandler::handleSkinData
        );

        registrar.playToClient(
                SkinConfirmedPayload.TYPE,
                SkinConfirmedPayload.CODEC,
                ClientSkinPayloadHandler::handleSkinConfirmed
        );

        registrar.playToServer(
                TradeAllPayload.TYPE,
                TradeAllPayload.CODEC,
                ServerPayloadHandler::handleTradeAll
        );

        registrar.playToServer(
                RequestAnimListPayload.TYPE,
                RequestAnimListPayload.CODEC,
                ServerAnimPayloadHandler::handleRequestAnimList
        );

        registrar.playToServer(
                RequestAnimPayload.TYPE,
                RequestAnimPayload.CODEC,
                ServerAnimPayloadHandler::handleRequestAnim
        );

        registrar.playToClient(
                AnimListPayload.TYPE,
                AnimListPayload.CODEC,
                FMLEnvironment.dist.isClient() ? ClientAnimPayloadHandler::handleAnimList : (payload, context) -> {}
        );

        registrar.playToClient(
                AnimDataPayload.TYPE,
                AnimDataPayload.CODEC,
                FMLEnvironment.dist.isClient() ? ClientAnimPayloadHandler::handleAnimData : (payload, context) -> {}
        );

        registrar.playToClient(
                SyncPlayerQuestsPayload.TYPE,
                SyncPlayerQuestsPayload.CODEC,
                FMLEnvironment.dist.isClient() ? ClientPayloadHandler::handleSyncPlayerQuests : (payload, context) -> {}
        );
    }
}