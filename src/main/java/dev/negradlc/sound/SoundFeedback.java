package dev.negradlc.sound;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Short original, gently faded tones. Does not change master/game volume settings. */
public final class SoundFeedback {
    private static final SoundEvent ON = register("toggle_on");
    private static final SoundEvent OFF = register("toggle_off");
    private static final SoundEvent CLICK = register("click");
    private SoundFeedback() {}
    private static SoundEvent register(String path) {
        Identifier id = Identifier.of("negradlc", path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    public static void initialize() { /* class initialization registers the events */ }
    public static void playToggle(MinecraftClient client, boolean enabled, ClientConfig config) {
        play(client, enabled ? ON : OFF, config, 1);
    }
    public static void playClick(MinecraftClient client, ClientConfig config) { play(client, CLICK, config, 0.55f); }
    private static void play(MinecraftClient client, SoundEvent event, ClientConfig config, float scale) {
        if (client == null || !config.toggleSounds || config.toggleVolume <= 0) return;
        client.getSoundManager().play(PositionedSoundInstance.master(event, 1f, (float) config.toggleVolume * scale));
    }
}
