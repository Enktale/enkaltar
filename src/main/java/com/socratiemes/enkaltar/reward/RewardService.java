package com.socratiemes.enkaltar.reward;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.socratiemes.enkaltar.config.EnkAltarConfig;
import com.socratiemes.enkaltar.config.LangConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class RewardService {
    private final Supplier<EnkAltarConfig> configSupplier;
    private final Supplier<LangConfig> langSupplier;

    public RewardService(Supplier<EnkAltarConfig> configSupplier, Supplier<LangConfig> langSupplier) {
        this.configSupplier = configSupplier;
        this.langSupplier = langSupplier;
    }

    public void grantReward(Player player) {
        EnkAltarConfig config = configSupplier.get();
        LangConfig lang = langSupplier.get();
        if (config == null || lang == null) {
            return;
        }

        RewardTier tier = rollTier(config.getTiers());
        if (tier == null || tier.getRewards().isEmpty()) {
            lang.send(player, "enkaltar.reward.none", Map.of("tier", tier == null ? "?" : Integer.toString(tier.getTier())));
            return;
        }

        RewardEntry reward = tier.pickRandom();
        if (reward == null) {
            lang.send(player, "enkaltar.reward.none", Map.of("tier", Integer.toString(tier.getTier())));
            return;
        }

        Map<String, String> params = Map.of("tier", Integer.toString(tier.getTier()));
        lang.send(player, "enkaltar.reward.tier.personal", params);

        boolean applied = reward.apply(player, config, lang);
        if (!applied) {
            return;
        }

        if (config.isTierBroadcastEnabled(tier.getTier())) {
            Message broadcast = lang.message("enkaltar.reward.tier.broadcast", Map.of(
                    "tier", Integer.toString(tier.getTier()),
                    "player", player.getDisplayName()
            ));
            if (player.getWorld() != null) {
                for (Player target : player.getWorld().getPlayers()) {
                    target.sendMessage(broadcast);
                }
            } else {
                player.sendMessage(broadcast);
            }
        }
    }

    private RewardTier rollTier(List<RewardTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return null;
        }

        double total = 0.0;
        for (RewardTier tier : tiers) {
            total += tier.getChance();
        }

        if (total <= 0.0) {
            return tiers.get(0);
        }

        double roll = ThreadLocalRandom.current().nextDouble() * total;
        double running = 0.0;
        for (RewardTier tier : tiers) {
            running += tier.getChance();
            if (roll <= running) {
                return tier;
            }
        }

        return tiers.get(tiers.size() - 1);
    }
}
