package com.example.dialognpc.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
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

public class DialogNpcEntity extends PathAwareEntity {

    public record DialogOption(String label, String command) {}

    private String dialogTitle = "NPC";
    private String dialogText  = "Hello! How can I help you?";
    private String npcTexture  = "minecraft:textures/entity/player/wide/steve.png";
    private final List<DialogOption> options = new ArrayList<>();

    // Dialog customization
    private int backgroundColor = 0xFF1A1A2E;
    private int titleColor    = 0xFF2D2D5A;
    private int buttonWidth   = 180;
    private int borderColor   = 0xFF404080;
    private int titleTextColor = 0xFFFFD966;
    private int optionsHeight = 0; // 0 = auto

    public DialogNpcEntity(EntityType<? extends DialogNpcEntity> type, World world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F, 1.0F));
        this.goalSelector.add(2, new LookAroundGoal(this));
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
    public boolean isPushable() { return false; }

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
}
