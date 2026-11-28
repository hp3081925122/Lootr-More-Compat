package com.hp.lootrmorecompat.data;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 单个兼容容器的持久化记录（1.20.1 Forge），保存 Lootr 标识、战利品表、保底掉落物及已领取/已解锁玩家集合。
 */
public final class LmcContainerRecord {
    public static final String TYPE_AETHER_TREASURE = "aether_treasure";
    public static final String TYPE_TWILIGHT_BOSS = "twilight_boss";
    public static final String TYPE_AQUAMIRAE_FROZEN = "aquamirae_frozen";
    public static final String TYPE_CONFLUENCE_CHEST = "confluence_chest";
    public static final String TYPE_CONFLUENCE_DEATH_CHEST = "confluence_death_chest";
    public static final String TYPE_CONFLUENCE_LOST_PAPER = "confluence_lost_paper";
    public static final String TYPE_BREAK_LOOT = "break_loot";
    public static final String TYPE_UNIVERSAL = "universal";

    private UUID lootrId;
    @Nullable
    private ResourceLocation lootTable;
    private long lootTableSeed;
    private String compatType;
    private String extraMeta;
    private int containerSize;
    @Nullable
    private Component displayName;
    private NonNullList<ItemStack> fallbackItems;
    private final Set<UUID> openers = new HashSet<>();
    private final Set<UUID> unlockedPlayers = new HashSet<>();

    public LmcContainerRecord(
            UUID lootrId,
            @Nullable ResourceLocation lootTable,
            long lootTableSeed,
            String compatType,
            String extraMeta,
            int containerSize,
            @Nullable Component displayName,
            @Nullable List<ItemStack> fallbackItems
    ) {
        this.lootrId = lootrId;
        this.lootTable = lootTable;
        this.lootTableSeed = lootTableSeed;
        this.compatType = compatType;
        this.extraMeta = extraMeta == null ? "" : extraMeta;
        this.containerSize = normalizeSize(containerSize);
        this.displayName = displayName;
        this.fallbackItems = NonNullList.withSize(this.containerSize, ItemStack.EMPTY);
        if (fallbackItems != null) {
            for (int i = 0; i < Math.min(this.containerSize, fallbackItems.size()); i++) {
                ItemStack stack = fallbackItems.get(i);
                if (stack != null && !stack.isEmpty()) {
                    this.fallbackItems.set(i, stack.copy());
                }
            }
        }
    }

    public static int normalizeSize(int rawSize) {
        if (rawSize <= 9) {
            return 9;
        }
        int rows = Math.min(6, (rawSize + 8) / 9);
        return rows * 9;
    }

    public UUID getLootrId() {
        return lootrId;
    }

    public void resetForReloot() {
        this.lootrId = UUID.randomUUID();
        this.openers.clear();
        this.unlockedPlayers.clear();
    }

    @Nullable
    public ResourceLocation getLootTable() {
        return lootTable;
    }

    public long getLootTableSeed() {
        return lootTableSeed;
    }

    public String getCompatType() {
        return compatType;
    }

    public String getExtraMeta() {
        return extraMeta;
    }

    public int getContainerSize() {
        return containerSize;
    }

    @Nullable
    public Component getDisplayName() {
        return displayName;
    }

    public NonNullList<ItemStack> getFallbackItems() {
        return fallbackItems;
    }

    public boolean hasFallbackItems() {
        for (ItemStack stack : fallbackItems) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Set<UUID> getOpeners() {
        return openers;
    }

    public boolean hasOpened(UUID playerId) {
        return openers.contains(playerId);
    }

    public boolean markOpened(UUID playerId) {
        return openers.add(playerId);
    }

    public Set<UUID> getUnlockedPlayers() {
        return unlockedPlayers;
    }

    public boolean hasUnlocked(UUID playerId) {
        return unlockedPlayers.contains(playerId);
    }

    public boolean markUnlocked(UUID playerId) {
        return unlockedPlayers.add(playerId);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("LootrId", this.lootrId);
        if (this.lootTable != null) {
            tag.putString("LootTable", this.lootTable.toString());
        }
        tag.putLong("LootTableSeed", this.lootTableSeed);
        tag.putString("CompatType", this.compatType);
        if (!this.extraMeta.isEmpty()) {
            tag.putString("ExtraMeta", this.extraMeta);
        }
        tag.putInt("ContainerSize", this.containerSize);
        if (this.displayName != null) {
            tag.putString("DisplayName", Component.Serializer.toJson(this.displayName));
        }
        if (hasFallbackItems()) {
            CompoundTag itemsTag = new CompoundTag();
            ContainerHelper.saveAllItems(itemsTag, this.fallbackItems);
            tag.put("FallbackItems", itemsTag);
        }
        ListTag openerList = new ListTag();
        for (UUID uuid : this.openers) {
            openerList.add(NbtUtils.createUUID(uuid));
        }
        tag.put("Openers", openerList);

        ListTag unlockedList = new ListTag();
        for (UUID uuid : this.unlockedPlayers) {
            unlockedList.add(NbtUtils.createUUID(uuid));
        }
        tag.put("UnlockedPlayers", unlockedList);
        return tag;
    }

    public static LmcContainerRecord load(CompoundTag tag) {
        UUID lootrId = tag.hasUUID("LootrId") ? tag.getUUID("LootrId") : UUID.randomUUID();
        ResourceLocation lootTable = tag.contains("LootTable", Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString("LootTable"))
                : null;
        long seed = tag.getLong("LootTableSeed");
        String compatType = tag.getString("CompatType");
        String extraMeta = tag.getString("ExtraMeta");
        int size = normalizeSize(tag.getInt("ContainerSize"));
        Component displayName = null;
        if (tag.contains("DisplayName", Tag.TAG_STRING)) {
            displayName = Component.Serializer.fromJson(tag.getString("DisplayName"));
        }
        NonNullList<ItemStack> fallback = NonNullList.withSize(size, ItemStack.EMPTY);
        if (tag.contains("FallbackItems", Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(tag.getCompound("FallbackItems"), fallback);
        }

        LmcContainerRecord record = new LmcContainerRecord(
                lootrId, lootTable, seed, compatType, extraMeta, size, displayName, fallback
        );
        if (tag.contains("Openers", Tag.TAG_LIST)) {
            ListTag openerList = tag.getList("Openers", Tag.TAG_INT_ARRAY);
            for (Tag entry : openerList) {
                record.openers.add(NbtUtils.loadUUID(entry));
            }
        }
        if (tag.contains("UnlockedPlayers", Tag.TAG_LIST)) {
            ListTag unlockedList = tag.getList("UnlockedPlayers", Tag.TAG_INT_ARRAY);
            for (Tag entry : unlockedList) {
                record.unlockedPlayers.add(NbtUtils.loadUUID(entry));
            }
        }
        return record;
    }
}
