package com.socratiemes.enkaltar.reward;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardTier {
    private final int tier;
    private final double chance;
    private final List<RewardEntry> rewards;

    public RewardTier(int tier, double chance, List<RewardEntry> rewards) {
        this.tier = tier;
        this.chance = Math.max(0.0, chance);
        this.rewards = rewards == null ? Collections.emptyList() : rewards;
    }

    public int getTier() {
        return tier;
    }

    public double getChance() {
        return chance;
    }

    public List<RewardEntry> getRewards() {
        return rewards;
    }

    public RewardEntry pickRandom() {
        if (rewards.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(rewards.size());
        return rewards.get(index);
    }
}
