package dev.negradlc.ui;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Dependency-free Click GUI. Positions and sizes persist in the client configuration. */
public final class NegradlcScreen extends Screen {
    private static final int BASE = 0xF211141D;
    private static final int PANEL = 0xFF191D29;
    private static final int ROW = 0xFF202534;
    private static final int ACCENT = 0xFFAA8AFF;
    private static final int WHITE = 0xFFF0EDF8;
    private static final int MUTED = 0xFF8F98AD;
    private static final String[] CATEGORIES = {"Combat", "Movement", "Visuals", "Settings"};
    private final ClientConfig config;
    private final Runnable save;
    private final List<Control> controls = new ArrayList<>();
    private int category;
    private double scroll;
    private boolean dragging;
    private boolean resizing;
    private double dragX, dragY;
    private Control activeSlider;
    private String pendingBind;
    private String notice;
    private long noticeUntil;
    private int x, y, w, h, contentX, contentY, contentWidth, contentHeight;
    private double maxScroll;

    public NegradlcScreen(ClientConfig config, Runnable save) {
        super(Text.literal("Negradlc"));
        this.config = config;
        this.save = save;
    }

    @Override
    protected void init() {
        if (config.guiX < 0 || config.guiY < 0) {
            config.guiX = (width - Math.min(config.guiWidth, width - 12)) / 2;
            config.guiY = (height - Math.min(config.guiHeight, height - 12)) / 2;
        }
        constrainWindow();
        buildControls();
    }

    private void constrainWindow() {
        config.guiWidth = MathHelper.clamp(config.guiWidth, Math.min(430, width - 12), Math.max(220, width - 12));
        config.guiHeight = MathHelper.clamp(config.guiHeight, Math.min(280, height - 12), Math.max(180, height - 12));
        config.guiX = MathHelper.clamp(config.guiX, 6, Math.max(6, width - config.guiWidth - 6));
        config.guiY = MathHelper.clamp(config.guiY, 6, Math.max(6, height - config.guiHeight - 6));
        x = (int) config.guiX;
        y = (int) config.guiY;
        w = (int) config.guiWidth;
        h = (int) config.guiHeight;
        contentX = x + 120;
        contentY = y + 68;
        contentWidth = w - 136;
        contentHeight = h - 99;
    }

