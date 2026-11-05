package dev.negradlc;

import dev.negradlc.combat.CombatController;
import dev.negradlc.config.ClientConfig;
import dev.negradlc.config.ConfigStore;
import dev.negradlc.input.ModuleBindings;
import dev.negradlc.movement.MovementController;
import dev.negradlc.ui.HudRenderer;
import dev.negradlc.ui.NegradlcScreen;
import dev.negradlc.visuals.EspRenderer;
import dev.negradlc.sound.SoundFeedback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class NegradlcClient implements ClientModInitializer {
    private static ClientConfig config = new ClientConfig();
    private final CombatController combat = new CombatController();
    private final MovementController movement = new MovementController();
    private static final ModuleBindings bindings = new ModuleBindings();
    private static ConfigStore store;

    public static ClientConfig config() { return config; }
    public static boolean mouseButton(int button, int action) {
        return store != null && bindings.mouseButton(net.minecraft.client.MinecraftClient.getInstance(), config, button, action, () -> store.save(config));
    }
    public static boolean scroll(double amount) {
        return store != null && bindings.scroll(net.minecraft.client.MinecraftClient.getInstance(), config, amount, () -> store.save(config));
    }

    @Override
    public void onInitializeClient() {
        store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("negradlc.json"));
        config = store.load();
        SoundFeedback.initialize();
        KeyBinding menu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.negradlc.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.negradlc"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (bindings.tick(client, config)) store.save(config);
            while (menu.wasPressed()) {
                if (client.currentScreen == null) {
                    SoundFeedback.playClick(client, config);
                    client.setScreen(new NegradlcScreen(config, () -> store.save(config)));
                }
                else if (client.currentScreen instanceof NegradlcScreen screen) screen.close();
            }
            movement.tick(client, config);
            combat.tick(client, config);
        });
        HudRenderCallback.EVENT.register((context, tickCounter) ->
            HudRenderer.render(context, net.minecraft.client.MinecraftClient.getInstance(), config, combat.getTarget(), combat.getStatus()));
        WorldRenderEvents.LAST.register(context -> EspRenderer.render(context, config));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            movement.reset();
            combat.reset();
            store.save(config);
        });
    }
}
