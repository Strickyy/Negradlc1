package dev.negradlc.ui;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Drag panels by their body and resize from the lower-right corner. */
public final class HudEditorScreen extends Screen {
    private final ClientConfig config;
    private final Runnable save;
    private int selected = -1;
    private boolean resizing;
    private double dragX;
    private double dragY;

    public HudEditorScreen(ClientConfig config, Runnable save) {
        super(Text.literal("Negradlc HUD Editor"));
        this.config = config;
        this.save = save;
    }

    @Override
    protected void init() {
        for (int widget = 0; widget < 3; widget++) clamp(widget);
    }

    @Override
    public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
        draw.fill(0, 0, width, height, 0xA510101A);
        for (int x = 0; x < width; x += 20) draw.fill(x, 0, x + 1, height, 0x102F2B45);
        for (int y = 0; y < height; y += 20) draw.fill(0, y, width, y + 1, 0x102F2B45);
        draw.drawCenteredTextWithShadow(textRenderer, "NEGRADLC  /  HUD EDITOR", width / 2, 12, HudRenderer.WHITE);
        draw.drawCenteredTextWithShadow(textRenderer, "Drag panels to move • Drag a corner to resize • Esc to save", width / 2, height - 19, HudRenderer.MUTED);
        int visible = 0;
        for (int widget = 0; widget < 3; widget++) {
            if (!HudRenderer.enabled(config, widget)) continue;
            visible++;
            clamp(widget);
            double x = HudRenderer.x(config, widget);
            double y = HudRenderer.y(config, widget);
            double scale = HudRenderer.scale(config, widget);
            int w = (int) (HudRenderer.width(widget) * scale);
            int h = (int) (HudRenderer.height(client, config, widget) * scale);
            HudRenderer.renderWidget(draw, client, config, null, widget, x, y, scale, true);
            boolean hover = contains(mouseX, mouseY, x, y, w, h);
            int color = widget == selected || hover ? HudRenderer.ACCENT : 0x994D4764;
            draw.drawBorder((int) x - 1, (int) y - 1, w + 2, h + 2, color);
            draw.fill((int) x + w - 6, (int) y + h - 6, (int) x + w + 1, (int) y + h + 1, color);
            if (widget == selected) {
                String label = HudRenderer.name(widget) + "  " + Math.round(scale * 100) + "%";
                int labelY = y < 14 ? (int) y + h + 5 : (int) y - 12;
                draw.drawTextWithShadow(textRenderer, label, (int) x, labelY, HudRenderer.ACCENT);
            }
        }
        if (visible == 0) draw.drawCenteredTextWithShadow(textRenderer,
                "Enable widgets in Visuals to edit their positions", width / 2, height / 2, HudRenderer.MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        selected = -1;
        for (int widget = 2; widget >= 0; widget--) {
            if (!HudRenderer.enabled(config, widget)) continue;
            double x = HudRenderer.x(config, widget);
            double y = HudRenderer.y(config, widget);
            double scale = HudRenderer.scale(config, widget);
            double w = HudRenderer.width(widget) * scale;
            double h = HudRenderer.height(client, config, widget) * scale;
            if (!contains(mouseX, mouseY, x - 2, y - 2, w + 4, h + 4)) continue;
            selected = widget;
            resizing = mouseX >= x + w - 10 && mouseY >= y + h - 10;
            dragX = mouseX - x;
            dragY = mouseY - y;
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || selected < 0) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        double x = HudRenderer.x(config, selected);
        double y = HudRenderer.y(config, selected);
        double scale = HudRenderer.scale(config, selected);
        if (resizing) {
            double sx = (mouseX - x) / HudRenderer.width(selected);
            double sy = (mouseY - y) / HudRenderer.height(client, config, selected);
            scale = MathHelper.clamp(Math.max(sx, sy), 0.6, 2.0);
        } else {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }
        HudRenderer.layout(config, selected, x, y, scale);
        clamp(selected);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { resizing = false; selected = -1; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clamp(int widget) {
        double scale = MathHelper.clamp(HudRenderer.scale(config, widget), 0.6, 2.0);
        double x = MathHelper.clamp(HudRenderer.x(config, widget), 0,
                Math.max(0, width - HudRenderer.width(widget) * scale));
        double y = MathHelper.clamp(HudRenderer.y(config, widget), 0,
                Math.max(0, height - HudRenderer.height(client, config, widget) * scale));
        HudRenderer.layout(config, widget, x, y, scale);
    }

    private static boolean contains(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void close() {
        save.run();
        if (client != null) client.setScreen(new NegradlcScreen(config, save));
    }

    @Override
    public boolean shouldPause() { return false; }
}
