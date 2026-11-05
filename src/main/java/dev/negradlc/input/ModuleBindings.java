package dev.negradlc.input;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import dev.negradlc.sound.SoundFeedback;

import java.util.HashSet;
import java.util.Set;

/** Edge detection prevents held keys from toggling every tick. Menus consume keys. */
public final class ModuleBindings {
    private final Set<Integer> held = new HashSet<>();
    private final Set<Integer> consumedMouse = new HashSet<>();
    private final WheelAccumulator wheel = new WheelAccumulator();

    public boolean tick(MinecraftClient client, ClientConfig config) {
        Set<Integer> down = new HashSet<>();
        boolean changed = false;
        boolean active = client.player != null && client.currentScreen == null && client.isWindowFocused();
        for (String module : ClientConfig.MODULES) {
            int key = config.binds.getOrDefault(module, -1);
            if (key < 32 || key > 348 || key == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
            if (InputUtil.isKeyPressed(client.getWindow().getHandle(), key)) {
                down.add(key);
                if (active && !held.contains(key)) {
                    config.toggle(module);
                    SoundFeedback.playToggle(client, config.enabled(module), config);
                    changed = true;
                }
            }
        }
        held.clear();
        held.addAll(down);
        return changed;
    }

    private boolean active(MinecraftClient client) {
        return client.player != null && client.currentScreen == null && client.isWindowFocused();
    }

    private boolean trigger(MinecraftClient client, ClientConfig config, int code, Runnable save) {
        boolean changed = false;
        for (String module : ClientConfig.MODULES) if (config.binds.getOrDefault(module, -1) == code) {
            config.toggle(module);
            SoundFeedback.playToggle(client, config.enabled(module), config);
            changed = true;
        }
        if (changed) save.run();
        return changed;
    }

    public boolean mouseButton(MinecraftClient client, ClientConfig config, int button, int action, Runnable save) {
        if (action == GLFW.GLFW_RELEASE) return consumedMouse.remove(button);
        if (action != GLFW.GLFW_PRESS || !active(client)) return false;
        if (trigger(client, config, BindingCodes.mouse(button), save)) {
            consumedMouse.add(button);
            return true;
        }
        return false;
    }

    public boolean scroll(MinecraftClient client, ClientConfig config, double amount, Runnable save) {
        if (!active(client) || amount == 0) { wheel.reset(); return false; }
        int code = amount > 0 ? BindingCodes.WHEEL_UP : BindingCodes.WHEEL_DOWN;
        boolean bound = java.util.Arrays.stream(ClientConfig.MODULES)
                .anyMatch(module -> config.binds.getOrDefault(module, -1) == code);
        if (!bound) { wheel.reset(); return false; }
        int direction = wheel.accept(amount);
        if (direction != 0) trigger(client, config, code, save);
        return true; // A bound wheel gesture must not also change the hotbar slot.
    }
}
