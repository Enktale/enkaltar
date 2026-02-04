package com.socratiemes.enkaltar.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.socratiemes.enkaltar.config.EnkAltarConfig;
import com.socratiemes.enkaltar.npc.AltarNpcRuntimeService;
import com.socratiemes.enkaltar.npc.AltarNpcStore;

import java.util.function.Supplier;

public final class EnkAltarNpcCommand extends CommandBase {
    public EnkAltarNpcCommand(
            Supplier<AltarNpcStore> npcStore,
            Supplier<AltarNpcRuntimeService> npcRuntimeService,
            Supplier<EnkAltarConfig> config
    ) {
        super("npc", "EnkAltar NPC commands");
        addSubCommand(new EnkAltarNpcSpawnCommand(npcStore, npcRuntimeService, config));
        addSubCommand(new EnkAltarNpcRemoveCommand(npcStore));
        addSubCommand(new EnkAltarNpcListCommand(npcStore));
    }

    @Override
    protected void executeSync(com.hypixel.hytale.server.core.command.system.CommandContext context) {
        context.sendMessage(com.hypixel.hytale.server.core.Message.raw("Usage: /enkaltar npc <spawn|list|remove>"));
    }
}
