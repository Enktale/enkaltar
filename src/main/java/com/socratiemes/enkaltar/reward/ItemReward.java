package com.socratiemes.enkaltar.reward;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.socratiemes.enkaltar.config.EnkAltarConfig;
import com.socratiemes.enkaltar.config.LangConfig;

import java.util.Map;

public final class ItemReward implements RewardEntry {
    private final String itemId;
    private final int amount;

    public ItemReward(String itemId, int amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    @Override
    public boolean apply(Player player, EnkAltarConfig config, LangConfig lang) {
        if (player == null) {
            return false;
        }

        ItemStack stack = new ItemStack(itemId, amount);
        ItemContainer container = player.getInventory().getCombinedBackpackStorageHotbar();
        if (!container.canAddItemStack(stack)) {
            lang.send(player, "enkaltar.reward.inventory_full", null);
            return false;
        }

        container.addItemStack(stack);
        lang.send(player, "enkaltar.reward.item", Map.of(
            "item", resolveItemName(itemId),
            "amount", Integer.toString(amount)
        ));
        return true;
    }

    @Override
    public String describe() {
        return itemId + ":" + amount;
    }

    private String resolveItemName(String id) {
        if (id == null || id.isBlank()) {
            return id;
        }

        Item item = Item.getAssetMap().getAsset(id);
        if (item == null) {
            return id;
        }

        String key = item.getTranslationKey();
        if (key == null || key.isBlank()) {
            return id;
        }

        I18nModule module = I18nModule.get();
        if (module == null) {
            return id;
        }

        String translated = module.getMessage(I18nModule.DEFAULT_LANGUAGE, key);
        if (translated == null || translated.isBlank()) {
            return id;
        }
        return translated;
    }
}