    private void buildControls() {
        controls.clear();
        switch (category) {
            case 0 -> {
                section("AUTOMATION");
                toggle("KillAura", "Attacks the nearest visible selected target", () -> config.killAura, () -> config.killAura = !config.killAura);
                toggle("TriggerBot", "Attack the entity under the crosshair", () -> config.triggerBot, () -> config.triggerBot = !config.triggerBot);
                toggle("Aim Assist", "Smoothly turn towards a selected target", () -> config.aimAssist, () -> config.aimAssist = !config.aimAssist);
                toggle("Crit only", "Wait until a falling critical hit is possible", () -> config.critOnly, () -> config.critOnly = !config.critOnly);
                section("TARGETS");
                toggle("Players", "Allow players as combat targets", () -> config.targetPlayers, () -> config.targetPlayers = !config.targetPlayers);
                toggle("Hostile mobs", "Include monsters (disabled by default)", () -> config.targetHostile, () -> config.targetHostile = !config.targetHostile);
                toggle("Passive mobs", "Include animals (disabled by default)", () -> config.targetPassive, () -> config.targetPassive = !config.targetPassive);
                toggle("Ignore friends", "Exclude names in your friend list", () -> config.ignoreFriends, () -> config.ignoreFriends = !config.ignoreFriends);
                action("Manage friends", "Add or remove player names", () -> {
                    save.run();
                    if (client != null) client.setScreen(new FriendsScreen(config, save));
                });
                section("KILLAURA / TRIGGERBOT");
                slider("Attack range", "Maximum melee distance", 1, 3, 0.1, () -> config.attackRange, v -> config.attackRange = v, " blocks");
                slider("Attack cooldown", "Minimum vanilla attack recharge", 0.1, 1, 0.01, () -> config.attackCooldown, v -> config.attackCooldown = v, "");
                section("AIM ASSIST");
                toggle("Hold attack to aim", "Only assist while the attack key is held", () -> config.requireAttackKey, () -> config.requireAttackKey = !config.requireAttackKey);
                slider("Aim range", "Distance for aim target selection", 1, 8, 0.1, () -> config.aimRange, v -> config.aimRange = v, " blocks");
                slider("Aim field of view", "Maximum angle from your crosshair", 10, 180, 1, () -> config.aimFov, v -> config.aimFov = v, " deg");
                slider("Aim speed", "Maximum turn per client tick", 0.5, 20, 0.5, () -> config.aimSpeed, v -> config.aimSpeed = v, " deg/tick");
            }
            case 1 -> {
                section("MOVEMENT");
                toggle("Sprint", "Automatically sprint while moving forward", () -> config.sprint, () -> config.sprint = !config.sprint);
                toggle("AirStag", "Hold your altitude while airborne", () -> config.airStag, () -> config.airStag = !config.airStag);
                toggle("AirStrafe", "Directional control while in the air", () -> config.airStrafe, () -> config.airStrafe = !config.airStrafe);
                slider("Air control", "Strength of airborne directional input", 0.01, 0.2, 0.01, () -> config.airControl, v -> config.airControl = v, "");
                section("TIP");
                note("Bind your modules", "Set individual hotkeys in Settings.");
                note("Server movement", "Server rules can limit airborne movement.");
            }
            case 2 -> {
                section("DISPLAY");
                toggle("HUD", "Show all enabled overlay widgets", () -> config.hud, () -> config.hud = !config.hud);
                toggle("Watermark", "Client identity and performance", () -> config.watermark, () -> config.watermark = !config.watermark);
                toggle("Module list", "Display your active modules", () -> config.moduleList, () -> config.moduleList = !config.moduleList);
                toggle("Target HUD", "Current combat target and health", () -> config.targetHud, () -> config.targetHud = !config.targetHud);
                toggle("Fullbright", "Increase brightness in dark areas", () -> config.fullbright, () -> config.fullbright = !config.fullbright);
                section("LAYOUT");
                action("Edit HUD layout", "Drag widgets and resize them using the corner", () -> {
                    save.run();
                    if (client != null) client.setScreen(new HudEditorScreen(config, save));
                });
            }
            case 3 -> {
                section("MODULE HOTKEYS");
                bind("KillAura", "killAura");
                bind("TriggerBot", "triggerBot");
                bind("Aim Assist", "aimAssist");
                bind("Sprint", "sprint");
                bind("AirStag", "airStag");
                bind("AirStrafe", "airStrafe");
                bind("Fullbright", "fullbright");
                bind("HUD", "hud");
                section("WORKSPACE");
                action("Center window", "Restore the default menu size and position", () -> {
                    config.guiWidth = 540;
                    config.guiHeight = 350;
                    config.guiX = (width - Math.min(540, width - 12)) / 2.0;
                    config.guiY = (height - Math.min(350, height - 12)) / 2.0;
                    constrainWindow();
                });
                note("Right Shift", "Open this menu. Esc closes and saves.");
            }
            default -> throw new IllegalStateException("Unknown category");
        }
    }

    private void section(String text) { controls.add(Control.section(text)); }
    private void note(String title, String description) { controls.add(new Control(Kind.NOTE, title, description)); }
    private void toggle(String title, String description, BooleanSupplier state, Runnable action) {
        Control c = new Control(Kind.TOGGLE, title, description); c.state = state; c.action = action; controls.add(c);
    }
    private void action(String title, String description, Runnable action) {
        Control c = new Control(Kind.ACTION, title, description); c.action = action; controls.add(c);
    }
    private void slider(String title, String description, double min, double max, double step, DoubleSupplier value, DoubleConsumer setter, String suffix) {
        Control c = new Control(Kind.SLIDER, title, description);
        c.min = min; c.max = max; c.step = step; c.value = value; c.setter = setter; c.suffix = suffix;
        controls.add(c);
    }
    private void bind(String title, String id) {
        Control c = new Control(Kind.BIND, title, "Click, then press a key. Delete clears."); c.bind = id; controls.add(c);
    }

