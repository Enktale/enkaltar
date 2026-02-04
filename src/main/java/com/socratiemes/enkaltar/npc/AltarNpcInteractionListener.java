package com.socratiemes.enkaltar.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.socratiemes.enkaltar.EnkAltarPlugin;

import java.util.UUID;
import java.util.function.Supplier;

public final class AltarNpcInteractionListener {
    private final Supplier<AltarNpcStore> npcStore;
    private final Supplier<EnkAltarPlugin> plugin;

    public AltarNpcInteractionListener(Supplier<AltarNpcStore> npcStore, Supplier<EnkAltarPlugin> plugin) {
        this.npcStore = npcStore;
        this.plugin = plugin;
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null) {
            return;
        }
        Entity target = event.getTargetEntity();
        if (target == null) {
            Ref<EntityStore> targetRef = event.getTargetRef();
            if (targetRef != null && targetRef.isValid()) {
                Store<EntityStore> store = targetRef.getStore();
                target = EntityUtils.getEntity(targetRef, store);
            }
        }
        UUID targetUuid = null;
        if (target != null) {
            @SuppressWarnings("removal")
            UUID legacyUuid = target.getUuid();
            targetUuid = legacyUuid;
        }
        if (targetUuid == null) {
            Ref<EntityStore> targetRef = event.getTargetRef();
            if (targetRef != null && targetRef.isValid()) {
                Store<EntityStore> store = targetRef.getStore();
                UUIDComponent uuidComponent = store.getComponent(targetRef, UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    targetUuid = uuidComponent.getUuid();
                }
            }
        }
        if (targetUuid == null) {
            return;
        }
        AltarNpcRecord record = npcStore.get().getByUuid(targetUuid);
        if (record == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        EnkAltarPlugin pluginInstance = plugin.get();
        if (pluginInstance == null) {
            return;
        }
        Vector3d position = new Vector3d(record.x, record.y, record.z);
        pluginInstance.handleNpcUse(player, event.getItemInHand(), position);
    }
}
