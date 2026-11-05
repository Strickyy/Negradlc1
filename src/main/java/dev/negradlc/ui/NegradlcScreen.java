package dev.negradlc.ui;

import dev.negradlc.config.ClientConfig;
import dev.negradlc.input.BindingCodes;
import dev.negradlc.sound.SoundFeedback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/** Compact two-column module workspace. Every enabled feature has its own bind and settings. */
public final class NegradlcScreen extends Screen {
    private static final int ACCENT = 0xFF61D9C0, WHITE = 0xFFE3ECE6, MUTED = 0xFF8B9D91;
    private static final String[] CATEGORIES = {"combat", "move", "visual", "other"};
    private final ClientConfig config;
    private final Runnable save;
    private final List<Card> cards = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>(Set.of("killAura", "aimAssist"));
    private final Map<String, Float> switchAnimation = new HashMap<>();
    private int category, x, y, w, h, sidebar, contentX, contentY, contentWidth, contentHeight;
    private double scroll, maxScroll, dragX, dragY;
    private boolean dragging, resizing;
    private Control slider;
    private String pendingBind, notice;
    private long noticeUntil, frameNanos;
    private float frameBlend = 1;

    public NegradlcScreen(ClientConfig config, Runnable save) {
        super(Text.literal("Negradlc"));
        this.config = config;
        this.save = save;
    }

    @Override protected void init() {
        if (config.guiX < 0 || config.guiY < 0) {
            config.guiX = (width - Math.min(config.guiWidth, width - 12)) / 2;
            config.guiY = (height - Math.min(config.guiHeight, height - 12)) / 2;
        }
        constrain();
        buildCards();
    }

    private void constrain() {
        int availableW = Math.max(180, width - 12), availableH = Math.max(140, height - 12);
        config.guiWidth = MathHelper.clamp(config.guiWidth, Math.min(420, availableW), availableW);
        config.guiHeight = MathHelper.clamp(config.guiHeight, Math.min(270, availableH), availableH);
        config.guiX = MathHelper.clamp(config.guiX, 6, Math.max(6, width - config.guiWidth - 6));
        config.guiY = MathHelper.clamp(config.guiY, 6, Math.max(6, height - config.guiHeight - 6));
        x = (int) config.guiX; y = (int) config.guiY;
        w = (int) config.guiWidth; h = (int) config.guiHeight;
        sidebar = w < 330 ? 66 : 83;
        contentX = x + sidebar + 9; contentY = y + 35;
        contentWidth = Math.max(60, w - sidebar - 20); contentHeight = Math.max(45, h - 61);
    }

