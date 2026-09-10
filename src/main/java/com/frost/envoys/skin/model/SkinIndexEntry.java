package com.frost.envoys.skin.model;

import java.util.ArrayList;
import java.util.List;

public class SkinIndexEntry {
    public String uuid;
    public String path;
    public String model;
    public String source;
    public String hash;
    public long updatedAt;

    public SkinIndexEntry() {}

    public SkinIndexEntry(String uuid, String path, String model, String source, String hash, long updatedAt) {
        this.uuid = uuid;
        this.path = path;
        this.model = model;
        this.source = source;
        this.hash = hash;
        this.updatedAt = updatedAt;
    }
}