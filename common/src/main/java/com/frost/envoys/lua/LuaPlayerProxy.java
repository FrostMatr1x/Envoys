package com.frost.envoys.lua;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.function.LibFunction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class LuaPlayerProxy extends LuaUserdata {

    private static final LuaTable METATABLE = createMetatable();

    private final UUID playerId;
    private final MinecraftServer server;

    public LuaPlayerProxy(UUID playerId, MinecraftServer server) {
        super(playerId, METATABLE);
        this.playerId = playerId;
        this.server = server;
    }

    public UUID playerId() {
        return playerId;
    }

    public ServerPlayer tryResolve() {
        if (server == null) {
            return null;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player != null && !player.isRemoved() ? player : null;
    }

    public ServerPlayer resolve() throws LuaError {
        ServerPlayer player = tryResolve();
        if (player == null) {
            throw new LuaError("player offline");
        }
        return player;
    }

    private LuaValue index(String key) throws LuaError {
        return switch (key) {
            case "name" -> {
                ServerPlayer player = tryResolve();
                yield player == null ? Constants.NIL : LuaStrings.toLua(player.getGameProfile().getName());
            }
            case "uuid" -> LuaStrings.toLua(playerId.toString());
            case "pos" -> {
                ServerPlayer player = tryResolve();
                if (player == null) {
                    yield Constants.NIL;
                }
                LuaTable pos = new LuaTable();
                pos.rawset("x", ValueFactory.valueOf(player.getX()));
                pos.rawset("y", ValueFactory.valueOf(player.getY()));
                pos.rawset("z", ValueFactory.valueOf(player.getZ()));
                yield pos;
            }
            case "sendMessage" -> LibFunction.create((LibFunction.TwoArg) (state, self, message) -> {
                ServerPlayer player = asProxy(self).tryResolve();
                if (player != null) {
                    player.sendSystemMessage(Component.literal(LuaStrings.toJava(message)));
                }
                return Constants.NIL;
            });
            case "distanceTo" -> LibFunction.create((LibFunction.TwoArg) (state, self, target) -> {
                ServerPlayer player = asProxy(self).tryResolve();
                if (player == null) {
                    return Constants.NIL;
                }
                double x;
                double y;
                double z;
                if (target instanceof LuaPlayerProxy other) {
                    ServerPlayer otherPlayer = other.tryResolve();
                    if (otherPlayer == null) {
                        return Constants.NIL;
                    }
                    x = otherPlayer.getX();
                    y = otherPlayer.getY();
                    z = otherPlayer.getZ();
                } else if (target instanceof LuaTable table) {
                    x = table.rawget("x").checkDouble();
                    y = table.rawget("y").checkDouble();
                    z = table.rawget("z").checkDouble();
                } else {
                    throw new LuaError("distanceTo expects a player or a position table");
                }
                double dx = player.getX() - x;
                double dy = player.getY() - y;
                double dz = player.getZ() - z;
                return ValueFactory.valueOf(Math.sqrt(dx * dx + dy * dy + dz * dz));
            });
            default -> Constants.NIL;
        };
    }

    private static LuaPlayerProxy asProxy(LuaValue value) throws LuaError {
        if (value instanceof LuaPlayerProxy proxy) {
            return proxy;
        }
        throw new LuaError("expected player");
    }

    private static LuaTable createMetatable() {
        LuaTable metatable = new LuaTable();
        metatable.rawset("__index", LibFunction.create((LibFunction.TwoArg) (LuaState state, LuaValue self, LuaValue key) ->
                asProxy(self).index(LuaStrings.toJava(key))));
        return metatable;
    }
}
