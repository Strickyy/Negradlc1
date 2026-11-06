# Validation: 0.4.0

- Minecraft Java 1.21.4, Fabric Loader 0.16.10, Fabric API 0.119.2, Java 21.
- Full Gradle build and 51 tests pass, with no failures/skips. Tests cover combat camera sessions, stable crosshair dwell, rotation limits, configuration migration, geometry, jump detection, ambience, and earlier combat/movement/projection rules.
- The critical-hit decision block is byte-for-byte identical to 0.3.0. FallTracker and existing key/mouse bindings were not changed.
- The actual Minecraft client loaded all three WorldCustomizer mixins and entered a local test world.
- With KillAura and AimAssist enabled together, a cow 4.1 blocks away remained outside melee range but was aimed at successfully; HUD showed Aiming and the server observed rotation[-13.928057,9.6659155].
- Cube, Square, Circle and Points target effects were selected and rendered in-game; JumpCircle appeared on a real rising jump. WorldParticles rendered around the player.
- With server time 18013, visual time 6000 showed daylight. Disabling WorldCustomizer restored real rainy nighttime. Server time/weather fields are not overwritten by the module.
- Existing CritOnly combat attacks were exercised during the initial local-world smoke test. Remote server/anti-cheat behavior was not tested.
- Audio used OpenAL null output. The cloud graphical test uses a virtual display/software OpenGL; its FPS is not a desktop performance benchmark.
