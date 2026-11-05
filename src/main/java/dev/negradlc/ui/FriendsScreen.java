package dev.negradlc.ui;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Friend names are matched without regard to case by combat target selection. */
public final class FriendsScreen extends Screen {
    private final ClientConfig config;
    private final Runnable save;
    private TextFieldWidget input;
    private int x, y, panelWidth, panelHeight;
    private int scroll;
    private String message = "Player names are matched without regard to case.";

    public FriendsScreen(ClientConfig config, Runnable save) {
        super(Text.literal("Negradlc Friends"));
        this.config = config;
        this.save = save;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(360, width - 20);
        panelHeight = Math.min(300, height - 20);
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        input = addDrawableChild(new TextFieldWidget(textRenderer, x + 14, y + 45, panelWidth - 87, 20, Text.literal("Player name")));
        input.setMaxLength(16);
        input.setPlaceholder(Text.literal("Player name"));
        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), button -> addFriend()).dimensions(x + panelWidth - 65, y + 45, 51, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(x + panelWidth - 73, y + panelHeight - 32, 59, 20).build());
        setInitialFocus(input);
    }

    private void addFriend() {
        String name = input.getText().trim();
        if (!name.matches("[A-Za-z0-9_]{1,16}")) {
            message = "Use 1–16 letters, numbers or underscores.";
            return;
        }
        if (config.friends.stream().anyMatch(existing -> existing.equalsIgnoreCase(name))) {
            message = name + " is already a friend.";
            return;
        }
        config.friends.add(name);
        input.setText("");
        message = "Added " + name + ".";
        save.run();
    }

    private List<String> names() { return new ArrayList<>(config.friends); }
    private int visibleRows() { return Math.max(1, (panelHeight - 127) / 24); }

    @Override
    public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
        draw.fill(0, 0, width, height, 0xB2090B14);
        draw.fill(x, y, x + panelWidth, y + panelHeight, 0xFA161925);
        draw.fill(x, y, x + panelWidth, y + 2, 0xFFAA8AFF);
        draw.drawTextWithShadow(textRenderer, "NEGRADLC  /  FRIENDS", x + 14, y + 15, 0xFFF0EDF8);
        List<String> names = names();
        scroll = MathHelper.clamp(scroll, 0, Math.max(0, names.size() - visibleRows()));
        int top = y + 77;
        for (int i = 0; i < visibleRows() && scroll + i < names.size(); i++) {
            int rowY = top + i * 24;
            draw.fill(x + 14, rowY, x + panelWidth - 14, rowY + 21, 0xFF242A3A);
            draw.drawTextWithShadow(textRenderer, names.get(scroll + i), x + 23, rowY + 6, 0xFFF0EDF8);
            draw.drawTextWithShadow(textRenderer, "\u00d7", x + panelWidth - 31, rowY + 6, 0xFFAA8AFF);
        }
        if (names.isEmpty()) draw.drawTextWithShadow(textRenderer, "No friends added yet", x + 22, top + 10, 0xFF8F98AD);
        draw.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(message, panelWidth - 100), x + 14, y + panelHeight - 26, 0xFF8F98AD);
        super.render(draw, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= x + panelWidth - 42 && mouseX < x + panelWidth - 14) {
            int relativeY = (int) mouseY - y - 77;
            if (relativeY >= 0 && relativeY < visibleRows() * 24 && relativeY % 24 < 21) {
                int index = scroll + relativeY / 24;
                List<String> names = names();
                if (index < names.size()) {
                    String removed = names.get(index);
                    config.friends.remove(removed);
                    message = "Removed " + removed + ".";
                    save.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - (int) Math.signum(verticalAmount), 0, Math.max(0, config.friends.size() - visibleRows()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { addFriend(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { save.run(); if (client != null) client.setScreen(new NegradlcScreen(config, save)); }

    @Override
    public boolean shouldPause() { return false; }
}
