package com.frost.envoys.action;

import com.frost.envoys.action.handler.ChatActionHandler;
import com.frost.envoys.action.handler.CommandActionHandler;
import com.frost.envoys.action.handler.DelayActionHandler;
import com.frost.envoys.action.handler.DialogActionHandler;
import com.frost.envoys.action.handler.LoadPointActionHandler;
import com.frost.envoys.action.handler.MerchantLevelUpActionHandler;
import com.frost.envoys.action.handler.MoveActionHandler;
import com.frost.envoys.action.handler.QuestAdvanceStepActionHandler;
import com.frost.envoys.action.handler.QuestCheckActionHandler;
import com.frost.envoys.action.handler.QuestGiveActionHandler;
import com.frost.envoys.action.handler.QuestMarkCompletedActionHandler;
import com.frost.envoys.action.handler.SavePointActionHandler;
import com.frost.envoys.action.handler.StartActionHandler;
import com.frost.envoys.action.handler.TradeActionHandler;
import com.frost.envoys.action.model.ActionChat;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionDelay;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.action.model.ActionLoadPoint;
import com.frost.envoys.action.model.ActionMerchantLevelUp;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionQuestAdvanceStep;
import com.frost.envoys.action.model.ActionQuestCheck;
import com.frost.envoys.action.model.ActionQuestGive;
import com.frost.envoys.action.model.ActionQuestMarkCompleted;
import com.frost.envoys.action.model.ActionSavePoint;
import com.frost.envoys.action.model.ActionStart;
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
        instance.registry().register(ActionQuestGive.class, new QuestGiveActionHandler());
        instance.registry().register(ActionQuestCheck.class, new QuestCheckActionHandler());
        instance.registry().register(ActionQuestAdvanceStep.class, new QuestAdvanceStepActionHandler());
        instance.registry().register(ActionQuestMarkCompleted.class, new QuestMarkCompletedActionHandler());
        instance.registry().register(ActionStart.class, new StartActionHandler());
        instance.registry().register(ActionSavePoint.class, new SavePointActionHandler());
        instance.registry().register(ActionLoadPoint.class, new LoadPointActionHandler());
        instance.registry().register(ActionMerchantLevelUp.class, new MerchantLevelUpActionHandler());
    }

    public static NpcActionEngine getInstance() {
        return instance;
    }
}
