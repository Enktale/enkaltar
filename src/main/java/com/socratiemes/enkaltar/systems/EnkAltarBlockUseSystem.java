package com.socratiemes.enkaltar.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.socratiemes.enkaltar.EnkAltarPlugin;
import com.socratiemes.enkaltar.config.EnkAltarConfig;

import java.util.function.Supplier;

public final class EnkAltarBlockUseSystem
        extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {
    private final EnkAltarPlugin plugin;
    private final Supplier<EnkAltarConfig> configSupplier;

    public EnkAltarBlockUseSystem(EnkAltarPlugin plugin, Supplier<EnkAltarConfig> configSupplier) {
        super(UseBlockEvent.Post.class);
        this.plugin = plugin;
        this.configSupplier = configSupplier;
    }

    @Override
    public com.hypixel.hytale.component.query.Query<EntityStore> getQuery() {
        return com.hypixel.hytale.component.query.Query.any();
    }

    @Override
    public void handle(
            int entityId,
            com.hypixel.hytale.component.ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            UseBlockEvent.Post event
    ) {
        if (event == null || store == null) {
            return;
        }

        BlockType blockType = event.getBlockType();
        if (blockType == null || blockType.getId() == null) {
            return;
        }

        EnkAltarConfig config = configSupplier.get();
        if (config == null || !config.getAltarBlockId().equals(blockType.getId())) {
            return;
        }

        InteractionType actionType = event.getInteractionType();
        if (actionType != InteractionType.Use
                && actionType != InteractionType.Secondary
                && actionType != InteractionType.Primary) {
            return;
        }

        InteractionContext context = event.getContext();
        if (context == null) {
            return;
        }

        Ref<EntityStore> ref = context.getEntity();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        plugin.handleAltarUse(player, event.getTargetBlock());
    }
}
