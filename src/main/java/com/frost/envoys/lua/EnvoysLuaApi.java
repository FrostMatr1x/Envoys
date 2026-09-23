package com.frost.envoys.lua;

import com.frost.envoys.action.event.EventType;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;

public final class EnvoysLuaApi {

    private final LuaNpcEngine engine;

    public EnvoysLuaApi(LuaNpcEngine engine) {
        this.engine = engine;
    }

    public void install(LuaState state) {
        LuaTable envoys = new LuaTable();
        RegisteredFunction.bind(envoys, new RegisteredFunction[]{
                RegisteredFunction.ofV("say", this::say),
                RegisteredFunction.ofV("command", this::command),
                RegisteredFunction.ofV("on", this::on),
                RegisteredFunction.ofV("pos", this::pos),
                RegisteredFunction.ofV("name", this::name),
                RegisteredFunction.ofV("playersInRange", this::playersInRange),
                RegisteredFunction.ofFactory("wait", () -> new WaitFunction(engine)),
                RegisteredFunction.ofFactory("move", () -> new MoveFunction(engine)),
                RegisteredFunction.ofFactory("waitEvent", () -> new WaitEventFunction(engine)),
        });
        state.globals().rawset("envoys", envoys);
    }

    private Varargs say(LuaState state, Varargs args) throws LuaError {
        engine.sendMessageToTarget(args.arg(1).checkString());
        return Constants.NONE;
    }

    private Varargs command(LuaState state, Varargs args) throws LuaError {
        engine.runCommand(args.arg(1).checkString());
        return Constants.NONE;
    }

    private Varargs on(LuaState state, Varargs args) throws LuaError {
        String eventName = args.arg(1).checkString();
        LuaFunction callback = args.arg(2).checkFunction();
        int extra = args.arg(3).optInteger(0);

        EventType type = EventType.fromKey(eventName);
        if (type == null) {
            throw new LuaError("unknown event '" + eventName + "'");
        }

        engine.registerCallback(type, callback, extra);
        return Constants.NONE;
    }

    private Varargs pos(LuaState state, Varargs args) {
        return engine.posTable();
    }

    private Varargs name(LuaState state, Varargs args) {
        return engine.npcName();
    }

    private Varargs playersInRange(LuaState state, Varargs args) throws LuaError {
        int radius = Math.max(0, args.arg(1).optInteger(4));
        return engine.playersInRangeTable(radius);
    }

    private static final class WaitFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private WaitFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            int ticks = Math.max(0, args.arg(1).checkInteger());
            engine.beginWaitTicks(state, ticks);
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }

    private static final class MoveFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private MoveFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            double x = args.arg(1).checkDouble();
            double y = args.arg(2).checkDouble();
            double z = args.arg(3).checkDouble();
            double speed = args.arg(4).optDouble(1.0);

            if (!engine.beginMove(state, x, y, z, speed)) {
                return Constants.FALSE;
            }
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }

    private static final class WaitEventFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private WaitEventFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            String eventName = args.arg(1).checkString();
            int timeout = Math.max(0, args.arg(2).optInteger(0));
            engine.beginWaitEvent(state, eventName, timeout);
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }
}
