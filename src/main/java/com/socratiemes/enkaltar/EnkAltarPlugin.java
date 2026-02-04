package com.socratiemes.enkaltar;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.socratiemes.enkaltar.commands.EnkAltarCommand;
import com.socratiemes.enkaltar.commands.EnkAltarReloadCommand;
import com.socratiemes.enkaltar.config.EnkAltarConfig;
import com.socratiemes.enkaltar.config.LangConfig;
import com.socratiemes.enkaltar.reward.RewardService;
import com.socratiemes.enkaltar.systems.EnkAltarBlockUseSystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class EnkAltarPlugin extends JavaPlugin {
    private static final long USE_DEBOUNCE_MS = 250L;
    private EnkAltarConfig config;
    private LangConfig lang;
    private RewardService rewardService;
    private final Map<Ref<EntityStore>, Long> lastUseByPlayer = new HashMap<>();

    public EnkAltarPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        Path dataDirectory = getDataDirectory();
        this.config = new EnkAltarConfig(dataDirectory, getLogger());
        this.lang = new LangConfig(dataDirectory, getLogger());
        this.rewardService = new RewardService(() -> config, () -> lang);

        extractAssetPack(dataDirectory);

        try {
            reloadAll();
        } catch (IOException ex) {
            getLogger().atWarning().log("Failed to load EnkAltar configs: %s", ex.getMessage());
        }

        EnkAltarCommand root = new EnkAltarCommand();
        root.addSubCommand(new EnkAltarReloadCommand(() -> lang, this::reloadAll));
        getCommandRegistry().registerCommand(root);

        getEntityStoreRegistry().registerSystem(new EnkAltarBlockUseSystem(this, () -> config));

        logInfo(
                "EnkAltar enabled. altarBlockId=%s particleId=%s",
                config.getAltarBlockId(),
                config.getParticleId()
        );
    }

    private void reloadAll() throws IOException {
        config.reload();
        lang.reload();
        syncBlockHint();
    }

    public void handleAltarUse(Player player, Vector3i target) {
        if (player == null || !allowUse(player)) {
            if (player != null) {
                logInfo("handleUse: debounced");
            }
            return;
        }

        String votingOilItemId = config.getVotingOilItemId();
        if (votingOilItemId == null || votingOilItemId.isBlank()) {
            logInfo("handleUse: voting oil item id missing");
            return;
        }

        ItemStack oilStack = new ItemStack(votingOilItemId, 1);
        if (player.getInventory() == null
                || player.getInventory().getCombinedBackpackStorageHotbar() == null
                || !player.getInventory().getCombinedBackpackStorageHotbar().canRemoveItemStack(oilStack)) {
            logInfo("handleUse: voting oil missing");
            lang.send(player, "enkaltar.altar.need_oil", null);
            return;
        }

        player.getInventory().getCombinedBackpackStorageHotbar().removeItemStack(oilStack);

        if (target != null) {
            spawnParticles(player.getWorld(), target);
        }

        logInfo("handleUse: success (reward granted)");
        lang.send(player, "enkaltar.altar.used_oil", null);
        rewardService.grantReward(player);
    }

    // Legacy NPC hook retained for compatibility with old NPC classes (not used).
    public void handleNpcUse(Player player, ItemStack inHand, Vector3d npcPosition) {
        if (player == null) {
            return;
        }
        logInfo("handleNpcUse: ignored (NPC feature removed)");
    }

    private void logInfo(String message, Object... args) {
        boolean enabled = config == null || config.isLoggingEnabled();
        if (!enabled) {
            return;
        }
        String formatted;
        try {
            formatted = String.format(message, args);
        } catch (RuntimeException ex) {
            formatted = message;
        }
        getLogger().atInfo().log(formatted);
        System.out.println("[EnkAltar] " + formatted);
    }

    private void extractAssetPack(Path dataDirectory) {
        if (dataDirectory == null) {
            return;
        }
        try {
            Path manifestTarget = dataDirectory.resolve("manifest.json");
            try (InputStream in = EnkAltarPlugin.class.getResourceAsStream("/asset-manifest.json")) {
                if (in != null) {
                    Files.createDirectories(manifestTarget.getParent());
                    Files.copy(in, manifestTarget, StandardCopyOption.REPLACE_EXISTING);
                    logInfo("Wrote asset manifest to %s", manifestTarget);
                }
            }

            URL location = EnkAltarPlugin.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return;
            }
            Path jarPath = Path.of(location.toURI());
            if (!Files.isRegularFile(jarPath)) {
                return;
            }

            try (JarFile jar = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (!name.startsWith("Server/") && !name.startsWith("Common/")) {
                        continue;
                    }
                    Path target = dataDirectory.resolve(name);
                    Files.createDirectories(target.getParent());
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException | URISyntaxException ex) {
            getLogger().atWarning().log("Failed to extract asset pack: %s", ex.getMessage());
        }
    }

    private boolean allowUse(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return true;
        }

        long now = System.currentTimeMillis();
        Long lastUse = lastUseByPlayer.get(ref);
        if (lastUse != null && now - lastUse < USE_DEBOUNCE_MS) {
            return false;
        }

        lastUseByPlayer.put(ref, now);
        return true;
    }

    private void spawnParticles(World world, Vector3i blockPos) {
        String particleId = config.getParticleId();
        if (particleId == null || particleId.isEmpty()) {
            return;
        }

        Vector3d position = new Vector3d(blockPos).add(0.5, 0.6, 0.5);
        List<Ref<EntityStore>> receivers = new ArrayList<>();
        for (Player target : world.getPlayers()) {
            Ref<EntityStore> ref = target.getReference();
            if (ref != null && ref.isValid()) {
                receivers.add(ref);
            }
        }

        Store<EntityStore> accessor = world.getEntityStore().getStore();
        ParticleUtil.spawnParticleEffect(particleId, position, receivers, accessor);
    }

    private void spawnParticles(World world, Vector3d position) {
        String particleId = config.getParticleId();
        if (particleId == null || particleId.isEmpty() || world == null || position == null) {
            return;
        }
        List<Ref<EntityStore>> receivers = new ArrayList<>();
        for (Player target : world.getPlayers()) {
            Ref<EntityStore> ref = target.getReference();
            if (ref != null && ref.isValid()) {
                receivers.add(ref);
            }
        }
        Store<EntityStore> accessor = world.getEntityStore().getStore();
        ParticleUtil.spawnParticleEffect(particleId, position, receivers, accessor);
    }

    @Override
    protected void shutdown() {
        // Nothing to shutdown.
    }

    private void syncBlockHint() {
        if (lang == null || config == null) {
            return;
        }
        String hintText = config.getAltarHint();
        if (hintText == null || hintText.isBlank()) {
            return;
        }
        updateInteractionHint(
                getDataDirectory().resolve("Server/Item/Items/EnkAltar/EnkAltar_Altar.json"),
                "    ",
                hintText
        );
        updateInteractionHint(
                getDataDirectory().resolve("Server/Item/Block/Blocks/EnkAltar_Altar.json"),
                "  ",
                hintText
        );
    }

    private void updateInteractionHint(Path path, String indent, String hint) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String escapedHint = hint.replace("\\", "\\\\").replace("\"", "\\\"");
            String updated = content.replaceAll(
                    "\"InteractionHint\"\\s*:\\s*\"[^\"]*\"",
                    "\"InteractionHint\": \"" + escapedHint + "\""
            );
            if (updated.equals(content)) {
                updated = content.replaceFirst(
                        "\"Flags\"\\s*:\\s*\\{\\}",
                        "\"Flags\": {},\n" + indent + "\"InteractionHint\": \"" + escapedHint + "\""
                );
            }
            if (!updated.equals(content)) {
                Files.writeString(path, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            logInfo("Failed to update interaction hint at %s: %s", path, ex.getMessage());
        }
    }

}
