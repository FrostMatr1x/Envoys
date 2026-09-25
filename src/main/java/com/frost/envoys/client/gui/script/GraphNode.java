package com.frost.envoys.client.gui.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphNode {

    public String id = "";
    public String type = "";
    public Map<String, String> params = new LinkedHashMap<>();
    public String nextId;
    public List<BranchOption> options = new ArrayList<>();
    public List<TradeOffer> offers = new ArrayList<>();

    public GraphNode() {
    }

    public GraphNode(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String param(String key, String fallback) {
        String value = params.get(key);
        return value == null ? fallback : value;
    }

    public int intParam(String key, int fallback) {
        try {
            return Integer.parseInt(param(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public float floatParam(String key, float fallback) {
        try {
            return Float.parseFloat(param(key, Float.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean boolParam(String key, boolean fallback) {
        String value = params.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public boolean isBranch() {
        return ScriptNodeTypes.isBranch(type);
    }

    public static final class BranchOption {
        public String key = "";
        public String label = "";
        public String headId;

        public BranchOption() {
        }

        public BranchOption(String key, String label, String headId) {
            this.key = key;
            this.label = label;
            this.headId = headId;
        }
    }

    public static final class TradeOffer {
        public String in1 = "";
        public int in1count = 1;
        public String in2 = "";
        public int in2count = 1;
        public String out = "";
        public int outcount = 1;
        public float priceMultiplier = 1.0f;
        public int demand = 0;
        public int maxTrades = -1;
        public int resetTime = -1;
        public int requiredLevel = 0;

        public TradeOffer() {
        }
    }
}
