package com.socratiemes.enkaltar.reward;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.socratiemes.enkaltar.config.EnkAltarConfig;
import com.socratiemes.enkaltar.config.LangConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandReward implements RewardEntry {
    private final String command;
    private static final Pattern BALANCE_ADD = Pattern.compile("^/?balance\\s+add\\s+\\S+\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCP_ADD_CHUNK = Pattern.compile("^/?scp\\s+add-chunk-amount\\s+\\S+\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);

    public CommandReward(String command) {
        this.command = command;
    }

    @Override
    public boolean apply(Player player, EnkAltarConfig config, LangConfig lang) {
        if (player == null) {
            return false;
        }

        String resolved = resolve(player);
        CommandManager.get().handleCommand(ConsoleSender.INSTANCE, resolved);
        sendCommandMessage(player, resolved);
        return true;
    }

    private String resolve(Player player) {
        PlayerRef ref = player.getPlayerRef();
        String name = ref != null ? ref.getUsername() : player.getDisplayName();
        String uuid = ref != null ? ref.getUuid().toString() : player.getUuid().toString();
        return command
            .replace("{player}", name)
            .replace("{uuid}", uuid);
    }

    @Override
    public String describe() {
        return command;
    }

    private void sendCommandMessage(Player player, String resolved) {
        String trimmed = resolved == null ? "" : resolved.trim();
        Matcher balanceMatch = BALANCE_ADD.matcher(trimmed);
        if (balanceMatch.matches()) {
            String amount = balanceMatch.group(1);
            player.sendMessage(Message.raw("The vote altar gave you $" + amount + "!"));
            return;
        }

        Matcher scpMatch = SCP_ADD_CHUNK.matcher(trimmed);
        if (scpMatch.matches()) {
            String amount = scpMatch.group(1);
            player.sendMessage(Message.raw("You have been given " + amount + " extra claims!"));
            return;
        }

        player.sendMessage(Message.raw("You have received something else than an item from the altar."));
    }
}
