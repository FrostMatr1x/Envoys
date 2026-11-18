package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionMerchantLevelUp;

public final class MerchantLevelUpActionHandler implements NpcActionHandler<ActionMerchantLevelUp> {

    @Override
    public void execute(ActionMerchantLevelUp action, ActionContext context) {
        String npcUuid = context.npcUuid() == null ? null : context.npcUuid().toString();
        MerchantSlotData.addUnlocked(context.player(), npcUuid, 1);
        context.advance(action.nextActionId);
    }
}
