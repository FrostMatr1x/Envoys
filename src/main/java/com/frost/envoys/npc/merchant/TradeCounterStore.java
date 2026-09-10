package com.frost.envoys.npc.merchant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.frost.envoys.Envoys;
import com.frost.envoys.npc.NPCTrade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Серверное хранилище счётчиков сделок: на каждого игрока по каждому офферу NPC.
 * Ключ оффера — индекс в {@code ActionTrade.trades} (исходный порядок).
 * Персистентность: {@code FMLPaths.CONFIGDIR/envoys/npcs/<npcUuid>.trades.json}.
 */
public final class TradeCounterStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<UUID, Map<UUID, Map<Integer, TradeCounter>>> DATA = new HashMap<>();

    private TradeCounterStore() {
    }

    public record TradeCounter(int used, long lastTradeMillis) {
    }

    private static Path getNpcDir() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("envoys/npcs");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to create trade counter dir", e);
        }
        return dir;
    }

    private static Path getFile(UUID npcUuid) {
        return getNpcDir().resolve(npcUuid + ".trades.json");
    }

    public static int remainingUses(UUID npcUuid, UUID playerUuid, int offerIndex, NPCTrade trade) {
        if (trade == null || trade.maxTrades <= 0) {
            return Integer.MAX_VALUE;
        }
        int used = effectiveUsed(npcUuid, playerUuid, offerIndex, trade);
        return Math.max(0, trade.maxTrades - used);
    }

    public static void onTradeCompleted(UUID npcUuid, UUID playerUuid, int offerIndex, NPCTrade trade, int times) {
        if (trade == null || times <= 0) {
            return;
        }
        int used = effectiveUsed(npcUuid, playerUuid, offerIndex, trade) + times;
        long now = System.currentTimeMillis();
        DATA.computeIfAbsent(npcUuid, k -> new HashMap<>())
            .computeIfAbsent(playerUuid, k -> new HashMap<>())
            .put(offerIndex, new TradeCounter(used, now));
        save(npcUuid);
    }

    public static int getCompletedTrades(UUID npcUuid, UUID playerUuid, int offerIndex, NPCTrade trade) {
        return effectiveUsed(npcUuid, playerUuid, offerIndex, trade);
    }


    private static int effectiveUsed(UUID npcUuid, UUID playerUuid, int offerIndex, NPCTrade trade) {
        TradeCounter counter = getCounter(npcUuid, playerUuid, offerIndex);
        if (counter == null) {
            return 0;
        }
        int used = counter.used();
        if (trade.resetTime > 0 && counter.lastTradeMillis() > 0) {
            long elapsed = System.currentTimeMillis() - counter.lastTradeMillis();
            if (elapsed >= trade.resetTime * 1000L) {
                return 0;
            }
        }
        return used;
    }

    private static TradeCounter getCounter(UUID npcUuid, UUID playerUuid, int offerIndex) {
        Map<UUID, Map<Integer, TradeCounter>> players = DATA.get(npcUuid);
        if (players == null) {
            return null;
        }
        Map<Integer, TradeCounter> offers = players.get(playerUuid);
        if (offers == null) {
            return null;
        }
        return offers.get(offerIndex);
    }

    public static void saveAll() {
        for (UUID npcUuid : DATA.keySet()) {
            save(npcUuid);
        }
        Envoys.LOGGER.info("[Envoys] Saved trade counters for {} NPCs", DATA.size());
    }

    public static void save(UUID npcUuid) {
        Map<UUID, Map<Integer, TradeCounter>> players = DATA.get(npcUuid);
        if (players == null) {
            return;
        }
        CounterFile file = new CounterFile();
        for (Map.Entry<UUID, Map<Integer, TradeCounter>> pe : players.entrySet()) {
            PlayerEntry entry = new PlayerEntry();
            entry.player = pe.getKey().toString();
            for (Map.Entry<Integer, TradeCounter> oe : pe.getValue().entrySet()) {
                OfferEntry off = new OfferEntry();
                off.index = oe.getKey();
                off.used = oe.getValue().used();
                off.lastTradeMillis = oe.getValue().lastTradeMillis();
                entry.offers.add(off);
            }
            file.players.add(entry);
        }
        Path path = getFile(npcUuid);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(file, writer);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to save trade counters for {}", npcUuid, e);
        }
    }

    public static void loadAll() {
        DATA.clear();
        Path dir = getNpcDir();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".trades.json"))
                 .forEach(p -> {
                     String name = p.getFileName().toString().replace(".trades.json", "");
                     try {
                         load(UUID.fromString(name));
                     } catch (IllegalArgumentException e) {
                         Envoys.LOGGER.warn("[Envoys] Skipping invalid trade counter file: {}", p);
                     }
                 });
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to list trade counter dir", e);
        }
        Envoys.LOGGER.info("[Envoys] Loaded trade counters for {} NPCs", DATA.size());
    }

    private static void load(UUID npcUuid) {
        Path path = getFile(npcUuid);
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CounterFile file = GSON.fromJson(reader, CounterFile.class);
            if (file == null || file.players == null) {
                return;
            }
            Map<UUID, Map<Integer, TradeCounter>> players = new HashMap<>();
            for (PlayerEntry pe : file.players) {
                UUID playerUuid;
                try {
                    playerUuid = UUID.fromString(pe.player);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                Map<Integer, TradeCounter> offers = new HashMap<>();
                if (pe.offers != null) {
                    for (OfferEntry oe : pe.offers) {
                        offers.put(oe.index, new TradeCounter(oe.used, oe.lastTradeMillis));
                    }
                }
                players.put(playerUuid, offers);
            }
            DATA.put(npcUuid, players);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to load trade counters for {}", npcUuid, e);
        }
    }

    private static class CounterFile {
        public final List<PlayerEntry> players = new ArrayList<>();
    }

    private static class PlayerEntry {
        public String player;
        public final List<OfferEntry> offers = new ArrayList<>();
    }

    private static class OfferEntry {
        public int index;
        public int used;
        public long lastTradeMillis;
    }
}
