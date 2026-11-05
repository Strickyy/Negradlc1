package dev.negradlc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigStore {
    private static final Logger LOG = LoggerFactory.getLogger("Negradlc/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private boolean readFailed;

    public ConfigStore(Path file) { this.file = file; }

    public ClientConfig load() {
        if (!Files.exists(file)) return new ClientConfig();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ClientConfig config = GSON.fromJson(reader, ClientConfig.class);
            if (config == null) throw new IOException("Empty configuration");
            config.sanitize();
            return config;
        } catch (IOException | RuntimeException exception) {
            readFailed = true;
            LOG.warn("Cannot read {}; using defaults. Original file will be preserved.", file, exception);
            return new ClientConfig();
        }
    }

    public void save(ClientConfig config) {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            if (readFailed && Files.exists(file)) {
                // Keep the malformed user file; never silently overwrite it.
                Files.copy(file, file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis()));
                readFailed = false;
            }
            config.sanitize();
            Files.writeString(temp, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            LOG.error("Cannot save configuration to {}", file, exception);
        }
    }
}
