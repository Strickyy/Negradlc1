package dev.negradlc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @TempDir Path directory;

    @Test void freshConfigIsOptInAndDoesNotTargetMobs() {
        ClientConfig config = new ConfigStore(directory.resolve("client.json")).load();
        assertFalse(config.killAura);
        assertFalse(config.triggerBot);
        assertFalse(config.aimAssist);
        assertFalse(config.airStag);
        assertFalse(config.critOnly);
        assertTrue(config.autoCritJump);
        assertTrue(config.targetPlayers);
        assertFalse(config.targetHostile);
        assertFalse(config.targetPassive);
        assertEquals(-1, config.binds.get("killAura"));
    }

    @Test void savesAndRestoresBindingsLayoutAndFriends() {
        var store = new ConfigStore(directory.resolve("client.json"));
        var config = store.load();
        config.killAura = true;
        config.binds.put("killAura", 82);
        config.targetLayout.x = 167;
        config.targetLayout.scale = 1.3;
        config.friends.add("Steve");
        store.save(config);
        var restored = store.load();
        assertTrue(restored.killAura);
        assertEquals(82, restored.binds.get("killAura"));
        assertEquals(167, restored.targetLayout.x);
        assertEquals(1.3, restored.targetLayout.scale);
        assertTrue(restored.friends.contains("Steve"));
        assertFalse(Files.exists(directory.resolve("client.json.tmp")));
    }

    @Test void malformedFileIsPreservedBeforeReplacingIt() throws Exception {
        Path file = directory.resolve("client.json");
        Files.writeString(file, "{invalid json");
        var store = new ConfigStore(file);
        var config = store.load();
        store.save(config);
        try (var files = Files.list(directory)) {
            Path backup = files.filter(path -> path.getFileName().toString().contains(".invalid-")).findFirst().orElseThrow();
            assertEquals("{invalid json", Files.readString(backup));
        }
        assertDoesNotThrow(() -> new ConfigStore(file).load());
    }

    @Test void repairsMissingNestedFieldsAndInvalidNumbers() throws Exception {
        Path file = directory.resolve("client.json");
        Files.writeString(file, """
            {"attackRange":50,"guiWidth":1,"watermarkLayout":null,
            "binds":{"killAura":999},"friends":null,"aimSpeed":-5}
            """);
        var config = new ConfigStore(file).load();
        assertEquals(3, config.attackRange);
        assertEquals(220, config.guiWidth);
        assertEquals(0.1, config.aimSpeed);
        assertNotNull(config.watermarkLayout);
        assertNotNull(config.friends);
        assertEquals(-1, config.binds.get("killAura"));
        assertEquals(-1, config.binds.get("airStag"));
    }

    @Test void versionTwoSettingsKeepCritPolicyAndTriggerBindWhenNewModesAreAdded() throws Exception {
        Path file = directory.resolve("client.json");
        Files.writeString(file, """
            {"critOnly":true,"autoCritJump":false,"triggerBot":true,
             "attackCooldown":0.97,"binds":{"triggerBot":290},"aimPoint":"HEAD"}
            """);
        var store = new ConfigStore(file);
        var config = store.load();
        assertTrue(config.critOnly);
        assertFalse(config.autoCritJump);
        assertTrue(config.triggerBot);
        assertEquals(0.97, config.attackCooldown);
        assertEquals(290, config.binds.get("triggerBot"));
        assertFalse(config.smoothTriggerAim);
        assertEquals("SNAP_LEGIT", config.auraRotation);
        config.auraRotation = "SMOOTH";
        config.auraYawSpeed = 12;
        store.save(config);
        var loaded = store.load();
        assertEquals("SMOOTH", loaded.auraRotation);
        assertEquals(12, loaded.auraYawSpeed);
        assertTrue(loaded.critOnly);
        assertFalse(loaded.autoCritJump);
        assertEquals(290, loaded.binds.get("triggerBot"));
        assertEquals("HEAD", loaded.aimPoint);
    }

    @Test void invalidNewModesFallBackWithoutChangingExistingCombatSettings() throws Exception {
        Path file = directory.resolve("client.json");
        Files.writeString(file, """
            {"auraRotation":"bad","auraAimPoint":null,"auraPriority":"bad",
             "auraYawSpeed":-1,"auraSmoothing":5,"aimAcceleration":-2,
             "critOnly":true,"autoCritJump":true}
            """);
        var config = new ConfigStore(file).load();
        assertEquals("SNAP_LEGIT", config.auraRotation);
        assertEquals("CHEST", config.auraAimPoint);
        assertEquals("DISTANCE", config.auraPriority);
        assertEquals(1, config.auraYawSpeed);
        assertEquals(1, config.auraSmoothing);
        assertEquals(0.1, config.aimAcceleration);
        assertTrue(config.critOnly);
        assertTrue(config.autoCritJump);
    }

    @Test void visualCustomizationRoundTripsWithoutChangingCritsOrBindings() {
        var store = new ConfigStore(directory.resolve("visuals.json"));
        var config = new ClientConfig();
        config.critOnly = true;
        config.autoCritJump = false;
        config.binds.put("triggerBot", 290);
        config.jumpCircle = true;
        config.targetEffects = true;
        config.targetEffectStyle = "POINTS";
        config.targetEffectColor = 0xAA88CC;
        config.worldParticles = true;
        config.worldParticleStyle = "CHERRY";
        config.worldCustomizer = true;
        config.worldWeather = "THUNDER";
        config.worldTime = 18000;
        config.skyTintEnabled = true;
        config.skyTintStrength = 0.4;
        store.save(config);
        var loaded = store.load();
        assertTrue(loaded.jumpCircle);
        assertTrue(loaded.targetEffects);
        assertEquals("POINTS", loaded.targetEffectStyle);
        assertEquals(0xAA88CC, loaded.targetEffectColor);
        assertEquals("CHERRY", loaded.worldParticleStyle);
        assertEquals("THUNDER", loaded.worldWeather);
        assertEquals(18000, loaded.worldTime);
        assertEquals(0.4, loaded.skyTintStrength);
        assertTrue(loaded.critOnly);
        assertFalse(loaded.autoCritJump);
        assertEquals(290, loaded.binds.get("triggerBot"));
    }

    @Test void malformedVisualOptionsAreRepairedAndNewModulesStayOptIn() throws Exception {
        Path file = directory.resolve("visuals.json");
        Files.writeString(file, """
            {"targetEffectStyle":"bad","jumpCircleDuration":-10,"worldParticleDensity":999,
             "worldParticleStyle":null,"worldWeather":"bad","worldTime":90000,
             "skyTintStrength":2,"triggerDwellTicks":-5,"critOnly":true}
            """);
        var config = new ConfigStore(file).load();
        assertEquals("CIRCLE", config.targetEffectStyle);
        assertEquals(0.4, config.jumpCircleDuration);
        assertEquals(40, config.worldParticleDensity);
        assertEquals("END_ROD", config.worldParticleStyle);
        assertEquals("CLEAR", config.worldWeather);
        assertEquals(23999, config.worldTime);
        assertEquals(1, config.skyTintStrength);
        assertEquals(0, config.triggerDwellTicks);
        assertFalse(config.jumpCircle);
        assertFalse(config.worldCustomizer);
        assertTrue(config.critOnly);
        assertEquals(-1, config.binds.get("worldCustomizer"));
    }
}
