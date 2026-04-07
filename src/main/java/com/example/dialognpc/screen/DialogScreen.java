package com.example.dialognpc.screen;

import com.example.dialognpc.network.ModPackets;
import com.example.dialognpc.util.MinecraftColors;
import com.example.dialognpc.util.TextureLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class DialogScreen extends Screen {

    // ── Layout Constants ──────────────────────────────────────────────────
    private static final int BTN_HEIGHT        = 20;
    private static final int BTN_GAP           = 4;
    private static final int TEXT_BOX_H        = 90;
    private static final int DEFAULT_BTN_WIDTH = 180;

    // ── Default Colors ───────────────────────────────────────────────────
    private static final int COLOR_TEXT = MinecraftColors.WHITE;

    // ── Data ──────────────────────────────────────────────────────────────
    private final UUID         npcUuid;
    private final String       dialogTitle;
    private final String       dialogText;
    private final String       npcName;
    private final List<String> optionLabels;
    private final List<String> optionSounds;
    private final List<String> optionParticles;
    private final List<Integer> optionParticleCounts;

    // Layout parameters
    private final int boxWidth;
    private final int boxHeight;
    private final int titleHeight;
    private final int boxPadding;
    private final int portraitSize;
    private final int buttonWidth;
    private final int optionsHeight;

    // Colors
    private final int backgroundColor;
    private final int titleColor;
    private final int borderColor;
    private final int titleTextColor;

    // Texture
    private Identifier loadedTexture;

    // Button positions
    private int buttonBoxX;
    private int buttonBoxY;

    public DialogScreen(UUID npcUuid, String dialogTitle, String dialogText,
                        String npcTexture, String textureType, String customTextureData,
                        String npcName, List<String> optionLabels,
                        List<String> optionSounds, List<String> optionParticles, List<Integer> optionParticleCounts,
                        int backgroundColor, int titleColor, int buttonWidth,
                        int borderColor, int titleTextColor, int optionsHeight,
                        int boxWidth, int boxHeight, int titleHeight,
                        int boxPadding, int portraitSize) {
        super(Text.literal(dialogTitle));
        this.npcUuid               = npcUuid;
        this.dialogTitle           = dialogTitle;
        this.dialogText            = dialogText;
        this.npcName               = npcName != null ? npcName : "";
        this.optionLabels          = optionLabels;
        this.optionSounds          = optionSounds;
        this.optionParticles       = optionParticles;
        this.optionParticleCounts  = optionParticleCounts;
        this.backgroundColor       = backgroundColor;
        this.titleColor            = titleColor;
        this.buttonWidth           = buttonWidth;
        this.borderColor           = borderColor;
        this.titleTextColor        = titleTextColor;
        this.optionsHeight         = optionsHeight;
        this.boxWidth              = boxWidth > 0 ? boxWidth : 280;
        this.boxHeight             = boxHeight;
        this.titleHeight           = titleHeight > 0 ? titleHeight : 24;
        this.boxPadding            = boxPadding > 0 ? boxPadding : 10;
        this.portraitSize          = portraitSize > 0 ? portraitSize : 40;

        // Load texture using shared TextureLoader utility
        this.loadedTexture = TextureLoader.loadTexture(
            textureType != null ? textureType : "vanilla",
            customTextureData != null ? customTextureData : "",
            Identifier.tryParse(npcTexture)
        );
    }

    @Override
    protected void init() {
        int boxX   = this.width  / 2 - boxWidth / 2;
        int boxTop = calcBoxTop();

        // Store box position for custom rendering
        buttonBoxX = boxX;

        // Calculate options area height (custom or auto)
        int optionsAreaH = optionsHeight > 0 ? optionsHeight :
            (optionLabels.size() + 1) * (BTN_HEIGHT + BTN_GAP) + BTN_GAP;
        buttonBoxY = boxTop + titleHeight + TEXT_BOX_H + boxPadding * 2 + 4;

        // Center buttons within the box
        int effectiveBtnWidth = buttonWidth > 0 ? buttonWidth : DEFAULT_BTN_WIDTH;
        int btnOffset = (boxWidth - effectiveBtnWidth) / 2;

        for (int i = 0; i < optionLabels.size(); i++) {
            final int idx = i;
            final String soundId = optionSounds.size() > idx ? optionSounds.get(idx) : null;
            final String particleType = optionParticles.size() > idx ? optionParticles.get(idx) : null;
            final int particleCount = optionParticleCounts.size() > idx ? optionParticleCounts.get(idx) : 0;

            this.addDrawableChild(
                ButtonWidget.builder(Text.literal(optionLabels.get(i)), btn -> {
                    playOptionSound(soundId);
                    spawnOptionParticles(particleType, particleCount);
                    ModPackets.sendRunOption(npcUuid, idx);
                    this.close();
                })
                .dimensions(buttonBoxX + btnOffset, buttonBoxY + i * (BTN_HEIGHT + BTN_GAP), effectiveBtnWidth, BTN_HEIGHT)
                .build()
            );
        }

        int closeBtnY = buttonBoxY + optionLabels.size() * (BTN_HEIGHT + BTN_GAP);
        this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("dialognpc.dialog.close"), btn -> this.close())
                .dimensions(buttonBoxX + btnOffset, closeBtnY, effectiveBtnWidth, BTN_HEIGHT)
                .build()
        );
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int boxX   = buttonBoxX;
        int boxTop = calcBoxTop();

        // Calculate full modal height
        int buttonsH = (optionLabels.size() + 1) * (BTN_HEIGHT + BTN_GAP);
        int fullHeight = (buttonBoxY - boxTop) + buttonsH;

        // 1. Draw solid background
        ctx.fill(boxX, boxTop, boxX + boxWidth, boxTop + fullHeight, backgroundColor);

        // 2. Draw border
        drawModalBorder(ctx, boxX, boxTop, fullHeight);

        // 3. Draw title bar
        ctx.fill(boxX, boxTop, boxX + boxWidth, boxTop + titleHeight, titleColor);

        // 4. Draw separator line
        ctx.fill(boxX, buttonBoxY - 1, boxX + boxWidth, buttonBoxY, borderColor);

        // 5. Draw title, NPC name, portrait and dialog text
        drawDialogForeground(ctx, boxX, boxTop);

        // 6. Render buttons on top
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(ctx, mouseX, mouseY, delta);
            }
        }
    }

    private void drawModalBorder(DrawContext ctx, int boxX, int boxTop, int fullHeight) {
        ctx.fill(boxX - 1, boxTop - 1, boxX + boxWidth + 1, boxTop, borderColor);
        ctx.fill(boxX - 1, boxTop + fullHeight, boxX + boxWidth + 1, boxTop + fullHeight + 1, borderColor);
        ctx.fill(boxX - 1, boxTop - 1, boxX, boxTop + fullHeight + 1, borderColor);
        ctx.fill(boxX + boxWidth, boxTop - 1, boxX + boxWidth + 1, boxTop + fullHeight + 1, borderColor);
    }

    private void drawDialogForeground(DrawContext ctx, int boxX, int boxTop) {
        int cx = this.width / 2;

        // Draw NPC name above head (if set)
        int nameY = boxTop + titleHeight + boxPadding - 12;
        if (!npcName.isEmpty()) {
            int nameWidth = this.textRenderer.getWidth(npcName);
            int px = boxX + boxPadding;
            ctx.drawText(this.textRenderer, npcName, px + (portraitSize - nameWidth) / 2, nameY, MinecraftColors.GOLD, true);
        }

        // Title text - centered with shadow
        ctx.drawCenteredTextWithShadow(this.textRenderer, dialogTitle,
            cx, boxTop + (titleHeight - 8) / 2, titleTextColor);

        // Portrait background
        int px = boxX + boxPadding;
        int py = boxTop + titleHeight + boxPadding;
        ctx.fill(px - 2, py - 2, px + portraitSize + 2, py + portraitSize + 2, 0xFF404080);
        ctx.fill(px, py, px + portraitSize, py + portraitSize, 0xFF202040);

        // Draw NPC head texture - use loaded texture (handles player skins correctly)
        ctx.drawTexture(loadedTexture, px, py, portraitSize, portraitSize, 8, 8, 8, 8, 64, 64);
        // Hat overlay
        ctx.drawTexture(loadedTexture, px, py, portraitSize, portraitSize, 40, 8, 8, 8, 64, 64);

        // Dialog text area - to the right of portrait
        int tx   = px + portraitSize + boxPadding;
        int ty   = py + 1;
        int maxW = boxWidth - portraitSize - boxPadding * 3;

        // Wrap and draw text line by line
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(dialogText), maxW);
        for (OrderedText line : lines) {
            ctx.drawText(this.textRenderer, line, tx, ty, COLOR_TEXT, true);
            ty += this.textRenderer.fontHeight + 1;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int calcBoxTop() {
        int buttonsH = (optionLabels.size() + 1) * (BTN_HEIGHT + BTN_GAP);
        int dialogH  = titleHeight + TEXT_BOX_H + boxPadding * 2;
        return this.height / 2 - (dialogH + buttonsH + 4) / 2;
    }

    private void playOptionSound(String soundId) {
        if (soundId == null || soundId.isEmpty()) return;
        try {
            var soundEvent = net.minecraft.registry.Registries.SOUND_EVENT.get(net.minecraft.util.Identifier.tryParse(soundId));
            if (soundEvent != null) {
                MinecraftClient.getInstance().player.playSound(soundEvent, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            // Ignore invalid sound IDs
        }
    }

    private void spawnOptionParticles(String particleType, int count) {
        if (particleType == null || particleType.isEmpty() || count <= 0) return;
        try {
            var particle = net.minecraft.registry.Registries.PARTICLE_TYPE.get(net.minecraft.util.Identifier.tryParse(particleType));
            if (particle == null) return;

            var client = MinecraftClient.getInstance();
            var player = client.player;
            if (player == null) return;

            var random = client.world.random;
            for (int i = 0; i < count; i++) {
                double x = player.getX() + (random.nextFloat() - 0.5) * 1.5;
                double y = player.getEyeY() + random.nextFloat() * 0.5;
                double z = player.getZ() + (random.nextFloat() - 0.5) * 1.5;
                double vx = (random.nextFloat() - 0.5) * 0.3;
                double vy = random.nextFloat() * 0.3;
                double vz = (random.nextFloat() - 0.5) * 0.3;
                client.particleManager.addParticle((net.minecraft.particle.ParticleEffect) particle, x, y, z, vx, vy, vz);
            }
        } catch (Exception e) {
            // Ignore invalid particle types
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
