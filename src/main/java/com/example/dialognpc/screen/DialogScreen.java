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
    private static final int DEFAULT_BTN_WIDTH = 180;

    // ── Default Colors ───────────────────────────────────────────────────
    private static final int COLOR_TEXT = MinecraftColors.WHITE;

    // ── Data ──────────────────────────────────────────────────────────────
    private final UUID         npcUuid;
    private final String       dialogTitle;
    private final String       dialogTitleKey;
    private final String       dialogText;
    private final String       dialogTextKey;
    private final String       npcName;
    private final String       npcNameKey;
    private final List<String> optionLabels;
    private final List<String> optionLabelKeys;
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
    private final int optionTextColor;

    // Texture
    private Identifier loadedTexture;

    // Positions
    private int screenCenterX;
    private int dialogBoxX;
    private int dialogBoxY;
    private int buttonBoxY;
    private int npcRenderX;
    private int npcRenderY;

    public DialogScreen(UUID npcUuid, String dialogTitle, String dialogTitleKey,
                        String dialogText, String dialogTextKey,
                        String npcTexture, String textureType, String customTextureData,
                        String npcName, String npcNameKey,
                        List<String> optionLabels, List<String> optionLabelKeys,
                        List<String> optionSounds, List<String> optionParticles, List<Integer> optionParticleCounts,
                        int backgroundColor, int titleColor, int buttonWidth,
                        int borderColor, int titleTextColor, int optionsHeight,
                        int boxWidth, int boxHeight, int titleHeight,
                        int boxPadding, int portraitSize, int optionTextColor) {
        super(Text.literal(dialogTitle));
        this.npcUuid               = npcUuid;
        this.dialogTitle           = dialogTitle;
        this.dialogTitleKey        = dialogTitleKey != null ? dialogTitleKey : "";
        this.dialogText            = dialogText;
        this.dialogTextKey         = dialogTextKey != null ? dialogTextKey : "";
        this.npcName               = npcName != null ? npcName : "";
        this.npcNameKey            = npcNameKey != null ? npcNameKey : "";
        this.optionLabels          = optionLabels;
        this.optionLabelKeys       = optionLabelKeys;
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
        this.optionTextColor       = optionTextColor;

        // Load texture using shared TextureLoader utility
        this.loadedTexture = TextureLoader.loadTexture(
            textureType != null ? textureType : "vanilla",
            customTextureData != null ? customTextureData : "",
            Identifier.tryParse(npcTexture)
        );
    }

    @Override
    protected void init() {
        this.screenCenterX = this.width / 2;

        // Calculate dialog box position (centered horizontally, upper portion of screen)
        this.dialogBoxX = this.screenCenterX - boxWidth / 2;
        this.dialogBoxY = this.height / 6; // Position in upper portion

        // Calculate dialog box total height for positioning
        int dialogBoxTotalHeight = calculateDialogBoxHeight();

        // Calculate NPC render position (to the left of dialog box, facing the text)
        this.npcRenderX = this.dialogBoxX - this.portraitSize - 15; // 15px gap (close to speech bubble)
        this.npcRenderY = this.dialogBoxY + dialogBoxTotalHeight / 2 - this.portraitSize / 2; // Center vertically with dialog

        // Calculate button box position (below dialog box)
        this.buttonBoxY = this.dialogBoxY + dialogBoxTotalHeight + 15; // 15px gap between box and buttons

        // Center buttons within the box
        int effectiveBtnWidth = buttonWidth > 0 ? buttonWidth : DEFAULT_BTN_WIDTH;

        // Calculate button spacing based on optionsHeight
        int totalButtons = optionLabels.size() + 1; // +1 for close button
        int buttonsTotalHeight = totalButtons * BTN_HEIGHT;

        // Determine the actual height available for buttons
        int availableHeight;
        if (optionsHeight > 0) {
            // Use custom height
            availableHeight = optionsHeight;
        } else {
            // Auto height: default spacing
            availableHeight = buttonsTotalHeight + (totalButtons + 1) * BTN_GAP;
        }

        // Calculate gap between buttons to fill available height
        int remainingSpace = availableHeight - buttonsTotalHeight;
        int gap = Math.max(BTN_GAP, remainingSpace / (totalButtons + 1));

        for (int i = 0; i < optionLabels.size(); i++) {
            final int idx = i;
            final String soundId = optionSounds.size() > idx ? optionSounds.get(idx) : null;
            final String particleType = optionParticles.size() > idx ? optionParticles.get(idx) : null;
            final int particleCount = optionParticleCounts.size() > idx ? optionParticleCounts.get(idx) : 0;
            final String labelKey = optionLabelKeys.size() > idx ? optionLabelKeys.get(i) : "";

            Text buttonText = !labelKey.isEmpty() ? Text.translatable(labelKey) : Text.literal(optionLabels.get(i));

            this.addDrawableChild(
                new ColoredButtonWidget(
                    this.screenCenterX - effectiveBtnWidth / 2,
                    this.buttonBoxY + i * (BTN_HEIGHT + gap),
                    effectiveBtnWidth,
                    BTN_HEIGHT,
                    buttonText,
                    btn -> {
                        playOptionSound(soundId);
                        spawnOptionParticles(particleType, particleCount);
                        ModPackets.sendRunOption(npcUuid, idx);
                        this.close();
                    },
                    optionTextColor
                )
            );
        }

        int closeBtnY = this.buttonBoxY + optionLabels.size() * (BTN_HEIGHT + gap);
        this.addDrawableChild(
            new ColoredButtonWidget(
                this.screenCenterX - effectiveBtnWidth / 2,
                closeBtnY,
                effectiveBtnWidth,
                BTN_HEIGHT,
                Text.translatable("dialognpc.dialog.close"),
                btn -> this.close(),
                optionTextColor
            )
        );
    }

    private int calculateDialogBoxHeight() {
        // Calculate text content height
        int maxW = boxWidth - boxPadding * 2;
        Text textContent = !dialogTextKey.isEmpty() ? Text.translatable(dialogTextKey) : Text.literal(dialogText);
        List<OrderedText> lines = this.textRenderer.wrapLines(textContent, maxW);
        int textHeight = lines.size() * (this.textRenderer.fontHeight + 1);

        // Total height: name area + text area + padding
        int nameHeight = !npcName.isEmpty() || !npcNameKey.isEmpty() ? 16 : 0;
        return nameHeight + textHeight + boxPadding * 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 1. Render buttons first (so they appear below the dialog box)
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(ctx, mouseX, mouseY, delta);
            }
        }

        // 2. Calculate dialog box dimensions
        int dialogBoxTotalHeight = calculateDialogBoxHeight();

        // 3. Draw speech bubble tail pointing to NPC
        drawSpeechBubbleTail(ctx, dialogBoxX, dialogBoxY, dialogBoxTotalHeight);

        // 4. Draw solid background for dialog box (sharp corners, no rounding)
        ctx.fill(dialogBoxX, dialogBoxY, dialogBoxX + boxWidth, dialogBoxY + dialogBoxTotalHeight, backgroundColor);

        // 5. Draw border (sharp rectangular border) - draw only left, top, bottom (right side overlaps tail connection)
        drawSharpBorderWithoutLeft(ctx, dialogBoxX, dialogBoxY, boxWidth, dialogBoxTotalHeight);

        // 6. Draw NPC name at top of dialog box (if NPC has a name)
        int yOffset = dialogBoxY;
        if (!npcName.isEmpty() || !npcNameKey.isEmpty()) {
            drawNameBar(ctx, dialogBoxX, dialogBoxY, boxWidth);
            yOffset += 16; // Name bar height
        }

        // 7. Draw dialog text content
        drawDialogText(ctx, dialogBoxX, yOffset, boxWidth, dialogBoxTotalHeight - (yOffset - dialogBoxY));

        // 8. Render NPC (outside the dialog box, facing the text)
        renderNPC(ctx);
    }

    private void renderNPC(DrawContext ctx) {
        // Draw NPC head texture facing the dialog box
        int headSize = portraitSize;

        // Draw head background (border color)
        ctx.fill(npcRenderX - 2, npcRenderY - 2, npcRenderX + headSize + 2, npcRenderY + headSize + 2, borderColor);
        ctx.fill(npcRenderX, npcRenderY, npcRenderX + headSize, npcRenderY + headSize, backgroundColor);

        // Draw NPC head texture
        ctx.drawTexture(loadedTexture, npcRenderX, npcRenderY, headSize, headSize, 8, 8, 8, 8, 64, 64);
        // Hat overlay
        ctx.drawTexture(loadedTexture, npcRenderX, npcRenderY, headSize, headSize, 40, 8, 8, 8, 64, 64);
    }

    private void drawSpeechBubbleTail(DrawContext ctx, int x, int y, int height) {
        // Draw isosceles triangle tail pointing from dialog box to NPC (left side)
        // Triangle that extends beyond the box so it's not clipped
        int tailLength = 12; // Length of triangle (how far it extends left)
        int tailHalfHeight = 10; // Half height of triangle base
        int tailY = y + height / 2;

        // Triangle points (drawn as two triangles to form isosceles)
        // The triangle extends tailLength pixels to the left from the box
        // Draw the triangle filled with background color
        for (int i = 0; i < tailLength; i++) {
            int currentHalfHeight = tailHalfHeight - (i * tailHalfHeight / tailLength);
            ctx.fill(x - tailLength + i, tailY - currentHalfHeight, x - tailLength + i + 1, tailY + currentHalfHeight, backgroundColor);
        }

        // Draw border around triangle (top and bottom edges only - base connects to box)
        // Top edge of triangle
        for (int i = 0; i < tailLength; i++) {
            int currentHalfHeight = tailHalfHeight - (i * tailHalfHeight / tailLength);
            ctx.fill(x - tailLength + i, tailY - currentHalfHeight - 1, x - tailLength + i + 1, tailY - currentHalfHeight, borderColor);
        }
        // Bottom edge of triangle
        for (int i = 0; i < tailLength; i++) {
            int currentHalfHeight = tailHalfHeight - (i * tailHalfHeight / tailLength);
            ctx.fill(x - tailLength + i, tailY + currentHalfHeight, x - tailLength + i + 1, tailY + currentHalfHeight + 1, borderColor);
        }
        // Left tip of triangle (vertical line)
        ctx.fill(x - tailLength - 1, tailY - 1, x - tailLength, tailY + 2, borderColor);
    }

    private void drawSharpBorder(DrawContext ctx, int x, int y, int width, int height) {
        // Top border (3px thick)
        ctx.fill(x - 1, y - 3, x + width + 1, y, borderColor);
        // Bottom border (3px thick)
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 3, borderColor);
        // Left border (3px thick)
        ctx.fill(x - 3, y - 3, x, y + height + 3, borderColor);
        // Right border (3px thick)
        ctx.fill(x + width, y - 3, x + width + 3, y + height + 3, borderColor);
    }

    private void drawSharpBorderWithoutLeft(DrawContext ctx, int x, int y, int width, int height) {
        // Top border (3px thick)
        ctx.fill(x - 1, y - 3, x + width + 1, y, borderColor);
        // Bottom border (3px thick)
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 3, borderColor);
        // Right border only (3px thick) - left side connects to tail
        ctx.fill(x + width, y - 3, x + width + 3, y + height + 3, borderColor);
    }

    private void drawNameBar(DrawContext ctx, int x, int y, int width) {
        // Draw name bar background (using titleColor)
        ctx.fill(x, y, x + width, y + 16, titleColor);

        // Draw NPC name centered in the bar
        Text nameText = !npcNameKey.isEmpty() ? Text.translatable(npcNameKey) : Text.literal(npcName);
        int nameY = y + (16 - 8) / 2;
        ctx.drawCenteredTextWithShadow(this.textRenderer, nameText, x + width / 2, nameY, titleTextColor);
    }

    private void drawDialogText(DrawContext ctx, int x, int y, int width, int height) {
        int tx = x + boxPadding;
        int ty = y + boxPadding;
        int maxW = width - boxPadding * 2;

        // Wrap and draw text line by line
        Text textContent = !dialogTextKey.isEmpty() ? Text.translatable(dialogTextKey) : Text.literal(dialogText);
        List<OrderedText> lines = this.textRenderer.wrapLines(textContent, maxW);
        for (OrderedText line : lines) {
            ctx.drawTextWithShadow(this.textRenderer, line, tx, ty, COLOR_TEXT);
            ty += this.textRenderer.fontHeight + 1;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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

    // ── Custom Button Widget with colored text ────────────────────────────

    public class ColoredButtonWidget extends ButtonWidget {
        private final int textColor;

        public ColoredButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, int textColor) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.textColor = textColor;
        }

        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            // Draw button background (gray colors like original layout)
            ctx.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                this.isHovered() ? 0xFF505050 : 0xFF303030);

            // Draw thicker border (white/gray colors)
            int borderColor = this.isHovered() ? MinecraftColors.WHITE : MinecraftColors.GRAY;
            ctx.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY(), borderColor);
            ctx.fill(this.getX() - 1, this.getY() + this.height, this.getX() + this.width + 1, this.getY() + this.height + 1, borderColor);
            ctx.fill(this.getX() - 1, this.getY() - 1, this.getX(), this.getY() + this.height + 1, borderColor);
            ctx.fill(this.getX() + this.width, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, borderColor);

            // Draw centered text with custom color
            int textX = this.getX() + this.width / 2 - textRenderer.getWidth(this.getMessage()) / 2;
            int textY = this.getY() + (this.height - 8) / 2;
            ctx.drawText(textRenderer, this.getMessage(), textX, textY, this.textColor, true);
        }
    }
}
