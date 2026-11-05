# Validation: 0.3.0

- Minecraft Java 1.21.4, Fabric Loader 0.16.10, Fabric API 0.119.2, Java 21.
- Full Gradle build, remapped JAR and 34 tests passed; zero failures/skips.
- Tests cover bounded accelerated rotations, yaw wrapping, deadzone, convergence, abrupt reversal, configuration migration, existing TriggerBot binds and Crit only values, plus the previous combat/movement/projection tests.
- The critical-hit decision block in CombatController is identical to 0.2.0. FallTracker and the existing critical rules were not edited.
- Actual client launched in a local test world. Snap and Smooth reduced a stationary cow's 100 HP to 92.5 and 95.5 respectively with Crit only enabled. Snap Legit reduced the same target from 100 to 97 HP. These are local-world checks, not claims about a remote server.
- Menu rotation choices, optional TriggerBot Smooth aim, Ctrl+F search, module filtering and configuration persistence were exercised in-game. Screenshot: menu-0.3.0.png.
- Test audio used OpenAL's null output. Remote server/anti-cheat behavior was not tested.