    private void buildCards() {
        cards.clear();
        switch (category) {
            case 0 -> {
                Card aura = module("killAura", "KillAura", "Closest eligible target", () -> config.killAura, () -> config.killAura = !config.killAura);
                toggle(aura, "Crit only", "Only attack while falling; grounded hits wait", () -> config.critOnly, () -> config.critOnly = !config.critOnly);
                toggle(aura, "Auto crit jump", "Jump normally when a critical hit is needed", () -> config.autoCritJump, () -> config.autoCritJump = !config.autoCritJump);
                range(aura, "Distance", "Melee reach in blocks", 1, 3, .1, () -> config.attackRange, v -> config.attackRange = v);
                range(aura, "Recharge", "Wait for this fraction of vanilla attack cooldown", .1, 1, .01, () -> config.attackCooldown, v -> config.attackCooldown = v);
                range(aura, "Field of view", "Target selection cone in degrees", 10, 360, 5, () -> config.auraFov, v -> config.auraFov = v);
                Card aim = module("aimAssist", "Aim Assist", "Independent yaw / pitch control", () -> config.aimAssist, () -> config.aimAssist = !config.aimAssist);
                choice(aim, "Aim point", "Body point towards which the camera turns", () -> config.aimPoint, () -> config.aimPoint = next(config.aimPoint, "CHEST", "HEAD", "NEAREST"));
                range(aim, "Distance", "Maximum aiming distance in blocks", 1, 8, .1, () -> config.aimRange, v -> config.aimRange = v);
                range(aim, "Field of view", "Horizontal cone in degrees", 5, 180, 5, () -> config.aimFov, v -> config.aimFov = v);
                range(aim, "Yaw speed", "Maximum horizontal turn per tick", .1, 30, .1, () -> config.aimSpeed, v -> config.aimSpeed = v);
                range(aim, "Pitch speed", "Maximum vertical turn per tick", .1, 30, .1, () -> config.aimPitchSpeed, v -> config.aimPitchSpeed = v);
                range(aim, "Smoothing", "Blend of turn towards the target", .05, 1, .05, () -> config.aimSmoothing, v -> config.aimSmoothing = v);
                toggle(aim, "Hold attack", "Assist only while holding the attack key", () -> config.requireAttackKey, () -> config.requireAttackKey = !config.requireAttackKey);
                toggle(aim, "Visible only", "Ignore targets hidden by blocks when aiming", () -> config.aimVisibleOnly, () -> config.aimVisibleOnly = !config.aimVisibleOnly);
                Card trigger = module("triggerBot", "TriggerBot", "Target must be under the crosshair", () -> config.triggerBot, () -> config.triggerBot = !config.triggerBot);
                toggle(trigger, "Crit only", "Shared with KillAura: waits for a real falling crit", () -> config.critOnly, () -> config.critOnly = !config.critOnly);
                toggle(trigger, "Auto crit jump", "Shared automatic jump for critical attacks", () -> config.autoCritJump, () -> config.autoCritJump = !config.autoCritJump);
                range(trigger, "Recharge", "Shared vanilla attack cooldown threshold", .1, 1, .01, () -> config.attackCooldown, v -> config.attackCooldown = v);
                Card targets = panel("targets", "Targets", "Shared combat filters");
                toggle(targets, "Players", "Allow survival players as targets", () -> config.targetPlayers, () -> config.targetPlayers = !config.targetPlayers);
                toggle(targets, "Hostile mobs", "Include hostile mobs in combat selection", () -> config.targetHostile, () -> config.targetHostile = !config.targetHostile);
                toggle(targets, "Passive mobs", "Include passive mobs in combat selection", () -> config.targetPassive, () -> config.targetPassive = !config.targetPassive);
                toggle(targets, "Ignore friends", "Exclude your configured friends", () -> config.ignoreFriends, () -> config.ignoreFriends = !config.ignoreFriends);
                action(targets, "Manage friends", "Add or remove friend names", () -> { if (client != null) client.setScreen(new FriendsScreen(config, save)); });
            }
            case 1 -> {
                Card stag = module("airStag", "AirStag", "Hover; Jump / Sneak changes height", () -> config.airStag, () -> config.airStag = !config.airStag);
                note(stag, "Jump / Sneak", "Ascend or descend while hovering");
                note(stag, "Crit only", "Hover prevents falling critical hits");
                Card strafe = module("airStrafe", "AirStrafe", "Directional control in the air", () -> config.airStrafe, () -> config.airStrafe = !config.airStrafe);
                range(strafe, "Air control", "Strength of horizontal steering", .01, .2, .01, () -> config.airControl, v -> config.airControl = v);
                Card sprint = module("sprint", "AutoSprint", "Sprint while moving forward", () -> config.sprint, () -> config.sprint = !config.sprint);
                note(sprint, "Smart release", "Combat can stop sprint for critical hits");
            }
            case 2 -> {
                Card esp = module("esp", "ESP", "Entity outlines and information", () -> config.esp, () -> config.esp = !config.esp);
                toggle(esp, "Players", "Show player boxes", () -> config.espPlayers, () -> config.espPlayers = !config.espPlayers);
                toggle(esp, "Hostile mobs", "Show monster boxes", () -> config.espHostile, () -> config.espHostile = !config.espHostile);
                toggle(esp, "Passive mobs", "Show passive mob boxes", () -> config.espPassive, () -> config.espPassive = !config.espPassive);
                toggle(esp, "Through walls", "Draw outlines behind blocks", () -> config.espThroughWalls, () -> config.espThroughWalls = !config.espThroughWalls);
                toggle(esp, "Health", "Show health beside target boxes", () -> config.espHealth, () -> config.espHealth = !config.espHealth);
                toggle(esp, "Names", "Show entity names", () -> config.espNames, () -> config.espNames = !config.espNames);
                range(esp, "Distance", "Maximum ESP distance", 8, 128, 1, () -> config.espRange, v -> config.espRange = v);
                range(esp, "Line width", "Outline thickness", 1, 4, .25, () -> config.espLineWidth, v -> config.espLineWidth = v);
                choice(esp, "Color", "Click to cycle the outline palette", () -> colorName(config.espColor), () -> config.espColor = nextColor(config.espColor));
                Card tracer = module("tracers", "Tracers", "Lines from crosshair to entities", () -> config.tracers, () -> config.tracers = !config.tracers);
                toggle(tracer, "Players", "Draw lines towards players", () -> config.tracerPlayers, () -> config.tracerPlayers = !config.tracerPlayers);
                toggle(tracer, "Mobs", "Draw lines towards mobs", () -> config.tracerMobs, () -> config.tracerMobs = !config.tracerMobs);
                toggle(tracer, "Through walls", "Draw lines behind blocks", () -> config.tracerThroughWalls, () -> config.tracerThroughWalls = !config.tracerThroughWalls);
                range(tracer, "Distance", "Maximum tracer distance", 8, 128, 1, () -> config.tracerRange, v -> config.tracerRange = v);
                range(tracer, "Line width", "Tracer thickness", 1, 4, .25, () -> config.tracerWidth, v -> config.tracerWidth = v);
                choice(tracer, "Color", "Click to cycle the tracer palette", () -> colorName(config.tracerColor), () -> config.tracerColor = nextColor(config.tracerColor));
                Card hud = module("hud", "HUD", "Movable overlays", () -> config.hud, () -> config.hud = !config.hud);
                toggle(hud, "Watermark", "Show client identity", () -> config.watermark, () -> config.watermark = !config.watermark);
                toggle(hud, "Module list", "Show active modules", () -> config.moduleList, () -> config.moduleList = !config.moduleList);
                toggle(hud, "Target HUD", "Show target health and distance", () -> config.targetHud, () -> config.targetHud = !config.targetHud);
                action(hud, "Edit HUD layout", "Move and resize individual HUD widgets", () -> { if (client != null) client.setScreen(new HudEditorScreen(config, save)); });
                module("fullbright", "Fullbright", "Brighten dark areas", () -> config.fullbright, () -> config.fullbright = !config.fullbright);
            }
            case 3 -> {
                Card sounds = module("toggleSounds", "Sounds", "Soft enable / disable feedback", () -> config.toggleSounds, () -> config.toggleSounds = !config.toggleSounds);
                range(sounds, "Volume", "Loudness of client feedback", 0, 1, .05, () -> config.toggleVolume, v -> config.toggleVolume = v);
                Card binds = panel("bindings", "Module binds", "Bind directly on any module");
                note(binds, "Middle-click title", "Then press a key or mouse button");
                note(binds, "Mouse wheel", "Scroll up / down while capturing");
                note(binds, "Delete / Esc", "Clear the bind / cancel capture");
                Card workspace = panel("workspace", "Workspace", "Window and interface");
                action(workspace, "Center window", "Restore the default menu position and size", () -> {
                    config.guiWidth = 540; config.guiHeight = 350;
                    config.guiX = (width - Math.min(540, width - 12)) / 2.0;
                    config.guiY = (height - Math.min(350, height - 12)) / 2.0;
                    constrain();
                });
                note(workspace, "Right Shift", "Open this menu; Esc saves and closes");
                note(workspace, "Right-click title", "Expand individual module settings");
            }
            default -> throw new IllegalStateException("Unknown category");
        }
    }

