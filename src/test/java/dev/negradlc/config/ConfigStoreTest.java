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
}
