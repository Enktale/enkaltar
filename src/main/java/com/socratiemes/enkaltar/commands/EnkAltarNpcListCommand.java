package com.socratiemes.enkaltar.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.socratiemes.enkaltar.npc.AltarNpcRecord;
import com.socratiemes.enkaltar.npc.AltarNpcStore;

import java.util.List;
import java.util.function.Supplier;

public final class EnkAltarNpcListCommand extends CommandBase {
    private final Supplier<AltarNpcStore> npcStore;

    public EnkAltarNpcListCommand(Supplier<AltarNpcStore> npcStore) {
        super("list", "List EnkAltar NPCs");
        this.npcStore = npcStore;
    }

    @Override
    protected void executeSync(CommandContext context) {
        List<AltarNpcRecord> records = npcStore.get().getAll();
        if (records.isEmpty()) {
            context.sendMessage(Message.raw("No altar NPCs found."));
            return;
        }
        context.sendMessage(Message.raw("Altar NPCs:"));
        for (AltarNpcRecord record : records) {
            String world = record.worldName == null ? "?" : record.worldName;
            context.sendMessage(Message.raw(
                    " - " + record.id + " @ " + world + " (" + record.x + ", " + record.y + ", " + record.z + ")"
            ));
        }
    }
}
