package com.frost.envoys.action.model;

public class ActionMerchantLevelUp extends AbstractActionData {

    public ActionMerchantLevelUp(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "merchant_level_up";
    }
}
