package com.socratiemes.enkaltar.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.socratiemes.enkaltar.config.EnkAltarConfig;

import java.util.function.Supplier;

public final class EnkAltarGiveCommand extends CommandBase {
    private final Supplier<EnkAltarConfig> configSupplier;
    private final RequiredArg<PlayerRef> playerArg;
    private final RequiredArg<Integer> amountArg;

    public EnkAltarGiveCommand(Supplier<EnkAltarConfig> configSupplier) {
        super("give", "Give voting oil to a player");
        this.configSupplier = configSupplier;
        this.playerArg = withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF);
        this.amountArg = withRequiredArg("amount", "Amount of voting oil", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(CommandContext context) {
        PlayerRef target = context.get(playerArg);
        Integer amount = context.get(amountArg);
        if (target == null || amount == null || amount <= 0) {
            context.sendMessage(Message.raw("Usage: /enkaltar give <player> <amount>"));
            return;
        }

        Ref<EntityStore> ref = target.getReference();
        if (ref == null || !ref.isValid()) {
            context.sendMessage(Message.raw("Target player not found."));
            return;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            context.sendMessage(Message.raw("Target player not found."));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Target player not found."));
            return;
        }

        EnkAltarConfig config = configSupplier.get();
        if (config == null || config.getVotingOilItemId() == null || config.getVotingOilItemId().isBlank()) {
            context.sendMessage(Message.raw("Voting oil item id is not configured."));
            return;
        }

        ItemStack stack = new ItemStack(config.getVotingOilItemId(), amount);
        ItemContainer container = player.getInventory().getCombinedBackpackStorageHotbar();
        if (container == null || !container.canAddItemStack(stack)) {
            context.sendMessage(Message.raw("Player inventory is full."));
            return;
        }

        container.addItemStack(stack);
        context.sendMessage(Message.raw("Gave " + amount + " voting oil to " + player.getDisplayName() + "."));
    }
}
