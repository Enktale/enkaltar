package com.socratiemes.enkaltar.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.socratiemes.enkaltar.util.ColorUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class LangConfig {
    private final Path dataDirectory;
    private final HytaleLogger logger;
    private Properties properties;

    public LangConfig(Path dataDirectory, HytaleLogger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void reload() throws IOException {
        this.properties = PropertiesLoader.loadOrCreate(dataDirectory, "lang.config", logger);
    }

    public String get(String key) {
        if (properties == null) {
            return key;
        }
        return properties.getProperty(key, key);
    }

    public String format(String key, Map<String, String> params) {
        String raw = get(key);
        if (params == null || params.isEmpty()) {
            return raw;
        }
        String result = raw;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public Message message(String key, Map<String, String> params) {
        String text = format(key, params);
        return ColorUtil.colorize(text);
    }

    public void send(Player player, String key, Map<String, String> params) {
        if (player == null) {
            return;
        }
        player.sendMessage(message(key, params));
    }
}
