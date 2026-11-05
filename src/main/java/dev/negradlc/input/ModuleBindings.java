package dev.negradlc.input;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/** Edge detection prevents held keys from toggling every tick. Menus consume keys. */
public final class ModuleBindings {
    private final Set<Integer> held = new HashSet<>();

    public boolean tick(MinecraftClient client, ClientConfig config) {
        Set<Integer> down = new HashSet<>();
        boolean changed = false;
        boolean active = client.player != null && client.currentScreen == null && client.isWindowFocused();
        for (String module : ClientConfig.MODULES) {
            int key = config.binds.getOrDefault(module, -1);
            if (key <= GLFW.GLFW_KEY_UNKNOWN || key == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
            if (InputUtil.isKeyPressed(client.getWindow().getHandle(), key)) {
                down.add(key);
                if (active && !held.contains(key)) {
                    config.toggle(module);
                    changed = true;
                }
            }
        }
        held.clear();
        held.addAll(down);
        return changed;
    }
}
