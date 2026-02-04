package com.socratiemes.enkaltar.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.AudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.MovementAudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.socratiemes.enkaltar.config.EnkAltarConfig;
import com.socratiemes.enkaltar.npc.AltarNpcRecord;
import com.socratiemes.enkaltar.npc.AltarNpcRuntimeService;
import com.socratiemes.enkaltar.npc.AltarNpcStore;
import it.unimi.dsi.fastutil.Pair;

import java.util.UUID;
import java.util.function.Supplier;

public final class EnkAltarNpcSpawnCommand extends CommandBase {
    private final Supplier<AltarNpcStore> npcStore;
    private final Supplier<AltarNpcRuntimeService> npcRuntimeService;
    private final Supplier<EnkAltarConfig> config;

    public EnkAltarNpcSpawnCommand(
            Supplier<AltarNpcStore> npcStore,
            Supplier<AltarNpcRuntimeService> npcRuntimeService,
            Supplier<EnkAltarConfig> config
    ) {
        super("spawn", "Spawn an EnkAltar NPC");
        this.npcStore = npcStore;
        this.npcRuntimeService = npcRuntimeService;
        this.config = config;
    }

    @Override
    protected void executeSync(CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by a player."));
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(context.sender().getUuid());
        if (playerRef == null) {
            context.sendMessage(Message.raw("Unable to find player."));
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            playerRef.sendMessage(Message.raw("Failed to spawn altar NPC."));
            return;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            playerRef.sendMessage(Message.raw("Failed to spawn altar NPC."));
            return;
        }

        EnkAltarConfig cfg = config.get();
        String modelAssetId = cfg == null ? null : cfg.getNpcModelAssetId();
        if (modelAssetId == null || modelAssetId.isBlank()) {
            playerRef.sendMessage(Message.raw("Missing altar NPC model asset id."));
            return;
        }

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (modelAsset == null) {
            playerRef.sendMessage(Message.raw("Could not find model asset: " + modelAssetId));
            return;
        }

        String roleName = cfg == null ? null : cfg.getNpcRoleName();
        if (roleName == null || roleName.isBlank()) {
            playerRef.sendMessage(Message.raw("Missing altar NPC role name."));
            return;
        }

        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getIndex(roleName);
        if (roleIndex < 0) {
            playerRef.sendMessage(Message.raw("NPC role not found: " + roleName));
            return;
        }

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                playerRef.sendMessage(Message.raw("Failed to spawn altar NPC."));
                return;
            }

            Vector3d position = new Vector3d(transform.getPosition());
            Vector3f rotation = new Vector3f(transform.getRotation());
            Model model = Model.createUnitScaleModel(modelAsset);
            Pair<Ref<EntityStore>, NPCEntity> spawnResult = npcPlugin.spawnEntity(store, roleIndex, position, rotation, model, null);
            if (spawnResult == null || spawnResult.first() == null || !spawnResult.first().isValid()) {
                playerRef.sendMessage(Message.raw("Failed to spawn altar NPC."));
                return;
            }

            Ref<EntityStore> npcRef = spawnResult.first();
            store.tryRemoveComponent(npcRef, com.hypixel.hytale.server.core.entity.Frozen.getComponentType());
            store.ensureComponent(npcRef, Invulnerable.getComponentType());
            store.ensureComponent(npcRef, Interactable.getComponentType());
            store.tryRemoveComponent(npcRef, AudioComponent.getComponentType());
            store.tryRemoveComponent(npcRef, MovementAudioComponent.getComponentType());

            Velocity velocity = store.ensureAndGetComponent(npcRef, Velocity.getComponentType());
            velocity.setZero();
            velocity.setClient(0, 0, 0);

            MovementStatesComponent movementStatesComponent = store.ensureAndGetComponent(npcRef, MovementStatesComponent.getComponentType());
            MovementStates movementStates = new MovementStates();
            movementStates.idle = true;
            movementStates.horizontalIdle = true;
            movementStates.onGround = true;
            movementStatesComponent.setMovementStates(movementStates);

            String displayName = cfg == null ? null : cfg.getNpcDisplayName();
            if (displayName != null && !displayName.isBlank()) {
                store.putComponent(npcRef, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(displayName)));
            }

            NPCEntity npcEntity = spawnResult.second();
            UUID uuid = npcEntity == null ? null : npcEntity.getUuid();
            if (uuid == null) {
                com.hypixel.hytale.server.core.entity.UUIDComponent uuidComponent = store.getComponent(npcRef, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    uuid = uuidComponent.getUuid();
                }
            }
            if (uuid == null) {
                playerRef.sendMessage(Message.raw("Failed to spawn altar NPC."));
                return;
            }

            int id = npcStore.get().allocateId();
            AltarNpcRecord record = new AltarNpcRecord();
            record.id = id;
            record.uuid = uuid;
            record.roleName = roleName;
            record.displayName = displayName;
            record.worldName = world.getName();
            record.x = position.x;
            record.y = position.y;
            record.z = position.z;
            npcStore.get().save(record);
            npcRuntimeService.get().syncNpcRuntime();

            playerRef.sendMessage(Message.raw("Spawned altar NPC with id " + id + "."));
        });
    }
}
