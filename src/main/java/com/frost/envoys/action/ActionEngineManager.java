package com.frost.envoys.action;

import com.frost.envoys.action.handler.ChatActionHandler;
import com.frost.envoys.action.handler.CommandActionHandler;
import com.frost.envoys.action.handler.DelayActionHandler;
import com.frost.envoys.action.handler.DialogActionHandler;
import com.frost.envoys.action.handler.MoveActionHandler;
import com.frost.envoys.action.handler.TradeActionHandler;
import com.frost.envoys.action.model.ActionChat;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionDelay;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionTrade;

public class ActionEngineManager {

    private static NpcActionEngine instance;

    public static void initialize() {
        instance = new NpcActionEngine();

        instance.registry().register(ActionMove.class, new MoveActionHandler());
        instance.registry().register(ActionDelay.class, new DelayActionHandler());
        instance.registry().register(ActionChat.class, new ChatActionHandler());
        instance.registry().register(ActionDialog.class, new DialogActionHandler());
        instance.registry().register(ActionTrade.class, new TradeActionHandler());
        instance.registry().register(ActionCommand.class, new CommandActionHandler());
    }

    public static NpcActionEngine getInstance() {
        return instance;
    }
}
