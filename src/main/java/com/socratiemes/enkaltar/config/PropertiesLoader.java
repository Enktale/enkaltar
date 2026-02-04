package com.socratiemes.enkaltar.config;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class PropertiesLoader {
    private PropertiesLoader() {
    }

    public static Properties loadOrCreate(Path dataDirectory, String fileName, HytaleLogger logger) throws IOException {
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve(fileName);
        if (Files.notExists(file)) {
            try (InputStream stream = PropertiesLoader.class.getResourceAsStream("/" + fileName)) {
                if (stream == null) {
                    throw new IOException("Missing default resource: " + fileName);
                }
                Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
                logger.atInfo().log("Created default %s at %s", fileName, file.toAbsolutePath());
            }
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }
        return props;
    }
}
