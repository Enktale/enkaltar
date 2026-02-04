package com.socratiemes.enkaltar.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.socratiemes.enkaltar.npc.AltarNpcRecord;
import com.socratiemes.enkaltar.npc.AltarNpcStore;

import java.util.function.Supplier;

public final class EnkAltarNpcRemoveCommand extends CommandBase {
    private final Supplier<AltarNpcStore> npcStore;
    private final RequiredArg<Integer> idArg;

    public EnkAltarNpcRemoveCommand(Supplier<AltarNpcStore> npcStore) {
        super("remove", "Remove an EnkAltar NPC");
        this.npcStore = npcStore;
        this.idArg = withRequiredArg("id", "NPC id", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(CommandContext context) {
        Integer id = context.get(idArg);
        if (id == null || id <= 0) {
            context.sendMessage(Message.raw("Invalid NPC id."));
            return;
        }

        AltarNpcRecord record = npcStore.get().get(id);
        if (record == null) {
            context.sendMessage(Message.raw("NPC not found."));
            return;
        }

        if (record.worldName != null && record.uuid != null) {
            World world = Universe.get().getWorld(record.worldName);
            if (world != null) {
                world.execute(() -> {
                    com.hypixel.hytale.server.core.entity.Entity entity = world.getEntity(record.uuid);
                    if (entity != null) {
                        entity.remove();
                    }
                });
            }
        }

        npcStore.get().remove(id);
        context.sendMessage(Message.raw("Removed altar NPC " + id + "."));
    }
}
