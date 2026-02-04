package com.socratiemes.enkaltar.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.socratiemes.enkaltar.reward.CommandReward;
import com.socratiemes.enkaltar.reward.ItemReward;
import com.socratiemes.enkaltar.reward.RewardEntry;
import com.socratiemes.enkaltar.reward.RewardTier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class EnkAltarConfig {
    private static final double DEFAULT_TIER1 = 0.50;
    private static final double DEFAULT_TIER2 = 0.25;
    private static final double DEFAULT_TIER3 = 0.15;
    private static final double DEFAULT_TIER4 = 0.10;
    private static final double DEFAULT_TIER5 = 0.02;

    private final Path dataDirectory;
    private final HytaleLogger logger;
    private Properties properties;

    private String altarBlockId;
    private String altarHint;
    private String votingOilItemId;
    private String particleId;
    private boolean loggingEnabled;
    // Legacy NPC settings kept for compile compatibility (NPC feature is no longer used).
    private String npcModelAssetId;
    private String npcRoleName;
    private String npcInteractionHint;
    private String npcDisplayName;
    private List<RewardTier> tiers;
    private java.util.Map<Integer, Boolean> tierBroadcasts;

    public EnkAltarConfig(Path dataDirectory, HytaleLogger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.altarBlockId = "EnkAltar_Altar";
        this.altarHint = "Press F to throw Voting Oil on the altar";
        this.votingOilItemId = "EnkAltar_Voting_Oil";
        this.particleId = "Portal_Going_Through_Blue";
        this.loggingEnabled = true;
        this.npcModelAssetId = "Blocks/Decorative_Sets/Temple_Light/Brazier";
        this.npcRoleName = "Klops_Merchant";
        this.npcInteractionHint = "Press F to make an offer";
        this.npcDisplayName = "Enk Altar";
        this.tiers = List.of(
            new RewardTier(1, DEFAULT_TIER1, List.of()),
            new RewardTier(2, DEFAULT_TIER2, List.of()),
            new RewardTier(3, DEFAULT_TIER3, List.of()),
            new RewardTier(4, DEFAULT_TIER4, List.of()),
            new RewardTier(5, DEFAULT_TIER5, List.of())
        );
        this.tierBroadcasts = new java.util.HashMap<>();
    }

    public void reload() throws IOException {
        this.properties = PropertiesLoader.loadOrCreate(dataDirectory, "config.properties", logger);
        this.loggingEnabled = getBoolean("logging.enabled", true);
        this.altarBlockId = getString("altar.block.id", "EnkAltar_Altar");
        this.altarHint = getString("altar.hint", "Press F to throw Voting Oil on the altar");
        this.votingOilItemId = getString("altar.voting_oil.item", "EnkAltar_Voting_Oil");
        this.particleId = getString("altar.particle.id", "Portal_Going_Through_Blue");
        this.npcModelAssetId = getString("altar.npc.model.asset", "Blocks/Decorative_Sets/Temple_Light/Brazier");
        this.npcRoleName = getString("altar.npc.role", "Klops_Merchant");
        this.npcInteractionHint = getString("altar.npc.hint", "Press F to make an offer");
        this.npcDisplayName = getString("altar.npc.display_name", "Enk Altar");

        double tier1Chance = getDouble("tier1.chance", DEFAULT_TIER1);
        double tier2Chance = getDouble("tier2.chance", DEFAULT_TIER2);
        double tier3Chance = getDouble("tier3.chance", DEFAULT_TIER3);
        double tier4Chance = getDouble("tier4.chance", DEFAULT_TIER4);
        double tier5Chance = getDouble("tier5.chance", DEFAULT_TIER5);

        RewardTier tier1 = new RewardTier(1, tier1Chance, parseRewards("tier1"));
        RewardTier tier2 = new RewardTier(2, tier2Chance, parseRewards("tier2"));
        RewardTier tier3 = new RewardTier(3, tier3Chance, parseRewards("tier3"));
        RewardTier tier4 = new RewardTier(4, tier4Chance, parseRewards("tier4"));
        RewardTier tier5 = new RewardTier(5, tier5Chance, parseRewards("tier5"));

        this.tiers = List.of(tier1, tier2, tier3, tier4, tier5);
        this.tierBroadcasts = new java.util.HashMap<>();
        this.tierBroadcasts.put(1, getBoolean("tier1.broadcast", false));
        this.tierBroadcasts.put(2, getBoolean("tier2.broadcast", false));
        this.tierBroadcasts.put(3, getBoolean("tier3.broadcast", false));
        this.tierBroadcasts.put(4, getBoolean("tier4.broadcast", false));
        this.tierBroadcasts.put(5, getBoolean("tier5.broadcast", false));
    }

    public String getAltarBlockId() {
        return altarBlockId;
    }

    public String getAltarHint() {
        return altarHint;
    }

    public String getVotingOilItemId() {
        return votingOilItemId;
    }

    public String getParticleId() {
        return particleId;
    }

    public String getNpcModelAssetId() {
        return npcModelAssetId;
    }

    public String getNpcRoleName() {
        return npcRoleName;
    }

    public String getNpcInteractionHint() {
        return npcInteractionHint;
    }

    public String getNpcDisplayName() {
        return npcDisplayName;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public List<RewardTier> getTiers() {
        return tiers == null ? Collections.emptyList() : tiers;
    }

    public boolean isTierBroadcastEnabled(int tier) {
        if (tierBroadcasts == null) {
            return false;
        }
        return Boolean.TRUE.equals(tierBroadcasts.get(tier));
    }

    private String getString(String key, String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        return properties.getProperty(key, defaultValue).trim();
    }

    private double getDouble(String key, double defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            logWarning("Invalid %s value: %s", key, value);
            return defaultValue;
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim().toLowerCase();
        if ("true".equals(trimmed) || "false".equals(trimmed)) {
            return Boolean.parseBoolean(trimmed);
        }
        logWarning("Invalid %s value: %s", key, value);
        return defaultValue;
    }

    private List<RewardEntry> parseRewards(String tierKey) {
        List<RewardEntry> rewards = new ArrayList<>();

        String items = getString(tierKey + ".items", "");
        if (!items.isEmpty()) {
            String[] entries = items.split(",");
            for (String entry : entries) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split(":");
                if (parts.length != 2) {
                    logWarning("Invalid item entry in %s.items: %s", tierKey, trimmed);
                    continue;
                }
                String itemId = parts[0].trim();
                int amount;
                try {
                    amount = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ex) {
                    logWarning("Invalid item amount in %s.items: %s", tierKey, trimmed);
                    continue;
                }
                if (amount <= 0 || itemId.isEmpty()) {
                    continue;
                }
                rewards.add(new ItemReward(itemId, amount));
            }
        }

        String commands = getString(tierKey + ".commands", "");
        if (!commands.isEmpty()) {
            String[] entries = commands.split(";");
            for (String entry : entries) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                rewards.add(new CommandReward(trimmed));
            }
        }

        return rewards;
    }

    private void logWarning(String message, Object... args) {
        if (!loggingEnabled) {
            return;
        }
        String formatted;
        try {
            formatted = String.format(message, args);
        } catch (RuntimeException ex) {
            formatted = message;
        }
        logger.atWarning().log(formatted);
    }
}
