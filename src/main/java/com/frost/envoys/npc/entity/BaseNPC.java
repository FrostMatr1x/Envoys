package com.frost.envoys.npc.entity;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCPassportData;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.ScriptRunner;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.lua.LuaEngineManager;
import com.frost.envoys.lua.LuaNpcEngine;
import com.frost.envoys.init.ModItems;
import com.frost.envoys.network.payload.OpenSettingGuiPayload;
import com.frost.envoys.skin.gui.SkinGuiPreview;
import com.frost.envoys.skin.service.SkinSyncService;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BaseNPC extends PathfinderMob {

    private NPCPassportData passport;
    private ResourceLocation clientSkinLocation = null;
    private String lastLoadedHash = "";
    private boolean scriptedMovement = false;
    private final Set<UUID> playersInRange = new HashSet<>();

    private LookAtPlayerGoal lookAtPlayerGoal;
    private RandomLookAroundGoal randomLookAroundGoal;

    public static final EntityDataAccessor<String> SKIN_TYPE = SynchedEntityData.defineId(BaseNPC.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> SKIN_VALUE = SynchedEntityData.defineId(BaseNPC.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> SKIN_MODEL = SynchedEntityData.defineId(BaseNPC.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> SKIN_HASH = SynchedEntityData.defineId(BaseNPC.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> EMOTE = SynchedEntityData.defineId(BaseNPC.class, EntityDataSerializers.STRING);

    public BaseNPC(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        passport = new NPCPassportData();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.lookAtPlayerGoal = new LookAtPlayerGoal(this, Player.class, 6.0F);
        this.goalSelector.addGoal(2, this.lookAtPlayerGoal);
        this.randomLookAroundGoal = new RandomLookAroundGoal(this);
        this.goalSelector.addGoal(3, this.randomLookAroundGoal);
    }

    public void setLookLocked(boolean locked) {
        if (locked) {
            if (this.lookAtPlayerGoal != null) {
                this.goalSelector.removeGoal(this.lookAtPlayerGoal);
            }
            if (this.randomLookAroundGoal != null) {
                this.goalSelector.removeGoal(this.randomLookAroundGoal);
            }
        } else {
            if (this.lookAtPlayerGoal == null
                    || !this.goalSelector.getAvailableGoals().stream().anyMatch(e -> e.getGoal() == this.lookAtPlayerGoal)) {
                this.lookAtPlayerGoal = new LookAtPlayerGoal(this, Player.class, 6.0F);
                this.goalSelector.addGoal(2, this.lookAtPlayerGoal);
            }
            if (this.randomLookAroundGoal == null
                    || !this.goalSelector.getAvailableGoals().stream().anyMatch(e -> e.getGoal() == this.randomLookAroundGoal)) {
                this.randomLookAroundGoal = new RandomLookAroundGoal(this);
                this.goalSelector.addGoal(3, this.randomLookAroundGoal);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.SCALE, 1.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.GRAVITY, 0.08D) 
            .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1000.0D)
            .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    public ResourceLocation getClientSkinLocation() {
        String value = this.entityData.get(SKIN_VALUE);
        String type = this.entityData.get(SKIN_TYPE);
        String hash = this.entityData.get(SKIN_HASH);

        if (value.isBlank()) return null;

        if (clientSkinLocation == null || !lastLoadedHash.equalsIgnoreCase(hash)) {
            this.lastLoadedHash = hash;
            SkinSyncService.loadSkinAsync(value, type, true).thenAccept(result -> {
                if (result.isSuccess()) {
                    Minecraft.getInstance().execute(() -> {
                        this.clientSkinLocation = SkinGuiPreview.registerDynamicSkin("npc_" + getUUID(), result.pngData());
                    });
                }
            });
        }
        return clientSkinLocation;
    }

    public String getSkinModel() {
        return this.entityData.get(SKIN_MODEL);
    }

    public String getEmoteType() {
        return this.entityData.get(EMOTE);
    }

    public void setEmoteType(String emote) {
        this.entityData.set(EMOTE, emote != null ? emote : "");
    }

    public void beginScriptedMovement() {
        this.scriptedMovement = true;
    }

    public void endScriptedMovement() {
        this.scriptedMovement = false;
    }

    public boolean isScriptedMovementActive() {
        return this.scriptedMovement;
    }

    public Set<UUID> getPlayersInRange() {
        return playersInRange;
    }

    public void applySkinData(String skinType, String skinValue, String model, String hash) {
        this.entityData.set(SKIN_TYPE, skinType);
        this.entityData.set(SKIN_VALUE, skinValue);
        this.entityData.set(SKIN_MODEL, model);
        this.entityData.set(SKIN_HASH, hash);

        passport.skinType = skinType;
        passport.skinValue = skinValue;
        passport.skinModel = model;
        passport.skinHash = hash;
    }

    public void applyPassportData(NPCPassportData passport) {
        if (passport == null) return;

        this.passport = passport;

        if (passport.npcName != null && !passport.npcName.isEmpty()) {
            this.setCustomName(Component.literal(passport.npcName));
            this.setCustomNameVisible(true);
        }

        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(passport.hp);
            if (this.getHealth() > passport.hp) {
                this.setHealth(passport.hp);
            }
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(passport.speed);
        }

        if (this.getAttribute(Attributes.SCALE) != null) {
            this.getAttribute(Attributes.SCALE).setBaseValue(passport.size);
        }

        this.getAttribute(Attributes.GRAVITY).setBaseValue(passport.useGravity ? 0.08 : 0);

        this.entityData.set(SKIN_TYPE, passport.skinType);
        this.entityData.set(SKIN_VALUE, passport.skinValue);
        this.entityData.set(SKIN_MODEL, passport.skinModel);
        this.entityData.set(SKIN_HASH, passport.skinHash);

        this.setEmoteType(passport.emote != null ? passport.emote : "");

        this.setInvulnerable(!passport.canTakeDamage);
        this.setInvisible(!passport.isVisible);

        this.setLookLocked(passport.lookLocked);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_TYPE, "NICKNAME");
        builder.define(SKIN_VALUE, "");
        builder.define(SKIN_MODEL, "classic");
        builder.define(SKIN_HASH, "");
        builder.define(EMOTE, "");
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide()) {
            NPCInteractManager.byUUID(this.getUUID()).ifPresent(manager -> {
                this.applyPassportData(manager.passport);
            });
            LuaEngineManager.ensure(this.getUUID());
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.level().isClientSide) {
            boolean isCreativeTuner = stack.is(ModItems.NPC_TUNER.get());
            boolean isSurvivalTuner = false;//stack.is(ModItems.SURVIVAL_NPC_TUNER.get());

            if (isCreativeTuner || isSurvivalTuner) {
                if (player instanceof ServerPlayer serverPlayer) {

                    if (isCreativeTuner && !serverPlayer.hasPermissions(4)) {
                        return InteractionResult.FAIL;
                    }

                    NPCInteractManager manager = NPCInteractManager.byUUID(this.getUUID())
                            .orElseGet(() -> new NPCInteractManager(this.getUUID()));

                    NPCPassportData p = (passport != null) ? passport : manager.passport;

                    if (isSurvivalTuner && p.creativeTunerOnly) {
                        serverPlayer.sendSystemMessage(Component.translatable("envoys.gui.npc.creative_only"));
                        return InteractionResult.FAIL;
                    }

                    if (p.holdX == 0 && p.holdY == 0 && p.holdZ == 0) {
                        p.holdX = this.getX();
                        p.holdY = this.getY();
                        p.holdZ = this.getZ();
                    }

                    NPCScriptData scriptData = NPCScriptData.fromManager(manager);
                    String jsonScript = EntityActionAdapter.GSON.toJson(scriptData);

                    serverPlayer.connection.send(new OpenSettingGuiPayload(
                        isCreativeTuner,
                        this.getUUID(),
                        p.npcName,
                        p.size,
                        p.speed,
                        p.hp,
                        new Vec3(p.holdX, p.holdY, p.holdZ),
                        p.isVisible,
                        p.isHoldPosEnabled,
                        p.canTakeDamage,
                        p.useGravity,
                        p.creativeTunerOnly,
                        p.lookLocked,
                        jsonScript,
                        p.emote != null ? p.emote : ""
                    ));
                }
                return InteractionResult.SUCCESS;
            } else {
                LuaNpcEngine luaEngine = LuaEngineManager.getEngine(this.getUUID());
                if (luaEngine != null) {
                    if (luaEngine.isErrored()) {
                        if (player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(4)) {
                            serverPlayer.sendSystemMessage(Component.literal("§c[Envoys] Lua: " + luaEngine.errorText()));
                        }
                    } else {
                        luaEngine.dispatchEvent(EventType.CLICK, player);
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            EmoteIntegration.tickClient(this);
            return;
        }

        trackPassportLocation();

        if (passport.isHoldPosEnabled && !scriptedMovement) {
            double targetX = passport.holdX;
            double targetY = passport.holdY;
            double targetZ = passport.holdZ;

            if (this.distanceToSqr(targetX, targetY, targetZ) > 0.01D) {
                this.teleportTo(targetX, targetY, targetZ);
                this.setDeltaMovement(Vec3.ZERO);
            }
        }

        tickScriptEngine();
    }

    private void tickScriptEngine() {
        LuaNpcEngine luaEngine = LuaEngineManager.ensure(this.getUUID());
        if (luaEngine != null) {
            luaEngine.tick(this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
            this.discard();
            return true;
        }

        if (!this.level().isClientSide) {
            Player attacker = (source.getEntity() instanceof Player player) ? player : null;
            LuaNpcEngine luaEngine = LuaEngineManager.getEngine(this.getUUID());
            if (luaEngine != null) {
                if (luaEngine.isErrored()) {
                    if (attacker instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(4)) {
                        serverPlayer.sendSystemMessage(Component.literal("§c[Envoys] Lua: " + luaEngine.errorText()));
                    }
                } else {
                    luaEngine.dispatchEvent(EventType.KICK, attacker);
                }
            }
        }

        if (passport.canTakeDamage) {
            return super.hurt(source, amount);
        }
        return false;
    }

    private void trackPassportLocation() {
        if (passport == null || this.level() == null) {
            return;
        }
        passport.dimension = this.level().dimension().location().toString();
        passport.lastX = this.getX();
        passport.lastY = this.getY();
        passport.lastZ = this.getZ();
        passport.hasLastPosition = true;

        // NPCConfigManager persists the SCRIPTS manager passport, not this entity's
        // field. If the two objects diverged (manager created after the entity), the
        // saved config would never contain a location, leaving the record
        // unverifiable by /envoys cleanup npcs. Mirror the tracked location through.
        NPCInteractManager manager = NPCInteractManager.byUUID(this.getUUID()).orElse(null);
        if (manager != null && manager.passport != null && manager.passport != this.passport) {
            NPCPassportData saved = manager.passport;
            saved.dimension = passport.dimension;
            saved.lastX = passport.lastX;
            saved.lastY = passport.lastY;
            saved.lastZ = passport.lastZ;
            saved.hasLastPosition = true;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (this.level() != null && !this.level().isClientSide) {
            trackPassportLocation();
        }
        super.remove(reason);
        if (this.level() != null && !this.level().isClientSide) {
            LuaEngineManager.remove(this.getUUID());
            ScriptRunner.remove(this.getUUID());
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) { 
        if (passport.canTakeDamage) {
            return false;
        }
        return true; 
    }

    @Override
    public void setDeltaMovement(Vec3 vec) { 
        if (scriptedMovement) {
            if (passport.useGravity) {
                super.setDeltaMovement(vec);
            } else {
                super.setDeltaMovement(new Vec3(vec.x, 0, vec.z));
            }
            return;
        }
        if (passport.useGravity) {
            super.setDeltaMovement(new Vec3(0, vec.y, 0));
            return;
        }
        super.setDeltaMovement(Vec3.ZERO); 
    }

    @Override
    public float getSoundVolume() {
        return super.getSoundVolume();
    }

    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean isOnFire() { return false; }
    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
    @Override
    public void checkDespawn() {}
    @Override
    public boolean canRide(Entity vehicle) { return false; }
    @Override
    public boolean canBeLeashed() { return false; }
    @Override
    public boolean isPushedByFluid() { return false; }
    @Override
    public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override
    public boolean canUsePortal(boolean active) { return false; }
    @Override
    public boolean canBeSeenAsEnemy() { return false; }
}