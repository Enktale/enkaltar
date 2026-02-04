package com.socratiemes.enkaltar.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.socratiemes.enkaltar.config.LangConfig;

import java.util.function.Supplier;

public final class EnkAltarReloadCommand extends CommandBase {
    private final Supplier<LangConfig> langSupplier;
    private final ReloadHandler reloadHandler;

    public interface ReloadHandler {
        void reload() throws Exception;
    }

    public EnkAltarReloadCommand(Supplier<LangConfig> langSupplier, ReloadHandler reloadHandler) {
        super("reload", "Reload EnkAltar configuration");
        this.langSupplier = langSupplier;
        this.reloadHandler = reloadHandler;
        requirePermission("enkaltar.reload");
    }

    @Override
    protected void executeSync(CommandContext context) {
        LangConfig lang = langSupplier.get();
        try {
            reloadHandler.reload();
            String message = lang != null ? lang.format("enkaltar.reload.success", null) : "EnkAltar reloaded.";
            context.sendMessage(Message.raw(message));
        } catch (Exception ex) {
            String message = lang != null ? lang.format("enkaltar.reload.failure", null) : "Failed to reload EnkAltar configuration.";
            context.sendMessage(Message.raw(message));
        }
    }
}
