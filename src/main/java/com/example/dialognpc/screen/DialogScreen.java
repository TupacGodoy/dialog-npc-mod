package com.example.dialognpc.screen;

import com.example.dialognpc.network.ModPackets;
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
    private static final int BOX_WIDTH    = 240;
    private static final int BOX_PADDING  = 10;
    private static final int TITLE_HEIGHT = 20;
    private static final int PORTRAIT_SZ  = 36;
    private static final int BTN_HEIGHT   = 20;
    private static final int BTN_GAP      = 4;
    private static final int TEXT_BOX_H   = 70;

    // ── Colors ───────────────────────────────────────────────────────────
    private static final int COLOR_BG        = 0xE0101010;
    private static final int COLOR_TITLE_BAR = 0xFF3D3D7A;
    private static final int COLOR_TITLE_TXT = 0xFFFFD966;
    private static final int COLOR_BORDER    = 0xFF7070CC;
    private static final int COLOR_TEXT      = 0xFFFFFFFF;

    // ── Data ─────────────────────────────────────────────────────────────
    private final UUID         npcUuid;
    private final String       dialogTitle;
    private final String       dialogText;
    private final Identifier   npcTexture;
    private final List<String> optionLabels;

    public DialogScreen(UUID npcUuid, String dialogTitle, String dialogText,
                        String npcTexture, List<String> optionLabels) {
        super(Text.literal(dialogTitle));
        this.npcUuid      = npcUuid;
        this.dialogTitle  = dialogTitle;
        this.dialogText   = dialogText;
        this.npcTexture   = parseTexture(npcTexture);
        this.optionLabels = optionLabels;
    }

    private static Identifier parseTexture(String tex) {
        Identifier id = Identifier.tryParse(tex);
        return id != null ? id : Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    protected void init() {
        int boxX   = this.width  / 2 - BOX_WIDTH / 2;
        int boxTop = calcBoxTop();
        int btnY   = boxTop + TITLE_HEIGHT + TEXT_BOX_H + BOX_PADDING * 2 + 4;

        for (int i = 0; i < optionLabels.size(); i++) {
            final int idx = i;
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal(optionLabels.get(i)), btn -> {
                    ModPackets.sendRunOption(npcUuid, idx);
                    this.close();
                })
                .dimensions(boxX, btnY + i * (BTN_HEIGHT + BTN_GAP), BOX_WIDTH, BTN_HEIGHT)
                .build()
            );
        }

        int closeBtnY = btnY + optionLabels.size() * (BTN_HEIGHT + BTN_GAP);
        this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("dialognpc.dialog.close"), btn -> this.close())
                .dimensions(boxX, closeBtnY, BOX_WIDTH, BTN_HEIGHT)
                .build()
        );
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 1.21.1: renderBackground no longer takes mouse coords
        super.renderBackground(ctx, mouseX, mouseY, delta);
        drawDialog(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawDialog(DrawContext ctx) {
        int cx     = this.width  / 2;
        int boxX   = cx - BOX_WIDTH / 2;
        int boxTop = calcBoxTop();

        // Title bar
        fill(ctx, boxX, boxTop, BOX_WIDTH, TITLE_HEIGHT, COLOR_TITLE_BAR);
        border(ctx, boxX, boxTop, BOX_WIDTH, TITLE_HEIGHT, COLOR_BORDER);
        ctx.drawCenteredTextWithShadow(this.textRenderer, dialogTitle,
            cx, boxTop + (TITLE_HEIGHT - 8) / 2, COLOR_TITLE_TXT);

        // Text area
        int textTop = boxTop + TITLE_HEIGHT;
        int textH   = TEXT_BOX_H + BOX_PADDING * 2;
        fill(ctx, boxX, textTop, BOX_WIDTH, textH, COLOR_BG);
        border(ctx, boxX, textTop, BOX_WIDTH, textH, COLOR_BORDER);

        // Portrait
        int px = boxX + BOX_PADDING;
        int py = textTop + BOX_PADDING;
        fill(ctx, px - 2, py - 2, PORTRAIT_SZ + 4, PORTRAIT_SZ + 4, COLOR_BORDER);
        fill(ctx, px, py, PORTRAIT_SZ, PORTRAIT_SZ, 0xFF202040);
        // Head (skin UV 8,8 → 8×8 on 64×64 sheet)
        ctx.drawTexture(npcTexture, px, py, PORTRAIT_SZ, PORTRAIT_SZ, 8, 8, 8, 8, 64, 64);
        // Hat overlay
        ctx.drawTexture(npcTexture, px, py, PORTRAIT_SZ, PORTRAIT_SZ, 40, 8, 8, 8, 64, 64);

        // Wrapped text
        int tx   = px + PORTRAIT_SZ + BOX_PADDING;
        int ty   = py;
        int maxW = BOX_WIDTH - PORTRAIT_SZ - BOX_PADDING * 3;
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(dialogText), maxW);
        for (OrderedText line : lines) {
            ctx.drawText(this.textRenderer, line, tx, ty, COLOR_TEXT, true);
            ty += this.textRenderer.fontHeight + 2;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private int calcBoxTop() {
        int buttonsH = (optionLabels.size() + 1) * (BTN_HEIGHT + BTN_GAP);
        int dialogH  = TITLE_HEIGHT + TEXT_BOX_H + BOX_PADDING * 2;
        return this.height / 2 - (dialogH + buttonsH + 4) / 2;
    }

    private static void fill(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    @Override
    public boolean shouldPause() { return false; }
}
