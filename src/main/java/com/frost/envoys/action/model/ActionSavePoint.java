package com.frost.envoys.action.model;

import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public class ActionSavePoint extends AbstractActionData {

    @SerializedName("save_id")
    public String saveId = "";

    @SerializedName("checkpoint_uuid")
    public String checkpointUuid;

    @SerializedName("exit_on_save")
    public boolean exitOnSave = false;

    public ActionSavePoint(String id) {
        super(id);
        this.checkpointUuid = UUID.randomUUID().toString();
    }

    @Override
    public String getType() {
        return "save_point";
    }
}
