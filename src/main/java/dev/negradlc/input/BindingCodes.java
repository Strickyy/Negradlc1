package dev.negradlc.input;

import dev.negradlc.config.ClientConfig;
import java.util.Locale;

/** Persisted codes: GLFW keys, 1000+GLFW mouse button, 2000/2001 wheel. No game dependencies. */
public final class BindingCodes {
    public static final int UNBOUND = -1, WHEEL_UP = 2000, WHEEL_DOWN = 2001;
    private BindingCodes() {}
    public static int mouse(int button) { return 1000 + button; }
    public static boolean isMouse(int code) { return code >= 1000 && code <= 1007; }
    public static boolean isValid(int code) {
        return code == UNBOUND || code >= 32 && code <= 348 || isMouse(code) || code == WHEEL_UP || code == WHEEL_DOWN;
    }
    public static void assign(ClientConfig config, String module, int code) {
        if (!isValid(code) || code == 344) throw new IllegalArgumentException("Invalid or reserved binding");
        if (!java.util.Arrays.asList(ClientConfig.MODULES).contains(module)) throw new IllegalArgumentException("Unknown module");
        if (code != UNBOUND) config.binds.replaceAll((id, old) -> id.equals(module) || old == null || old != code ? old : UNBOUND);
        config.binds.put(module, code);
    }
    public static String label(int code) {
        if (code == UNBOUND) return "NONE";
        if (code == WHEEL_UP) return "WHEEL UP";
        if (code == WHEEL_DOWN) return "WHEEL DOWN";
        if (isMouse(code)) return switch (code - 1000) {
            case 0 -> "MOUSE LEFT"; case 1 -> "MOUSE RIGHT"; case 2 -> "MOUSE MIDDLE";
            default -> "MOUSE " + (code - 999);
        };
        if (code >= 65 && code <= 90 || code >= 48 && code <= 57) return Character.toString((char) code).toUpperCase(Locale.ROOT);
        if (code >= 290 && code <= 314) return "F" + (code - 289);
        return switch (code) {
            case 32 -> "SPACE"; case 258 -> "TAB"; case 257 -> "ENTER";
            case 340 -> "L SHIFT"; case 344 -> "R SHIFT";
            case 341 -> "L CTRL"; case 345 -> "R CTRL";
            case 342 -> "L ALT"; case 346 -> "R ALT";
            default -> "KEY " + code;
        };
    }
}
