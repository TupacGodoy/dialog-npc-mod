package com.example.dialognpc.screen;

import com.example.dialognpc.network.ModPackets;
import com.example.dialognpc.util.MinecraftColors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

    // ── Layout ───────────────────────────────────────────────────────────
    private static final int DEFAULT_BOX_WIDTH   = 280;
    private static final int BOX_PADDING         = 10;
    private static final int TITLE_HEIGHT        = 24;
    private static final int PORTRAIT_SZ         = 40;
    private static final int BTN_HEIGHT          = 20;
    private static final int BTN_GAP             = 4;
    private static final int TEXT_BOX_H          = 90;
    private static final int DEFAULT_BTN_WIDTH   = 180;

    // ── Default Colors (Minecraft) ───────────────────────────────────────
    private static final int DEFAULT_BG_COLOR    = MinecraftColors.DARK_BLUE;
    private static final int DEFAULT_TITLE_COLOR = MinecraftColors.GOLD;
    private static final int DEFAULT_BORDER_COLOR = MinecraftColors.GRAY;
    private static final int COLOR_TITLE_TXT     = MinecraftColors.YELLOW;
    private static final int COLOR_TEXT          = MinecraftColors.WHITE;
    private static final int DEFAULT_OPTIONS_HEIGHT = 0; // auto

    // ── Data ─────────────────────────────────────────────────────────────
    private final UUID         npcUuid;
    private final String       dialogTitle;
    private final String       dialogText;
    private final Identifier   npcTexture;
    private final List<String> optionLabels;
    private final int          backgroundColor;
    private final int          titleColor;
    private final int          buttonWidth;
    private final int          borderColor;
    private final int          titleTextColor;
    private final int          optionsHeight;

    // Button positions
    private int buttonBoxX;
    private int buttonBoxY;

    public DialogScreen(UUID npcUuid, String dialogTitle, String dialogText,
                        String npcTexture, List<String> optionLabels,
                        int backgroundColor, int titleColor, int buttonWidth,
                        int borderColor, int titleTextColor, int optionsHeight) {
        super(Text.literal(dialogTitle));
        this.npcUuid          = npcUuid;
        this.dialogTitle      = dialogTitle;
        this.dialogText       = dialogText;
        this.npcTexture       = parseTexture(npcTexture);
        this.optionLabels     = optionLabels;
        this.backgroundColor  = backgroundColor;
        this.titleColor       = titleColor;
        this.buttonWidth      = buttonWidth;
        this.borderColor      = borderColor;
        this.titleTextColor   = titleTextColor;
        this.optionsHeight    = optionsHeight;
    }

    private static Identifier parseTexture(String tex) {
        Identifier id = Identifier.tryParse(tex);
        return id != null ? id : Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    protected void init() {
        int boxX   = this.width  / 2 - DEFAULT_BOX_WIDTH / 2;
        int boxTop = calcBoxTop();

        // Store box position for custom rendering
        buttonBoxX = boxX;
        // Calculate options area height (custom or auto)
        int optionsAreaH = optionsHeight > 0 ? optionsHeight :
            (optionLabels.size() + 1) * (BTN_HEIGHT + BTN_GAP) + BTN_GAP;
        buttonBoxY = boxTop + TITLE_HEIGHT + TEXT_BOX_H + BOX_PADDING * 2 + 4;

        // Center buttons within the box - use DEFAULT_BTN_WIDTH if buttonWidth not set
        int effectiveBtnWidth = buttonWidth > 0 ? buttonWidth : DEFAULT_BTN_WIDTH;
        int btnOffset = (DEFAULT_BOX_WIDTH - effectiveBtnWidth) / 2;

        for (int i = 0; i < optionLabels.size(); i++) {
            final int idx = i;
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal(optionLabels.get(i)), btn -> {
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

        // Calculate full modal height - from top to bottom of last button
        int buttonsH = (optionLabels.size() + 1) * (BTN_HEIGHT + BTN_GAP);
        int fullHeight = (buttonBoxY - boxTop) + buttonsH;

        // 1. Draw solid background for ENTIRE modal (text + buttons area) - opaque
        ctx.fill(boxX, boxTop, boxX + DEFAULT_BOX_WIDTH, boxTop + fullHeight, backgroundColor);

        // 2. Draw border around entire modal
        drawModalBorder(ctx, boxX, boxTop, fullHeight);

        // 3. Draw title bar with custom color
        ctx.fill(boxX, boxTop, boxX + DEFAULT_BOX_WIDTH, boxTop + TITLE_HEIGHT, titleColor);

        // 4. Draw separator line below text area (at top of button area)
        ctx.fill(boxX, buttonBoxY - 1, boxX + DEFAULT_BOX_WIDTH, buttonBoxY, DEFAULT_BORDER_COLOR);

        // 5. Draw title, portrait and dialog text
        drawDialogForeground(ctx, boxX, boxTop);

        // 6. Render buttons on top (without screen background blur)
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(ctx, mouseX, mouseY, delta);
            }
        }
    }

    private void drawModalBorder(DrawContext ctx, int boxX, int boxTop, int fullHeight) {
        // Border - brighter outline (1 pixel on each side)
        ctx.fill(boxX - 1, boxTop - 1, boxX + DEFAULT_BOX_WIDTH + 1, boxTop, borderColor);
        ctx.fill(boxX - 1, boxTop + fullHeight, boxX + DEFAULT_BOX_WIDTH + 1, boxTop + fullHeight + 1, borderColor);
        ctx.fill(boxX - 1, boxTop - 1, boxX, boxTop + fullHeight + 1, borderColor);
        ctx.fill(boxX + DEFAULT_BOX_WIDTH, boxTop - 1, boxX + DEFAULT_BOX_WIDTH + 1, boxTop + fullHeight + 1, borderColor);
    }

    private void drawDialogForeground(DrawContext ctx, int boxX, int boxTop) {
        int cx = this.width / 2;

        // Title text - centered with shadow
        ctx.drawCenteredTextWithShadow(this.textRenderer, dialogTitle,
            cx, boxTop + (TITLE_HEIGHT - 8) / 2, titleTextColor);

        // Portrait background
        int px = boxX + BOX_PADDING;
        int py = boxTop + TITLE_HEIGHT + BOX_PADDING;
        ctx.fill(px - 2, py - 2, px + PORTRAIT_SZ + 2, py + PORTRAIT_SZ + 2, 0xFF404080);
        ctx.fill(px, py, px + PORTRAIT_SZ, py + PORTRAIT_SZ, 0xFF202040);

        // Draw NPC head texture
        ctx.drawTexture(npcTexture, px, py, PORTRAIT_SZ, PORTRAIT_SZ, 8, 8, 8, 8, 64, 64);
        // Hat overlay
        ctx.drawTexture(npcTexture, px, py, PORTRAIT_SZ, PORTRAIT_SZ, 40, 8, 8, 8, 64, 64);

        // Dialog text area - to the right of portrait
        int tx   = px + PORTRAIT_SZ + BOX_PADDING;
        int ty   = py + 1;
        int maxW = DEFAULT_BOX_WIDTH - PORTRAIT_SZ - BOX_PADDING * 3;

        // Wrap and draw text line by line
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(dialogText), maxW);
        for (OrderedText line : lines) {
            ctx.drawText(this.textRenderer, line, tx, ty, COLOR_TEXT, true);
            ty += this.textRenderer.fontHeight + 1;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private int calcBoxTop() {
        int buttonsH = (optionLabels.size() + 1) * (BTN_HEIGHT + BTN_GAP);
        int dialogH  = TITLE_HEIGHT + TEXT_BOX_H + BOX_PADDING * 2;
        return this.height / 2 - (dialogH + buttonsH + 4) / 2;
    }

    @Override
    public boolean shouldPause() { return false; }
}