    private Card module(String id, String title, String description, BooleanSupplier enabled, Runnable toggle) {
        Card card = new Card(id, title, description, enabled, toggle);
        cards.add(card);
        return card;
    }
    private Card panel(String id, String title, String description) { return module(id, title, description, null, null); }
    private void toggle(Card card, String title, String description, BooleanSupplier state, Runnable action) {
        Control c = new Control(Kind.TOGGLE, title, description); c.state = state; c.action = action; card.controls.add(c);
    }
    private void range(Card card, String title, String description, double min, double max, double step, DoubleSupplier value, DoubleConsumer setter) {
        Control c = new Control(Kind.SLIDER, title, description); c.min = min; c.max = max; c.step = step; c.value = value; c.setter = setter; card.controls.add(c);
    }
    private void action(Card card, String title, String description, Runnable action) {
        Control c = new Control(Kind.ACTION, title, description); c.action = action; card.controls.add(c);
    }
    private void choice(Card card, String title, String description, Supplier<String> value, Runnable cycle) {
        Control c = new Control(Kind.CHOICE, title, description); c.choice = value; c.action = cycle; card.controls.add(c);
    }
    private void note(Card card, String title, String description) { card.controls.add(new Control(Kind.NOTE, title, description)); }

    private void layout() {
        boolean twoColumns = contentWidth >= 284;
        int columns = twoColumns ? 2 : 1, gap = 8;
        int columnWidth = (contentWidth - (columns - 1) * gap) / columns;
        int[] bottoms = new int[columns];
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int column = i % columns;
            card.x = contentX + column * (columnWidth + gap);
            card.y = contentY + bottoms[column] - (int) scroll;
            card.width = columnWidth;
            int cursor = 36;
            boolean open = card.enabled == null || expanded.contains(card.id);
            for (Control c : card.controls) {
                c.x = card.x + 9; c.y = card.y + cursor; c.width = columnWidth - 18;
                if (open) cursor += c.height();
            }
            card.height = open && !card.controls.isEmpty() ? cursor + 7 : 36;
            bottoms[column] += card.height + gap;
        }
        int total = 0;
        for (int bottom : bottoms) total = Math.max(total, bottom);
        maxScroll = Math.max(0, total - gap - contentHeight);
        double clamped = MathHelper.clamp(scroll, 0, maxScroll);
        if (clamped != scroll) { scroll = clamped; layout(); }
    }

    @Override public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        frameBlend = frameNanos == 0 ? 1 : (float) (1 - Math.exp(-Math.min(.1, (now - frameNanos) / 1e9) * 16));
        frameNanos = now;
        constrain(); layout();
        draw.fill(0, 0, width, height, 0x30080D09);
        rounded(draw, x - 4, y - 3, w + 8, h + 8, 0x30000000);
        rounded(draw, x, y, w, h, 0xD8151C15);
        draw.fill(x + 1, y + 1, x + sidebar, y + h - 1, 0x70313D27);
        draw.fill(x + sidebar, y + 10, x + sidebar + 1, y + h - 11, 0x283F6650);
        draw.fill(x + sidebar + 9, y + 26, x + w - 12, y + 27, 0x30496857);
        text(draw, "NEGRADLC", contentX, y + 12, WHITE);
        String version = "1.21.4";
        text(draw, version, x + w - textRenderer.getWidth(version) - 35, y + 12, MUTED);
        text(draw, "×", x + w - 22, y + 11, inside(mouseX, mouseY, x + w - 29, y + 5, 23, 22) ? WHITE : MUTED);
        crescent(draw, x + sidebar / 2, y + 34, 17, ACCENT);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int tabY = y + 69 + i * 29;
            boolean hover = inside(mouseX, mouseY, x + 5, tabY - 5, sidebar - 10, 25);
            if (hover || category == i) draw.fill(x + 5, tabY - 5, x + sidebar - 5, tabY + 20, category == i ? 0x203FD6B2 : 0x123FD6B2);
            if (category == i) draw.fill(x + 5, tabY - 1, x + 7, tabY + 15, ACCENT);
            text(draw, CATEGORIES[i], x + (sidebar - textRenderer.getWidth(CATEGORIES[i])) / 2, tabY + 3, category == i ? ACCENT : MUTED);
        }
        draw.enableScissor(contentX - 1, contentY, contentX + contentWidth + 1, contentY + contentHeight);
        String hoveredDescription = null;
        for (Card card : cards) {
            if (card.y + card.height < contentY || card.y > contentY + contentHeight) continue;
            boolean hover = inside(mouseX, mouseY, card.x, card.y, card.width, 33) && mouseY >= contentY && mouseY < contentY + contentHeight;
            rounded(draw, card.x, card.y, card.width, card.height, hover ? 0xD2151B16 : 0xB80B110D);
            boolean enabled = card.enabled != null && card.enabled.getAsBoolean();
            if (enabled) draw.fill(card.x, card.y + 6, card.x + 2, card.y + 25, ACCENT);
            String bind = card.enabled == null ? "" : card.id.equals(pendingBind) ? "..." : BindingCodes.label(config.binds.getOrDefault(card.id, -1));
            if ("NONE".equalsIgnoreCase(bind) || "UNBOUND".equalsIgnoreCase(bind)) bind = "—";
            int right = card.x + card.width - 9;
            String suffix = (card.controls.isEmpty() ? "" : (card.enabled == null || expanded.contains(card.id) ? "−" : "+"));
            if (!suffix.isEmpty()) { text(draw, suffix, right - 6, card.y + 9, MUTED); right -= 15; }
            if (!bind.isEmpty()) {
                String shownBind = textRenderer.trimToWidth(bind, Math.max(18, card.width / 3));
                text(draw, shownBind, right - textRenderer.getWidth(shownBind), card.y + 10, card.id.equals(pendingBind) ? ACCENT : MUTED);
                right -= textRenderer.getWidth(shownBind) + 8;
            }
            text(draw, textRenderer.trimToWidth(card.title, Math.max(18, right - card.x - 9)), card.x + 9, card.y + 9, enabled ? WHITE : MUTED);
            text(draw, textRenderer.trimToWidth(card.description, Math.max(10, card.width - 18)), card.x + 9, card.y + 23, 0xFF687E6E);
            if (hover) hoveredDescription = card.description + (card.enabled == null ? "" : " · LMB toggle / RMB settings / MMB bind");
            if (card.enabled == null || expanded.contains(card.id)) {
                for (Control control : card.controls) {
                    if (control.y + control.height() < contentY || control.y >= contentY + contentHeight) continue;
                    renderControl(draw, control, mouseX, mouseY);
                    if (inside(mouseX, mouseY, control.x, control.y, control.width, control.height()) && mouseY >= contentY && mouseY < contentY + contentHeight) hoveredDescription = control.description;
                }
            }
        }
        draw.disableScissor();
        if (maxScroll > 0) {
            int thumb = Math.max(15, (int) (contentHeight * contentHeight / (contentHeight + maxScroll)));
            int top = contentY + (int) ((contentHeight - thumb) * scroll / maxScroll);
            draw.fill(x + w - 7, contentY, x + w - 5, contentY + contentHeight, 0x30344B3A);
            draw.fill(x + w - 7, top, x + w - 5, top + thumb, 0xAA61D9C0);
        }
        String hint = notice != null && System.currentTimeMillis() < noticeUntil ? notice : pendingBind != null
                ? "Bind " + pendingBind + ": key / mouse / wheel · Esc cancel · Del clear"
                : hoveredDescription != null ? hoveredDescription : "RMB settings · MMB bind · Drag header / corner";
        text(draw, textRenderer.trimToWidth(hint, Math.max(20, w - sidebar - 27)), contentX, y + h - 15, pendingBind == null ? MUTED : ACCENT);
        for (int i = 0; i < 3; i++) draw.fill(x + w - 6 - i * 3, y + h - 7, x + w - 5 - i * 3, y + h - 6 - i * 3, MUTED);
    }

    private void renderControl(DrawContext draw, Control c, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, c.x, c.y, c.width, c.height());
        if (hover && c.kind != Kind.NOTE) rounded(draw, c.x - 3, c.y, c.width + 6, c.height() - 1, 0x173F785E);
        int reserved = switch (c.kind) {
            case TOGGLE -> 25;
            case CHOICE -> Math.min(c.width / 2, textRenderer.getWidth(c.choice.get()) + 5);
            case SLIDER -> textRenderer.getWidth(format(c)) + 7;
            case ACTION -> 12;
            case NOTE -> 0;
        };
        text(draw, textRenderer.trimToWidth(c.title, Math.max(10, c.width - reserved)), c.x, c.y + 5, c.kind == Kind.NOTE ? MUTED : WHITE);
        if (c.kind == Kind.TOGGLE) {
            String animationKey = c.x + ":" + c.title;
            float state = c.state.getAsBoolean() ? 1 : 0;
            float position = switchAnimation.getOrDefault(animationKey, state);
            position += (state - position) * frameBlend;
            switchAnimation.put(animationKey, position);
            int sx = c.x + c.width - 19;
            rounded(draw, sx, c.y + 6, 18, 8, c.state.getAsBoolean() ? 0xFF347662 : 0xFF34463A);
            rounded(draw, sx + 2 + Math.round(position * 10), c.y + 7, 5, 6, c.state.getAsBoolean() ? ACCENT : MUTED);
        } else if (c.kind == Kind.SLIDER) {
            String value = format(c);
            text(draw, value, c.x + c.width - textRenderer.getWidth(value), c.y + 5, ACCENT);
            int sy = c.y + 21;
            double fraction = MathHelper.clamp((c.value.getAsDouble() - c.min) / (c.max - c.min), 0, 1);
            draw.fill(c.x, sy, c.x + c.width, sy + 2, 0xFF35483B);
            draw.fill(c.x, sy, c.x + (int) (c.width * fraction), sy + 2, 0xFF48AA91);
            rounded(draw, c.x + (int) (c.width * fraction) - 2, sy - 2, 5, 6, WHITE);
        } else if (c.kind == Kind.CHOICE) {
            String value = textRenderer.trimToWidth(c.choice.get(), Math.max(18, c.width / 2));
            text(draw, value, c.x + c.width - textRenderer.getWidth(value), c.y + 5, ACCENT);
        } else if (c.kind == Kind.ACTION) text(draw, ">", c.x + c.width - 6, c.y + 5, ACCENT);
    }

    private static String format(Control c) { return String.format(Locale.ROOT, c.step >= 1 ? "%.0f" : c.step >= .1 ? "%.1f" : "%.2f", c.value.getAsDouble()); }
    private static String next(String current, String... values) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return values[(i + 1) % values.length];
        return values[0];
    }
    private static int nextColor(int current) {
        int[] colors = {0x57D9C0, 0x70B8FF, 0xC79BFF, 0xFF8B9B, 0xFFCF70, 0xFFFFFF};
        for (int i = 0; i < colors.length; i++) if (colors[i] == current) return colors[(i + 1) % colors.length];
        return colors[0];
    }
    private static String colorName(int color) {
        return switch (color) { case 0x57D9C0 -> "MINT"; case 0x70B8FF -> "SKY"; case 0xC79BFF -> "LILAC"; case 0xFF8B9B -> "ROSE"; case 0xFFCF70 -> "GOLD"; case 0xFFFFFF -> "WHITE"; default -> "CUSTOM"; };
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (pendingBind != null) { capture(BindingCodes.mouse(button)); return true; }
        constrain(); layout();
        if (button == 0 && inside(mx, my, x + w - 29, y + 5, 23, 22)) { close(); return true; }
        if (button == 0 && inside(mx, my, x + w - 15, y + h - 15, 15, 15)) { resizing = true; dragX = mx - w; dragY = my - h; return true; }
        if (button == 0 && inside(mx, my, x, y, w, 30)) { dragging = true; dragX = mx - x; dragY = my - y; return true; }
        for (int i = 0; i < CATEGORIES.length; i++) if (button == 0 && inside(mx, my, x + 5, y + 64 + i * 29, sidebar - 10, 25)) {
            category = i; scroll = 0; slider = null; buildCards(); click(); return true;
        }
        if (!inside(mx, my, contentX, contentY, contentWidth, contentHeight)) return inside(mx, my, x, y, w, h) || super.mouseClicked(mx, my, button);
        for (Card card : cards) {
            if (inside(mx, my, card.x, card.y, card.width, 36)) {
                if (button == 2 && card.enabled != null) { pendingBind = card.id; click(); return true; }
                if (button == 1 || button == 0 && card.enabled == null) {
                    if (!card.controls.isEmpty()) { if (!expanded.remove(card.id)) expanded.add(card.id); click(); }
                    return true;
                }
                if (button == 0 && card.toggle != null) {
                    boolean nextState = !card.enabled.getAsBoolean();
                    if ("toggleSounds".equals(card.id) && !nextState) SoundFeedback.playToggle(client, false, config);
                    card.toggle.run();
                    if (!"toggleSounds".equals(card.id) || nextState) SoundFeedback.playToggle(client, nextState, config);
                    save.run(); return true;
                }
            }
            if (card.enabled != null && !expanded.contains(card.id)) continue;
            for (Control c : card.controls) if (button == 0 && inside(mx, my, c.x, c.y, c.width, c.height())) {
                if (c.kind == Kind.SLIDER) { slider = c; changeSlider(mx); click(); return true; }
                if (c.action != null) { c.action.run(); click(); save.run(); return true; }
            }
        }
        return true;
    }

    private void capture(int code) {
        if (!BindingCodes.isValid(code)) return;
        if (code == GLFW.GLFW_KEY_RIGHT_SHIFT) { notify("Right Shift is reserved for the menu"); return; }
        BindingCodes.assign(config, pendingBind, code);
        notify(pendingBind + " bound to " + BindingCodes.label(code));
        pendingBind = null; click(); save.run();
    }
    private void notify(String message) { notice = message; noticeUntil = System.currentTimeMillis() + 3500; }
    private void click() { SoundFeedback.playClick(client, config); }
    private void changeSlider(double mx) {
        double fraction = MathHelper.clamp((mx - slider.x) / Math.max(1, slider.width), 0, 1);
        double value = slider.min + fraction * (slider.max - slider.min);
        slider.setter.accept(MathHelper.clamp(Math.round(value / slider.step) * slider.step, slider.min, slider.max));
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && dragging) { config.guiX = mx - dragX; config.guiY = my - dragY; constrain(); return true; }
        if (button == 0 && resizing) { config.guiWidth = mx - dragX; config.guiHeight = my - dragY; constrain(); return true; }
        if (button == 0 && slider != null) { changeSlider(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        boolean handled = dragging || resizing || slider != null;
        if (button == 0) { dragging = false; resizing = false; slider = null; if (handled) save.run(); }
        return handled || super.mouseReleased(mx, my, button);
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (pendingBind != null && vertical != 0) { capture(vertical > 0 ? BindingCodes.WHEEL_UP : BindingCodes.WHEEL_DOWN); return true; }
        if (inside(mx, my, x, y, w, h)) { scroll = MathHelper.clamp(scroll - vertical * 25, 0, maxScroll); return true; }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
    @Override public boolean keyPressed(int code, int scanCode, int modifiers) {
        if (pendingBind != null) {
            if (code == GLFW.GLFW_KEY_ESCAPE) { pendingBind = null; return true; }
            capture(code == GLFW.GLFW_KEY_DELETE || code == GLFW.GLFW_KEY_BACKSPACE ? BindingCodes.UNBOUND : code);
            return true;
        }
        return super.keyPressed(code, scanCode, modifiers);
    }
    @Override public void close() { save.run(); if (client != null) client.setScreen(null); }
    @Override public boolean shouldPause() { return false; }
    private void text(DrawContext draw, String text, int x, int y, int color) { draw.drawTextWithShadow(textRenderer, text, x, y, color); }
    private static boolean inside(double mx, double my, int x, int y, int width, int height) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    private static void rounded(DrawContext draw, int x, int y, int width, int height, int color) {
        draw.fill(x + 2, y, x + width - 2, y + height, color);
        draw.fill(x, y + 2, x + width, y + height - 2, color);
        draw.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }
    private static void crescent(DrawContext draw, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int extent = (int) Math.sqrt(radius * radius - dy * dy);
            for (int dx = -extent; dx <= extent; dx++) {
                // Subtract an offset circular cutout, preserving real scene transparency.
                if ((dx - 8) * (dx - 8) + (dy + 5) * (dy + 5) > (radius - 2) * (radius - 2)) draw.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
        }
    }
    private enum Kind { TOGGLE, SLIDER, ACTION, CHOICE, NOTE }
    private static final class Card {
        final String id, title, description;
        final BooleanSupplier enabled;
        final Runnable toggle;
        final List<Control> controls = new ArrayList<>();
        int x, y, width, height;
        Card(String id, String title, String description, BooleanSupplier enabled, Runnable toggle) { this.id = id; this.title = title; this.description = description; this.enabled = enabled; this.toggle = toggle; }
    }
    private static final class Control {
        final Kind kind;
        final String title, description;
        BooleanSupplier state;
        Runnable action;
        DoubleSupplier value;
        DoubleConsumer setter;
        Supplier<String> choice;
        double min, max, step;
        int x, y, width;
        Control(Kind kind, String title, String description) { this.kind = kind; this.title = title; this.description = description; }
        int height() { return kind == Kind.SLIDER ? 31 : 21; }
    }
}
