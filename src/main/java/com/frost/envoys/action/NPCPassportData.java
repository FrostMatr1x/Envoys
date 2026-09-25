package com.frost.envoys.action;

public class NPCPassportData {
    public String npcName = "";
    public float size = 1.0f;
    public float speed = 0.25f;
    public float hp = 20.0f;
    public double holdX;
    public double holdY;
    public double holdZ;
    public boolean isVisible = true;
    public boolean isHoldPosEnabled = false;
    public boolean canTakeDamage = false;
    public boolean useGravity = true;
    public boolean creativeTunerOnly = false;
    public boolean lookLocked = false;
    public String emote = "";

    public String skinType = "NICKNAME";
    public String skinValue = "";
    public String skinModel = "classic";
    public String skinHash = "";

    public String dimension = "";
    public double lastX;
    public double lastY;
    public double lastZ;
    public boolean hasLastPosition = false;
}