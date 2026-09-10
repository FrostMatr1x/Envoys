package com.frost.envoys.action.serialization;

import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.model.ActionChat;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionDelay;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionQuestCheck;
import com.frost.envoys.action.model.ActionQuestGive;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.action.model.EntityActionData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class EntityActionAdapter implements JsonSerializer<EntityActionData>, JsonDeserializer<EntityActionData> {

    public static final Gson GSON = createGson();
    private static final Gson SUB_GSON = createSubGson();

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeHierarchyAdapter(EntityActionData.class, new EntityActionAdapter())
                .registerTypeAdapter(NPCScriptData.class, new NPCScriptDataAdapter())
                .registerTypeHierarchyAdapter(net.minecraft.world.item.ItemStack.class, new ItemStackAdapter().nullSafe())
                .registerTypeHierarchyAdapter(net.minecraft.network.chat.Component.class, new ComponentAdapter().nullSafe())
                .setPrettyPrinting()
                .create();
    }

    private static Gson createSubGson() {
        return new GsonBuilder()
                .registerTypeHierarchyAdapter(net.minecraft.world.item.ItemStack.class, new ItemStackAdapter().nullSafe())
                .registerTypeHierarchyAdapter(net.minecraft.network.chat.Component.class, new ComponentAdapter().nullSafe())
                .create();
    }

    @Override
    public JsonElement serialize(EntityActionData src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) return com.google.gson.JsonNull.INSTANCE;
        JsonObject json = SUB_GSON.toJsonTree(src).getAsJsonObject();
        json.addProperty("type", src.getType());
        return json;
    }

    @Override
    public EntityActionData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (!json.isJsonObject()) {
            throw new JsonParseException("[Envoys] Expected JsonObject for EntityActionData, but got: " + json);
        }

        JsonObject obj = json.getAsJsonObject();
        JsonElement typeElem = obj.get("type");
        if (typeElem == null || typeElem.isJsonNull()) {
            throw new JsonParseException("[Envoys] Action data missing 'type' field in: " + obj);
        }

        String type = typeElem.getAsString();

        return switch (type) {
            case "dialog" -> SUB_GSON.fromJson(obj, ActionDialog.class);
            case "trade" -> SUB_GSON.fromJson(obj, ActionTrade.class);
            case "command" -> SUB_GSON.fromJson(obj, ActionCommand.class);
            case "quest_give" -> SUB_GSON.fromJson(obj, ActionQuestGive.class);
            case "quest_check" -> SUB_GSON.fromJson(obj, ActionQuestCheck.class);
            case "move" -> SUB_GSON.fromJson(obj, ActionMove.class);
            case "delay" -> SUB_GSON.fromJson(obj, ActionDelay.class);
            case "chat" -> SUB_GSON.fromJson(obj, ActionChat.class);
            default -> throw new JsonParseException("[Envoys] Unknown EntityAction type: " + type);
        };
    }
}