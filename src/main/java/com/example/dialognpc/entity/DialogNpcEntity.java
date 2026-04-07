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
import net.minecraft.entity.Entity;
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

    public record DialogOption(String label, String command, String soundId, String particleType, int particleCount, String labelTranslationKey) {}

    // TrackedData for client-side synchronization
    private static final TrackedData<String> TEXTURE_TYPE = DataTracker.registerData(DialogNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> CUSTOM_TEXTURE_DATA = DataTracker.registerData(DialogNpcEntity.class, TrackedDataHandlerRegistry.STRING);

    // Dialog content (literal text or translation keys)
    private String dialogTitle = "NPC";
    private String dialogTitleKey = "";  // Translation key for title (empty = use literal)
    private String dialogText  = "Hello! How can I help you?";
    private String dialogTextKey = "";   // Translation key for text (empty = use literal)
    private String npcName = "";         // Custom name for NPC (literal)
    private String npcNameKey = "";      // Translation key for NPC name (empty = use literal)
    private String npcTexture  = "minecraft:textures/entity/player/wide/steve.png";
    private final List<DialogOption> options = new ArrayList<>();

    // Dialog customization - default to Minecraft colors
    private int backgroundColor = MinecraftColors.DARK_BLUE;
    private int titleColor    = MinecraftColors.GOLD;
    private int buttonWidth   = 180;
    private int borderColor   = MinecraftColors.GRAY;
    private int titleTextColor = MinecraftColors.YELLOW;
    private int optionsHeight = 0; // 0 = auto
    private int optionTextColor = MinecraftColors.WHITE; // Color for option button text

    // Modal layout customization
    private int boxWidth    = 280;  // Default box width
    private int boxHeight   = 0;    // 0 = auto
    private int titleHeight = 24;   // Title bar height
    private int boxPadding  = 10;   // Padding inside box
    private int portraitSize = 40;  // Portrait size (px)

    // Behavior flags (not tracked, server-side only)
    private boolean headTracking = true;   // Head follows players
    private boolean bodyRotation = false;  // Body rotates to face players
    private boolean canMove = false;       // Can move from spawn position
    private boolean canRotate = false;     // Can rotate (yaw/pitch changes)
    private boolean showHitbox = true;     // Show/hide hitbox
    private boolean noClip = false;        // No collision (can walk through)
    private boolean hasHitbox = true;      // Has collision box (can be traspasado)

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
    public boolean isPushable() { return !canMove && hasHitbox; }

    @Override
    public boolean isImmobile() { return !canMove; }

    @Override
    public boolean isCustomNameVisible() { return showHitbox; }

    @Override
    public boolean collidesWith(Entity other) {
        return hasHitbox && super.collidesWith(other);
    }

    // ── NBT ──────────────────────────────────────────────────────────────

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("DialogTitle", dialogTitle);
        nbt.putString("DialogTitleKey", dialogTitleKey);
        nbt.putString("DialogText",  dialogText);
        nbt.putString("DialogTextKey", dialogTextKey);
        nbt.putString("NpcName", npcName);
        nbt.putString("NpcNameKey", npcNameKey);
        nbt.putString("NpcTexture",  npcTexture);
        nbt.putInt("BackgroundColor", backgroundColor);
        nbt.putInt("TitleColor", titleColor);
        nbt.putInt("ButtonWidth", buttonWidth);
        nbt.putInt("BorderColor", borderColor);
        nbt.putInt("TitleTextColor", titleTextColor);
        nbt.putInt("OptionsHeight", optionsHeight);
        nbt.putInt("OptionTextColor", optionTextColor);
        // Modal layout
        nbt.putInt("BoxWidth", boxWidth);
        nbt.putInt("BoxHeight", boxHeight);
        nbt.putInt("TitleHeight", titleHeight);
        nbt.putInt("BoxPadding", boxPadding);
        nbt.putInt("PortraitSize", portraitSize);
        // Behavior flags
        nbt.putBoolean("HeadTracking", headTracking);
        nbt.putBoolean("BodyRotation", bodyRotation);
        nbt.putBoolean("CanMove", canMove);
        nbt.putBoolean("CanRotate", canRotate);
        nbt.putBoolean("ShowHitbox", showHitbox);
        nbt.putBoolean("HasHitbox", hasHitbox);
        // Custom texture (save from tracked data)
        nbt.putString("CustomTextureData", this.dataTracker.get(CUSTOM_TEXTURE_DATA));
        nbt.putString("TextureType", this.dataTracker.get(TEXTURE_TYPE));
        NbtList list = new NbtList();
        for (DialogOption opt : options) {
            NbtCompound c = new NbtCompound();
            c.putString("Label",   opt.label());
            c.putString("Command", opt.command());
            c.putString("SoundId", opt.soundId() != null ? opt.soundId() : "");
            c.putString("ParticleType", opt.particleType() != null ? opt.particleType() : "");
            c.putInt("ParticleCount", opt.particleCount());
            c.putString("LabelTranslationKey", opt.labelTranslationKey() != null ? opt.labelTranslationKey() : "");
            list.add(c);
        }
        nbt.put("DialogOptions", list);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("DialogTitle")) dialogTitle = nbt.getString("DialogTitle");
        if (nbt.contains("DialogTitleKey")) dialogTitleKey = nbt.getString("DialogTitleKey");
        if (nbt.contains("DialogText"))  dialogText  = nbt.getString("DialogText");
        if (nbt.contains("DialogTextKey")) dialogTextKey = nbt.getString("DialogTextKey");
        if (nbt.contains("NpcName"))     npcName     = nbt.getString("NpcName");
        if (nbt.contains("NpcNameKey"))  npcNameKey  = nbt.getString("NpcNameKey");
        if (nbt.contains("NpcTexture"))  npcTexture  = nbt.getString("NpcTexture");
        if (nbt.contains("BackgroundColor")) backgroundColor = nbt.getInt("BackgroundColor");
        if (nbt.contains("TitleColor"))    titleColor    = nbt.getInt("TitleColor");
        if (nbt.contains("ButtonWidth"))   buttonWidth   = nbt.getInt("ButtonWidth");
        if (nbt.contains("BorderColor"))   borderColor   = nbt.getInt("BorderColor");
        if (nbt.contains("TitleTextColor")) titleTextColor = nbt.getInt("TitleTextColor");
        if (nbt.contains("OptionsHeight")) optionsHeight = nbt.getInt("OptionsHeight");
        if (nbt.contains("OptionTextColor")) optionTextColor = nbt.getInt("OptionTextColor");
        // Modal layout
        if (nbt.contains("BoxWidth"))    boxWidth    = nbt.getInt("BoxWidth");
        if (nbt.contains("BoxHeight"))   boxHeight   = nbt.getInt("BoxHeight");
        if (nbt.contains("TitleHeight")) titleHeight = nbt.getInt("TitleHeight");
        if (nbt.contains("BoxPadding"))  boxPadding  = nbt.getInt("BoxPadding");
        if (nbt.contains("PortraitSize")) portraitSize = nbt.getInt("PortraitSize");
        // Behavior flags
        if (nbt.contains("HeadTracking")) headTracking = nbt.getBoolean("HeadTracking");
        if (nbt.contains("BodyRotation")) bodyRotation = nbt.getBoolean("BodyRotation");
        if (nbt.contains("CanMove"))      canMove      = nbt.getBoolean("CanMove");
        if (nbt.contains("CanRotate"))    canRotate    = nbt.getBoolean("CanRotate");
        if (nbt.contains("ShowHitbox"))   showHitbox   = nbt.getBoolean("ShowHitbox");
        if (nbt.contains("HasHitbox"))    hasHitbox    = nbt.getBoolean("HasHitbox");
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
                String soundId = c.contains("SoundId") && !c.getString("SoundId").isEmpty() ? c.getString("SoundId") : null;
                String particleType = c.contains("ParticleType") && !c.getString("ParticleType").isEmpty() ? c.getString("ParticleType") : null;
                int particleCount = c.contains("ParticleCount") ? c.getInt("ParticleCount") : 0;
                String labelTranslationKey = c.contains("LabelTranslationKey") ? c.getString("LabelTranslationKey") : null;
                options.add(new DialogOption(c.getString("Label"), c.getString("Command"), soundId, particleType, particleCount, labelTranslationKey));
            }
        }
    }

    // ── Getters / setters ────────────────────────────────────────────────

    public String getDialogTitle()             { return dialogTitle; }
    public void   setDialogTitle(String t)     { this.dialogTitle = t; }
    public String getDialogTitleKey()          { return dialogTitleKey; }
    public void   setDialogTitleKey(String k)  { this.dialogTitleKey = k; }
    public String getNpcName()                 { return npcName; }
    public void   setNpcName(String name)      { this.npcName = name; }
    public String getNpcNameKey()              { return npcNameKey; }
    public void   setNpcNameKey(String k)      { this.npcNameKey = k; }
    public String getDialogText()              { return dialogText; }
    public void   setDialogText(String t)      { this.dialogText = t; }
    public String getDialogTextKey()           { return dialogTextKey; }
    public void   setDialogTextKey(String k)   { this.dialogTextKey = k; }
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
    public int    getOptionTextColor()         { return optionTextColor; }
    public void   setOptionTextColor(int c)    { this.optionTextColor = c; }

    // Modal layout getters/setters
    public int    getBoxWidth()                { return boxWidth; }
    public void   setBoxWidth(int w)           { this.boxWidth = w; }
    public int    getBoxHeight()               { return boxHeight; }
    public void   setBoxHeight(int h)          { this.boxHeight = h; }
    public int    getTitleHeight()             { return titleHeight; }
    public void   setTitleHeight(int h)        { this.titleHeight = h; }
    public int    getBoxPadding()              { return boxPadding; }
    public void   setBoxPadding(int p)         { this.boxPadding = p; }
    public int    getPortraitSize()            { return portraitSize; }
    public void   setPortraitSize(int s)       { this.portraitSize = s; }

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
    public boolean isHasHitbox()               { return hasHitbox; }
    public void   setHasHitbox(boolean v)      { this.hasHitbox = v; }

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
