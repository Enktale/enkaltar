package com.socratiemes.enkaltar.reward;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.socratiemes.enkaltar.config.EnkAltarConfig;
import com.socratiemes.enkaltar.config.LangConfig;

public interface RewardEntry {
    boolean apply(Player player, EnkAltarConfig config, LangConfig lang);

    String describe();
}
