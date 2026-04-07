package com.example.dialognpc.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import com.example.dialognpc.util.MinecraftColors;

public class DialogNpcEntity extends PathAwareEntity {

    public record DialogOption(String label, String command) {}

    // TrackedData for client-side synchronization
    private static final TrackedData<String> TEXTURE_TYPE = DataTracker.registerData(DialogNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> CUSTOM_TEXTURE_DATA = DataTracker.registerData(DialogNpcEntity.class, TrackedDataHandlerRegistry.STRING);

    private String dialogTitle = "NPC";
    private String dialogText  = "Hello! How can I help you?";
    private String npcTexture  = "minecraft:textures/entity/player/wide/steve.png";
    private final List<DialogOption> options = new ArrayList<>();

    // Dialog customization - default to Minecraft colors
    private int backgroundColor = MinecraftColors.DARK_BLUE;
    private int titleColor    = MinecraftColors.GOLD;
    private int buttonWidth   = 180;
    private int borderColor   = MinecraftColors.GRAY;
    private int titleTextColor = MinecraftColors.YELLOW;
    private int optionsHeight = 0; // 0 = auto

    // Behavior flags (not tracked, server-side only)
    private boolean headTracking = true;   // Head follows players
    private boolean bodyRotation = false;  // Body rotates to face players
    private boolean canMove = false;       // Can move from spawn position
    private boolean canRotate = false;     // Can rotate (yaw/pitch changes)
    private boolean showHitbox = true;     // Show/hide hitbox

    public DialogNpcEntity(EntityType<? extends DialogNpcEntity> type, World world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setPersistent();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TEXTURE_TYPE, "vanilla");
        builder.add(CUSTOM_TEXTURE_DATA, "");
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void initGoals() {
        if (headTracking) {
            this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F, 1.0F));
        }
        if (bodyRotation) {
            this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F, 0.5F));
        }
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            com.example.dialognpc.network.ModPackets.sendOpenDialog(serverPlayer, this);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    public boolean isPushable() { return !canMove; }

    @Override
    public boolean isImmobile() { return !canMove; }

    @Override
    public boolean isCustomNameVisible() { return showHitbox; }

    // ── NBT ──────────────────────────────────────────────────────────────

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("DialogTitle", dialogTitle);
        nbt.putString("DialogText",  dialogText);
        nbt.putString("NpcTexture",  npcTexture);
        nbt.putInt("BackgroundColor", backgroundColor);
        nbt.putInt("TitleColor", titleColor);
        nbt.putInt("ButtonWidth", buttonWidth);
        nbt.putInt("BorderColor", borderColor);
        nbt.putInt("TitleTextColor", titleTextColor);
        nbt.putInt("OptionsHeight", optionsHeight);
        // Behavior flags
        nbt.putBoolean("HeadTracking", headTracking);
        nbt.putBoolean("BodyRotation", bodyRotation);
        nbt.putBoolean("CanMove", canMove);
        nbt.putBoolean("CanRotate", canRotate);
        nbt.putBoolean("ShowHitbox", showHitbox);
        // Custom texture (save from tracked data)
        nbt.putString("CustomTextureData", this.dataTracker.get(CUSTOM_TEXTURE_DATA));
        nbt.putString("TextureType", this.dataTracker.get(TEXTURE_TYPE));
        NbtList list = new NbtList();
        for (DialogOption opt : options) {
            NbtCompound c = new NbtCompound();
            c.putString("Label",   opt.label());
            c.putString("Command", opt.command());
            list.add(c);
        }
        nbt.put("DialogOptions", list);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("DialogTitle")) dialogTitle = nbt.getString("DialogTitle");
        if (nbt.contains("DialogText"))  dialogText  = nbt.getString("DialogText");
        if (nbt.contains("NpcTexture"))  npcTexture  = nbt.getString("NpcTexture");
        if (nbt.contains("BackgroundColor")) backgroundColor = nbt.getInt("BackgroundColor");
        if (nbt.contains("TitleColor"))    titleColor    = nbt.getInt("TitleColor");
        if (nbt.contains("ButtonWidth"))   buttonWidth   = nbt.getInt("ButtonWidth");
        if (nbt.contains("BorderColor"))   borderColor   = nbt.getInt("BorderColor");
        if (nbt.contains("TitleTextColor")) titleTextColor = nbt.getInt("TitleTextColor");
        if (nbt.contains("OptionsHeight")) optionsHeight = nbt.getInt("OptionsHeight");
        // Behavior flags
        if (nbt.contains("HeadTracking")) headTracking = nbt.getBoolean("HeadTracking");
        if (nbt.contains("BodyRotation")) bodyRotation = nbt.getBoolean("BodyRotation");
        if (nbt.contains("CanMove"))      canMove      = nbt.getBoolean("CanMove");
        if (nbt.contains("CanRotate"))    canRotate    = nbt.getBoolean("CanRotate");
        if (nbt.contains("ShowHitbox"))   showHitbox   = nbt.getBoolean("ShowHitbox");
        // Custom texture (set tracked data)
        if (nbt.contains("CustomTextureData")) this.dataTracker.set(CUSTOM_TEXTURE_DATA, nbt.getString("CustomTextureData"));
        if (nbt.contains("TextureType"))     this.dataTracker.set(TEXTURE_TYPE, nbt.getString("TextureType"));
        // Re-init goals if world is loaded
        if (this.getWorld() != null) {
            this.goalSelector.clear(g -> true);
            this.initGoals();
        }
        options.clear();
        if (nbt.contains("DialogOptions", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("DialogOptions", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                options.add(new DialogOption(c.getString("Label"), c.getString("Command")));
            }
        }
    }

    // ── Getters / setters ────────────────────────────────────────────────

    public String getDialogTitle()             { return dialogTitle; }
    public void   setDialogTitle(String t)     { this.dialogTitle = t; }
    public String getDialogText()              { return dialogText; }
    public void   setDialogText(String t)      { this.dialogText = t; }
    public String getNpcTexture()              { return npcTexture; }
    public void   setNpcTexture(String tex)    { this.npcTexture = tex; }
    public List<DialogOption> getDialogOptions()  { return options; }
    public void addDialogOption(DialogOption opt) { options.add(opt); }
    public void clearDialogOptions()              { options.clear(); }

    // Customization getters/setters
    public int    getBackgroundColor()         { return backgroundColor; }
    public void   setBackgroundColor(int c)    { this.backgroundColor = c; }
    public int    getTitleColor()              { return titleColor; }
    public void   setTitleColor(int c)         { this.titleColor = c; }
    public int    getButtonWidth()             { return buttonWidth; }
    public void   setButtonWidth(int w)        { this.buttonWidth = w; }
    public int    getBorderColor()             { return borderColor; }
    public void   setBorderColor(int c)        { this.borderColor = c; }
    public int    getTitleTextColor()          { return titleTextColor; }
    public void   setTitleTextColor(int c)     { this.titleTextColor = c; }
    public int    getOptionsHeight()           { return optionsHeight; }
    public void   setOptionsHeight(int h)      { this.optionsHeight = h; }

    // Behavior getters/setters
    public boolean isHeadTracking()            { return headTracking; }
    public void   setHeadTracking(boolean v)   { this.headTracking = v; refreshGoals(); }
    public boolean isBodyRotation()            { return bodyRotation; }
    public void   setBodyRotation(boolean v)   { this.bodyRotation = v; refreshGoals(); }
    public boolean isCanMove()                 { return canMove; }
    public void   setCanMove(boolean v)        { this.canMove = v; }
    public boolean isCanRotate()               { return canRotate; }
    public void   setCanRotate(boolean v)      { this.canRotate = v; }
    public boolean isShowHitbox()              { return showHitbox; }
    public void   setShowHitbox(boolean v)     { this.showHitbox = v; }

    // Custom texture getters/setters (use tracked data for client sync)
    public String getCustomTextureData()       { return this.dataTracker.get(CUSTOM_TEXTURE_DATA); }
    public void   setCustomTextureData(String v) { this.dataTracker.set(CUSTOM_TEXTURE_DATA, v); }
    public String getTextureType()             { return this.dataTracker.get(TEXTURE_TYPE); }
    public void   setTextureType(String v)     { this.dataTracker.set(TEXTURE_TYPE, v); }

    private void refreshGoals() {
        if (this.getWorld() != null) {
            this.goalSelector.clear(g -> true);
            this.initGoals();
        }
    }
}
