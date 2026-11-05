package dev.negradlc.input;

import dev.negradlc.config.ClientConfig;
import dev.negradlc.config.ConfigStore;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BindingCodesTest {
    @TempDir Path directory;
    @Test void wheelAndMouseBindingsSurviveConfigRoundTrip() {
        var cfg = new ClientConfig();
        BindingCodes.assign(cfg, "killAura", BindingCodes.mouse(2));
        BindingCodes.assign(cfg, "aimAssist", BindingCodes.WHEEL_UP);
        BindingCodes.assign(cfg, "esp", BindingCodes.WHEEL_DOWN);
        var store = new ConfigStore(directory.resolve("binds.json"));
        store.save(cfg);
        var loaded = store.load();
        assertEquals(1002, loaded.binds.get("killAura"));
        assertEquals(2000, loaded.binds.get("aimAssist"));
        assertEquals(2001, loaded.binds.get("esp"));
    }
    @Test void assigningAnOccupiedKeyMovesItAndUnbindDoesNotClearOthers() {
        var cfg = new ClientConfig();
        BindingCodes.assign(cfg, "killAura", BindingCodes.WHEEL_UP);
        BindingCodes.assign(cfg, "esp", BindingCodes.WHEEL_UP);
        assertEquals(-1, cfg.binds.get("killAura"));
        assertEquals(2000, cfg.binds.get("esp"));
        BindingCodes.assign(cfg, "aimAssist", -1);
        assertEquals(2000, cfg.binds.get("esp"));
    }
    @Test void reservedAndInvalidCodesAreRejected() {
        var cfg = new ClientConfig();
        assertThrows(IllegalArgumentException.class, () -> BindingCodes.assign(cfg, "killAura", 344));
        assertThrows(IllegalArgumentException.class, () -> BindingCodes.assign(cfg, "killAura", 999));
        assertThrows(IllegalArgumentException.class, () -> BindingCodes.assign(cfg, "missing", 65));
        assertEquals("MOUSE MIDDLE", BindingCodes.label(1002));
    }
    @Test void highResolutionWheelWaitsForNotchAndResetsDirection() {
        var wheel = new WheelAccumulator();
        assertEquals(0, wheel.accept(.25));
        assertEquals(0, wheel.accept(.25));
        assertEquals(1, wheel.accept(.5));
        assertEquals(0, wheel.accept(.75));
        assertEquals(0, wheel.accept(-.5));
        assertEquals(-1, wheel.accept(-.5));
        assertEquals(0, wheel.accept(Double.NaN));
        wheel.reset();
        assertEquals(1, wheel.accept(1));
    }
}
