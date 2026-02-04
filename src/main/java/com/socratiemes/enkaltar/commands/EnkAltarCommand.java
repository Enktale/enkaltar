package com.socratiemes.enkaltar.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public final class EnkAltarCommand extends CommandBase {
    public EnkAltarCommand() {
        super("enkaltar", "EnkAltar commands");
    }

    @Override
    protected void executeSync(CommandContext context) {
        context.sendMessage(Message.raw("Usage: /enkaltar reload"));
    }
}
