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
}
