package com.frost.envoys.action.serialization;

import com.frost.envoys.action.NPCPassportData;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcClickEvent;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.event.NpcKickEvent;
import com.frost.envoys.action.event.NpcRangeEvent;
import com.frost.envoys.action.event.NpcUpdateEvent;
import com.frost.envoys.action.model.EntityActionData;
import com.frost.envoys.Envoys;
import com.frost.envoys.quest.QuestDefinition;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NPCScriptDataAdapter implements JsonSerializer<NPCScriptData>, JsonDeserializer<NPCScriptData> {

    private static final Gson SUB_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(EntityActionData.class, new EntityActionAdapter())
            .registerTypeHierarchyAdapter(net.minecraft.world.item.ItemStack.class, new ItemStackAdapter().nullSafe())
            .registerTypeHierarchyAdapter(net.minecraft.network.chat.Component.class, new ComponentAdapter().nullSafe())
            .create();

    @Override
    public JsonElement serialize(NPCScriptData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject root = new JsonObject();
        if (src.npcUUID != null) {
            root.addProperty("npcUUID", src.npcUUID);
        }
        if (src.passport != null) {
            root.add("passport", SUB_GSON.toJsonTree(src.passport));
        }
        JsonObject events = new JsonObject();
        if (src.events != null) {
            for (Map.Entry<String, NpcEventData> entry : src.events.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                events.add(entry.getKey(), SUB_GSON.toJsonTree(entry.getValue()));
            }
        }
        root.add("events", events);
        root.add("quests", SUB_GSON.toJsonTree(src.quests != null ? src.quests : List.of()));
        return root;
    }

    @Override
    public NPCScriptData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        NPCScriptData data = new NPCScriptData();
        if (json == null || !json.isJsonObject()) {
            return data;
        }

        JsonObject root = json.getAsJsonObject();
        if (root.has("npcUUID") && !root.get("npcUUID").isJsonNull()) {
            data.npcUUID = root.get("npcUUID").getAsString();
        }
        if (root.has("passport") && root.get("passport").isJsonObject()) {
            NPCPassportData passport = SUB_GSON.fromJson(root.get("passport"), NPCPassportData.class);
            if (passport != null) {
                data.passport = passport;
            }
        }

        if (root.has("quests") && root.get("quests").isJsonArray()) {
            List<QuestDefinition> quests = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("quests")) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                try {
                    QuestDefinition quest = SUB_GSON.fromJson(element, QuestDefinition.class);
                    if (quest != null) {
                        quests.add(quest);
                    }
                } catch (RuntimeException e) {
                    Envoys.LOGGER.warn("[Envoys] Skipping unreadable quest entry in NPC script: {}", e.getMessage());
                }
            }
            data.quests = quests;
        }

        JsonObject eventsObj = root.has("events") && root.get("events").isJsonObject()
                ? root.getAsJsonObject("events")
                : null;
        if (eventsObj != null) {
            for (Map.Entry<String, JsonElement> entry : eventsObj.entrySet()) {
                EventType type = EventType.fromKey(entry.getKey());
                if (type == null || entry.getValue() == null || !entry.getValue().isJsonObject()) {
                    continue;
                }
                NpcEventData event = switch (type) {
                    case UPDATE -> SUB_GSON.fromJson(entry.getValue(), NpcUpdateEvent.class);
                    case CLICK -> SUB_GSON.fromJson(entry.getValue(), NpcClickEvent.class);
                    case KICK -> SUB_GSON.fromJson(entry.getValue(), NpcKickEvent.class);
                    case RANGE -> SUB_GSON.fromJson(entry.getValue(), NpcRangeEvent.class);
                };
                if (event != null) {
                    data.events.put(type.jsonKey(), event);
                }
            }
        }

        data.materialize();
        return data;
    }
}
