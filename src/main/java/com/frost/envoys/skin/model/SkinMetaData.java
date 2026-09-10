package com.frost.envoys.skin.model;

public class SkinMetaData {
    public String uuid;
    public String hash;
    public String model;
    public String source;
    public long updatedAt;

    public SkinMetaData() {}

    public SkinMetaData(String uuid, String hash, String model, String source, long updatedAt) {
        this.uuid = uuid;
        this.hash = hash;
        this.model = model;
        this.source = source;
        this.updatedAt = updatedAt;
    }
}