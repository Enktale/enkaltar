package com.socratiemes.enkaltar.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.component.AudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.MovementAudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class AltarNpcRuntimeService {
    private final Supplier<AltarNpcStore> npcStore;
    private final Supplier<String> modelAssetId;
    private final Supplier<String> roleName;
    private final Supplier<String> interactionHint;
    private final Supplier<String> displayName;

    public AltarNpcRuntimeService(
            Supplier<AltarNpcStore> npcStore,
            Supplier<String> modelAssetId,
            Supplier<String> roleName,
            Supplier<String> interactionHint,
            Supplier<String> displayName
    ) {
        this.npcStore = npcStore;
        this.modelAssetId = modelAssetId;
        this.roleName = roleName;
        this.interactionHint = interactionHint;
        this.displayName = displayName;
    }

    public void syncNpcRuntime() {
        List<AltarNpcRecord> records = npcStore.get().getAll();
        if (records.isEmpty()) {
            return;
        }
        Map<String, List<AltarNpcRecord>> byWorld = groupByWorld(records);
        for (Map.Entry<String, List<AltarNpcRecord>> entry : byWorld.entrySet()) {
            World world = Universe.get().getWorld(entry.getKey());
            if (world == null) {
                continue;
            }
            List<AltarNpcRecord> worldRecords = entry.getValue();
            world.execute(() -> applyRuntime(world, worldRecords));
        }
    }

    public void pollNpcInteractions() {
        List<AltarNpcRecord> records = npcStore.get().getAll();
        if (records.isEmpty()) {
            return;
        }
        Map<String, List<AltarNpcRecord>> byWorld = groupByWorld(records);
        for (Map.Entry<String, List<AltarNpcRecord>> entry : byWorld.entrySet()) {
            World world = Universe.get().getWorld(entry.getKey());
            if (world == null) {
                continue;
            }
            List<AltarNpcRecord> worldRecords = entry.getValue();
            world.execute(() -> tickNpcRuntime(world, worldRecords));
        }
    }

    private void applyRuntime(World world, List<AltarNpcRecord> records) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        for (AltarNpcRecord record : records) {
            if (record == null || record.uuid == null) {
                continue;
            }
            Ref<EntityStore> ref = world.getEntityRef(record.uuid);
            if (ref == null || !ref.isValid()) {
                Ref<EntityStore> spawned = respawnNpc(world, store, record);
                if (spawned == null) {
                    continue;
                }
                ref = spawned;
            }

            NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
            applyAnchor(store, ref, record);
            applyStillAndSilent(store, ref);
            applyLeash(npcEntity, record);
            applyModel(store, ref);
            applyInteractionConfig(store, ref);
        }
    }

    private void tickNpcRuntime(World world, List<AltarNpcRecord> records) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        for (AltarNpcRecord record : records) {
            if (record == null || record.uuid == null) {
                continue;
            }
            Ref<EntityStore> ref = world.getEntityRef(record.uuid);
            if (ref == null || !ref.isValid()) {
                continue;
            }
            NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
            applyAnchor(store, ref, record);
            applyStillAndSilent(store, ref);
            applyLeash(npcEntity, record);
            applyInteractionConfig(store, ref);
        }
    }

    private void applyStillAndSilent(Store<EntityStore> store, Ref<EntityStore> ref) {
        store.tryRemoveComponent(ref, com.hypixel.hytale.server.core.entity.Frozen.getComponentType());
        store.ensureComponent(ref, Invulnerable.getComponentType());
        store.tryRemoveComponent(ref, AudioComponent.getComponentType());
        store.tryRemoveComponent(ref, MovementAudioComponent.getComponentType());
        store.tryRemoveComponent(ref, ActiveAnimationComponent.getComponentType());

        Velocity velocity = store.ensureAndGetComponent(ref, Velocity.getComponentType());
        velocity.setZero();
        velocity.setClient(0, 0, 0);

        MovementStatesComponent movementStatesComponent = store.ensureAndGetComponent(ref, MovementStatesComponent.getComponentType());
        MovementStates movementStates = new MovementStates();
        movementStates.idle = true;
        movementStates.horizontalIdle = true;
        movementStates.onGround = true;
        movementStatesComponent.setMovementStates(movementStates);
    }

    private void applyAnchor(Store<EntityStore> store, Ref<EntityStore> ref, AltarNpcRecord record) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d desired = new Vector3d(record.x, record.y, record.z);
        transform.setPosition(desired);
    }

    private void applyLeash(NPCEntity npcEntity, AltarNpcRecord record) {
        if (npcEntity == null) {
            return;
        }
        npcEntity.setLeashPoint(new Vector3d(record.x, record.y, record.z));
    }

    private void applyInteractionConfig(Store<EntityStore> store, Ref<EntityStore> ref) {
        store.ensureComponent(ref, Interactable.getComponentType());
        Interactions interactions = store.getComponent(ref, Interactions.getComponentType());
        if (interactions == null) {
            interactions = new Interactions();
            store.putComponent(ref, Interactions.getComponentType(), interactions);
        }
        for (InteractionType type : InteractionType.values()) {
            interactions.setInteractionId(type, null);
        }
        String hint = interactionHint.get();
        if (hint != null && !hint.isBlank()) {
            interactions.setInteractionHint(hint);
        }
    }

    private void applyModel(Store<EntityStore> store, Ref<EntityStore> ref) {
        String modelId = modelAssetId.get();
        if (modelId == null || modelId.isBlank()) {
            return;
        }
        ModelAsset asset = resolveModelAsset(modelId);
        if (asset == null) {
            return;
        }
        ModelComponent existing = store.getComponent(ref, ModelComponent.getComponentType());
        if (existing != null && asset.getId().equalsIgnoreCase(existing.getModel().getModelAssetId())) {
            return;
        }
        Model model = Model.createUnitScaleModel(asset);
        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(model));
        store.putComponent(ref, PersistentModel.getComponentType(), new PersistentModel(model.toReference()));

        String name = displayName.get();
        if (name != null && !name.isBlank()) {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(com.hypixel.hytale.server.core.Message.raw(name)));
        }
    }

    private Ref<EntityStore> respawnNpc(World world, Store<EntityStore> store, AltarNpcRecord record) {
        String modelId = modelAssetId.get();
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        ModelAsset modelAsset = resolveModelAsset(modelId);
        if (modelAsset == null) {
            return null;
        }
        String role = record.roleName == null || record.roleName.isBlank() ? roleName.get() : record.roleName;
        if (role == null || role.isBlank()) {
            return null;
        }
        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getIndex(role);
        if (roleIndex < 0) {
            return null;
        }
        Vector3d position = new Vector3d(record.x, record.y, record.z);
        Vector3f rotation = new Vector3f(0, 0, 0);
        Model model = Model.createUnitScaleModel(modelAsset);
        it.unimi.dsi.fastutil.Pair<Ref<EntityStore>, NPCEntity> spawnResult = npcPlugin.spawnEntity(store, roleIndex, position, rotation, model, null);
        if (spawnResult == null || spawnResult.first() == null || !spawnResult.first().isValid()) {
            return null;
        }
        Ref<EntityStore> npcRef = spawnResult.first();
        NPCEntity npcEntity = spawnResult.second();
        UUID uuid = npcEntity == null ? null : npcEntity.getUuid();
        if (uuid != null) {
            record.uuid = uuid;
            npcStore.get().save(record);
        }
        return npcRef;
    }

    private ModelAsset resolveModelAsset(String modelId) {
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(modelId);
        if (asset != null) {
            return asset;
        }

        String candidate = modelId;
        if (candidate.endsWith(".blockymodel")) {
            candidate = candidate.substring(0, candidate.length() - ".blockymodel".length());
        } else {
            candidate = candidate + ".blockymodel";
        }
        asset = ModelAsset.getAssetMap().getAsset(candidate);
        if (asset != null) {
            return asset;
        }

        if (!modelId.startsWith("Common/")) {
            asset = ModelAsset.getAssetMap().getAsset("Common/" + modelId);
            if (asset != null) {
                return asset;
            }
            asset = ModelAsset.getAssetMap().getAsset("Common/" + candidate);
            if (asset != null) {
                return asset;
            }
        }
        return null;
    }

    private Map<String, List<AltarNpcRecord>> groupByWorld(List<AltarNpcRecord> records) {
        Map<String, List<AltarNpcRecord>> byWorld = new HashMap<>();
        for (AltarNpcRecord record : records) {
            if (record == null || record.worldName == null || record.worldName.isBlank()) {
                continue;
            }
            byWorld.computeIfAbsent(record.worldName, key -> new java.util.ArrayList<>()).add(record);
        }
        return byWorld;
    }
}
