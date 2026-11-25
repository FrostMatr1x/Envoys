package com.frost.envoys.network;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class NetworkTestSupport {

    private static boolean bootstrapped;

    private NetworkTestSupport() {
    }

    public static synchronized void bootStrap() {
        if (bootstrapped) {
            return;
        }
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        bootstrapped = true;
    }

    public static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static RegistryFriendlyByteBuf registryBuffer() {
        bootStrap();
        RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
    }

    public static NetworkContext context(Player player) {
        NetworkContext context = mock(NetworkContext.class);
        when(context.player()).thenReturn(player);
        when(context.enqueueWork(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return CompletableFuture.completedFuture(null);
        });
        return context;
    }

    public static Player player() {
        bootStrap();
        return mock(Player.class);
    }

    public static ServerLevel level() {
        bootStrap();
        ServerLevel level = mock(ServerLevel.class);
        when(level.getEntity(any())).thenReturn(null);
        return level;
    }

    public static ServerLevel serverLevelWithNpc(UUID npcId, Entity npc) {
        bootStrap();
        ServerLevel level = mock(ServerLevel.class);
        when(level.getEntity(npcId)).thenReturn(npc);
        return level;
    }

    public static ServerPlayer serverPlayer(boolean op) {
        return serverPlayer(UUID.randomUUID(), op, level());
    }

    public static ServerPlayer serverPlayer(UUID uuid, boolean op) {
        return serverPlayer(uuid, op, level());
    }

    public static ServerPlayer serverPlayer(UUID uuid, boolean op, ServerLevel level) {
        bootStrap();
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.hasPermissions(anyInt())).thenReturn(op);
        when(player.getName()).thenReturn(Component.literal("Tester"));
        when(player.getUUID()).thenReturn(uuid);
        when(player.serverLevel()).thenReturn(level);
        when(player.distanceToSqr(any(Entity.class))).thenReturn(0.0D);
        return player;
    }

    public static MinecraftServer serverWithLevels(ServerLevel... levels) {
        bootStrap();
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getAllLevels()).thenReturn(List.of(levels));
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(server).execute(any(Runnable.class));
        return server;
    }

    public static void setServerField(ServerPlayer player, MinecraftServer server) {
        Class<?> type = player.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("server");
                field.setAccessible(true);
                field.set(player, server);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to set ServerPlayer.server", e);
            }
        }
        throw new IllegalStateException("ServerPlayer.server field not found");
    }

    public static final class RecordingNetworkManager implements NetworkManager {

        private final List<CustomPacketPayload> toServerList = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final List<CustomPacketPayload> toPlayerList = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final List<CustomPacketPayload> toAllList = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final BlockingQueue<CustomPacketPayload> playerQueue = new LinkedBlockingQueue<>();

        @Override
        public void sendToServer(CustomPacketPayload payload) {
            toServerList.add(payload);
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
            toPlayerList.add(payload);
            playerQueue.add(payload);
        }

        @Override
        public void sendToAllPlayers(CustomPacketPayload payload) {
            toAllList.add(payload);
        }

        public List<CustomPacketPayload> toServer() {
            return toServerList;
        }

        public List<CustomPacketPayload> toPlayer() {
            return toPlayerList;
        }

        public List<CustomPacketPayload> toAll() {
            return toAllList;
        }

        public CustomPacketPayload awaitPlayer() {
            try {
                CustomPacketPayload payload = playerQueue.poll(5, TimeUnit.SECONDS);
                if (payload == null) {
                    fail("Timed out waiting for a payload sent to the player");
                }
                return payload;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
