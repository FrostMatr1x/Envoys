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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class LuaPlayerProxy extends LuaUserdata {

    private static final LuaTable METATABLE = createMetatable();

    private final UUID playerId;
    private final ServerLevel level;

    public LuaPlayerProxy(UUID playerId, ServerLevel level) {
        super(playerId, METATABLE);
        this.playerId = playerId;
        this.level = level;
    }

    public UUID playerId() {
        return playerId;
    }

    public ServerPlayer resolve() throws LuaError {
        if (level != null && level.getPlayerByUUID(playerId) instanceof ServerPlayer player && !player.isRemoved()) {
            return player;
        }
        throw new LuaError("player offline");
    }

    private LuaValue index(String key) throws LuaError {
        return switch (key) {
            case "name" -> ValueFactory.valueOf(resolve().getGameProfile().getName());
            case "uuid" -> ValueFactory.valueOf(playerId.toString());
            case "pos" -> {
                ServerPlayer player = resolve();
                LuaTable pos = new LuaTable();
                pos.rawset("x", ValueFactory.valueOf(player.getX()));
                pos.rawset("y", ValueFactory.valueOf(player.getY()));
                pos.rawset("z", ValueFactory.valueOf(player.getZ()));
                yield pos;
            }
            case "sendMessage" -> LibFunction.create((LibFunction.TwoArg) (state, self, message) -> {
                asProxy(self).resolve().sendSystemMessage(Component.literal(message.checkString()));
                return Constants.NIL;
            });
            case "distanceTo" -> LibFunction.create((LibFunction.TwoArg) (state, self, target) -> {
                ServerPlayer player = asProxy(self).resolve();
                double x;
                double y;
                double z;
                if (target instanceof LuaPlayerProxy other) {
                    ServerPlayer otherPlayer = other.resolve();
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
                asProxy(self).index(key.checkString())));
        return metatable;
    }
}