    private void layoutControls() {
        int total = controls.stream().mapToInt(Control::height).sum();
        maxScroll = Math.max(0, total - contentHeight);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);
        int cursorY = contentY - (int) scroll;
        for (Control c : controls) {
            c.x = contentX; c.y = cursorY; c.width = contentWidth;
            cursorY += c.height();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        constrainWindow();
        layoutControls();
        context.fill(0, 0, width, height, 0x96050910);
        roundRect(context, x - 3, y - 3, w + 6, h + 6, 0x602A2340);
        roundRect(context, x, y, w, h, BASE);
        context.fill(x + 1, y + 1, x + w - 1, y + 3, ACCENT);
        context.fill(x + 1, y + 42, x + w - 1, y + 43, 0xFF2A2F40);
        text(context, "N", x + 15, y + 16, ACCENT);
        text(context, "NEGRADLC", x + 32, y + 13, WHITE);
        text(context, "FABRIC / 1.21.4", x + 32, y + 26, MUTED);
        text(context, "\u00d7", x + w - 21, y + 17, inside(mouseX, mouseY, x + w - 32, y + 7, 26, 29) ? WHITE : MUTED);
        context.fill(x + 108, y + 43, x + 109, y + h - 27, 0xFF272B3B);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int tabY = y + 62 + i * 36;
            boolean hovered = inside(mouseX, mouseY, x + 9, tabY - 6, 91, 29);
            if (i == category || hovered) roundRect(context, x + 9, tabY - 6, 91, 29, i == category ? 0xFF302842 : PANEL);
            if (i == category) context.fill(x + 9, tabY - 3, x + 11, tabY + 20, ACCENT);
            text(context, CATEGORIES[i], x + 22, tabY + 3, i == category ? WHITE : MUTED);
        }
        text(context, CATEGORIES[category].toUpperCase(Locale.ROOT), contentX, y + 51, ACCENT);
        String state = pendingBind == null ? "LIVE CONFIG" : "PRESS A KEY";
        text(context, state, x + w - textRenderer.getWidth(state) - 17, y + 51, pendingBind == null ? MUTED : ACCENT);
        context.enableScissor(contentX, contentY, x + w - 9, contentY + contentHeight);
        for (Control control : controls) if (control.y + control.height() >= contentY && control.y <= contentY + contentHeight) renderControl(context, control, mouseX, mouseY);
        context.disableScissor();
        if (maxScroll > 0) {
            int thumb = Math.max(18, (int) ((double) contentHeight / (contentHeight + maxScroll) * contentHeight));
            int top = contentY + (int) ((contentHeight - thumb) * scroll / maxScroll);
            context.fill(x + w - 7, contentY, x + w - 5, contentY + contentHeight, 0xFF242938);
            context.fill(x + w - 7, top, x + w - 5, top + thumb, 0xFF79659F);
        }
        context.fill(x + 1, y + h - 27, x + w - 1, y + h - 26, 0xFF2A2F40);
        String hint = notice != null && System.currentTimeMillis() < noticeUntil ? notice : pendingBind != null ? "Esc cancels  /  Delete unbinds" : "Drag header to move  /  Drag corner to resize";
        text(context, textRenderer.trimToWidth(hint, Math.max(30, w - 39)), x + 13, y + h - 17, MUTED);
        for (int i = 0; i < 3; i++) context.fill(x + w - 6 - i * 3, y + h - 6, x + w - 5 - i * 3, y + h - 5 - i * 3, ACCENT);
    }

    private void renderControl(DrawContext context, Control c, int mouseX, int mouseY) {
        if (c.kind == Kind.SECTION) { text(context, c.title, c.x + 1, c.y + 9, MUTED); return; }
        boolean hovered = inside(mouseX, mouseY, c.x, c.y, c.width, c.height() - 5) && mouseY >= contentY && mouseY < contentY + contentHeight;
        roundRect(context, c.x, c.y, c.width, c.height() - 5, hovered && c.kind != Kind.NOTE ? 0xFF282E40 : ROW);
        int reserved = switch (c.kind) {
            case TOGGLE -> 49;
            case ACTION -> 28;
            case BIND -> textRenderer.getWidth(c.bind.equals(pendingBind) ? "..." : keyName(config.binds.getOrDefault(c.bind, GLFW.GLFW_KEY_UNKNOWN))) + 20;
            case SLIDER -> textRenderer.getWidth(formattedValue(c)) + 20;
            default -> 10;
        };
        text(context, textRenderer.trimToWidth(c.title, Math.max(6, c.width - reserved - 10)), c.x + 10, c.y + 8, WHITE);
        String description = textRenderer.trimToWidth(c.description, Math.max(24, c.width - 22));
        text(context, description, c.x + 10, c.y + 23, MUTED);
        switch (c.kind) {
            case TOGGLE -> {
                int sx = c.x + c.width - 40;
                roundRect(context, sx, c.y + 7, 29, 13, c.state.getAsBoolean() ? 0xFF8D6CDB : 0xFF424A60);
                roundRect(context, sx + (c.state.getAsBoolean() ? 18 : 3), c.y + 10, 8, 7, c.state.getAsBoolean() ? WHITE : 0xFFB1B8C7);
            }
            case ACTION -> text(context, ">", c.x + c.width - 17, c.y + 8, ACCENT);
            case BIND -> {
                String binding = c.bind.equals(pendingBind) ? "..." : keyName(config.binds.getOrDefault(c.bind, GLFW.GLFW_KEY_UNKNOWN));
                text(context, binding, c.x + c.width - textRenderer.getWidth(binding) - 10, c.y + 8, ACCENT);
            }
            case SLIDER -> {
                String value = formattedValue(c);
                text(context, value, c.x + c.width - textRenderer.getWidth(value) - 10, c.y + 8, ACCENT);
                int sx = c.x + 11, sy = c.y + 44, sw = c.width - 22;
                double fraction = MathHelper.clamp((c.value.getAsDouble() - c.min) / (c.max - c.min), 0, 1);
                context.fill(sx, sy, sx + sw, sy + 3, 0xFF41485C);
                context.fill(sx, sy, sx + (int) (sw * fraction), sy + 3, ACCENT);
                roundRect(context, sx + (int) (sw * fraction) - 2, sy - 2, 5, 7, WHITE);
            }
            default -> { }
        }
    }

    private static String formattedValue(Control c) {
        return String.format(Locale.ROOT, c.step >= 1 ? "%.0f%s" : "%.2f%s", c.value.getAsDouble(), c.suffix);
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == 0) return "NONE";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(Locale.ROOT);
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R ALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            default -> key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25 ? "F" + (key - GLFW.GLFW_KEY_F1 + 1) : "KEY " + key;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (inside(mouseX, mouseY, x + w - 32, y + 7, 26, 29)) { close(); return true; }
        if (inside(mouseX, mouseY, x + w - 16, y + h - 16, 16, 16)) {
            resizing = true; dragX = mouseX - w; dragY = mouseY - h; return true;
        }
        if (inside(mouseX, mouseY, x, y, w, 42)) {
            dragging = true; dragX = mouseX - x; dragY = mouseY - y; return true;
        }
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (inside(mouseX, mouseY, x + 9, y + 56 + i * 36, 91, 29)) {
                category = i; scroll = 0; pendingBind = null; buildControls(); layoutControls(); return true;
            }
        }
        layoutControls();
        if (inside(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
            for (Control c : controls) if (inside(mouseX, mouseY, c.x, c.y, c.width, c.height() - 5)) {
                if (c.kind == Kind.SLIDER) { activeSlider = c; updateSlider(mouseX); return true; }
                if (c.kind == Kind.BIND) { pendingBind = c.bind; return true; }
                if (c.action != null) { c.action.run(); save.run(); return true; }
            }
        }
        return inside(mouseX, mouseY, x, y, w, h) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) { config.guiX = mouseX - dragX; config.guiY = mouseY - dragY; constrainWindow(); return true; }
        if (resizing) { config.guiWidth = mouseX - dragX; config.guiHeight = mouseY - dragY; constrainWindow(); return true; }
        if (activeSlider != null) { updateSlider(mouseX); return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void updateSlider(double mouseX) {
        Control c = activeSlider;
        double position = MathHelper.clamp((mouseX - c.x - 11) / Math.max(1, c.width - 22), 0, 1);
        double value = c.min + position * (c.max - c.min);
        c.setter.accept(MathHelper.clamp(Math.round(value / c.step) * c.step, c.min, c.max));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = dragging || resizing || activeSlider != null;
        dragging = false; resizing = false; activeSlider = null;
        if (handled) save.run();
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inside(mouseX, mouseY, x, y, w, h)) { scroll = MathHelper.clamp(scroll - verticalAmount * 29, 0, maxScroll); return true; }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pendingBind != null) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                notice = "Right Shift is reserved for the menu";
                noticeUntil = System.currentTimeMillis() + 3500;
                return true;
            }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                int binding = keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
                String reassigned = null;
                if (binding != GLFW.GLFW_KEY_UNKNOWN) {
                    for (var entry : config.binds.entrySet()) {
                        if (!entry.getKey().equals(pendingBind) && entry.getValue() != null && entry.getValue() == binding) {
                            reassigned = entry.getKey();
                            entry.setValue(GLFW.GLFW_KEY_UNKNOWN);
                        }
                    }
                }
                config.binds.put(pendingBind, binding);
                if (reassigned != null) {
                    notice = keyName(binding) + " reassigned from " + reassigned;
                    noticeUntil = System.currentTimeMillis() + 3500;
                }
                save.run();
            }
            pendingBind = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { save.run(); if (client != null) client.setScreen(null); }
    @Override
    public boolean shouldPause() { return false; }

    private void text(DrawContext context, String value, int x, int y, int color) { context.drawTextWithShadow(textRenderer, value, x, y, color); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static void roundRect(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x + 2, y, x + w - 2, y + h, color);
        context.fill(x, y + 2, x + w, y + h - 2, color);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    private enum Kind { SECTION, TOGGLE, SLIDER, ACTION, BIND, NOTE }
    private static final class Control {
        final Kind kind;
        final String title, description;
        BooleanSupplier state;
        Runnable action;
        DoubleSupplier value;
        DoubleConsumer setter;
        double min, max, step;
        String suffix, bind;
        int x, y, width;
        Control(Kind kind, String title, String description) { this.kind = kind; this.title = title; this.description = description; }
        static Control section(String title) { return new Control(Kind.SECTION, title, ""); }
        int height() { return kind == Kind.SECTION ? 28 : kind == Kind.SLIDER ? 61 : 46; }
    }
}
