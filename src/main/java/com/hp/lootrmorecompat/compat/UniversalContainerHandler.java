package com.hp.lootrmorecompat.compat;

import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;

import java.util.UUID;

/**
 * 通用第三方模组战利品容器原地兼容处理器（1.21.1 NeoForge）：
 * 兼容所有实现 RandomizableContainer 的自定义容器方块实体（包括 RandomizableContainerBlockEntity
 * 及直接实现接口的深暗之园 DepthrockPot、深幽暗域 GloomslatePot、Cobblemon 镀金箱、冰火幽灵箱等），
 * 保留其原本方块模型与 9~54 格精确容量，原地接入按玩家独立的 Lootr 战利品库存。
 */
public final class UniversalContainerHandler {
    private static final ResourceLocation ICE_AND_FIRE_GHOST_CHEST =
            ResourceLocation.fromNamespaceAndPath("iceandfire", "ghost_chest");

    private UniversalContainerHandler() {
    }

    public static boolean handleRightClick(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            BlockEntity blockEntity
    ) {
        if (!ModConfig.COMMON.enableUniversalModdedContainers.get()) {
            return false;
        }
        if (!(blockEntity instanceof RandomizableContainer rc)) {
            return false;
        }
        // 已属于 Lootr 原生容器则直接跳过
        if (blockEntity instanceof ILootrBlockEntity || LootrAPI.resolveBlockEntity(blockEntity) != null) {
            return false;
        }

        // 防御性校验：跳过任意模组带有未解锁/锁定状态的方块，防止绕过原模组钥匙或机关锁
        var blockState = blockEntity.getBlockState();
        var unlockedProp = ConfluenceCompatHandler.findBooleanProperty(blockState, "unlocked");
        if (unlockedProp != null && !blockState.getValue(unlockedProp)) {
            return false;
        }
        var lockedProp = ConfluenceCompatHandler.findBooleanProperty(blockState, "locked");
        if (lockedProp != null && blockState.getValue(lockedProp)) {
            return false;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (blockId == null) {
            return false;
        }
        if (ModConfig.COMMON.blacklistedMods.get().contains(blockId.getNamespace())) {
            return false;
        }
        if (ModConfig.COMMON.blacklistedBlocks.get().contains(blockId.toString())) {
            return false;
        }

        ResourceKey<LootTable> lootTableKey = rc.getLootTable();
        if (lootTableKey == null) {
            return false;
        }
        if (LootrAPI.isLootTableBlacklisted(lootTableKey)) {
            return false;
        }

        Component displayName = blockEntity instanceof Nameable nameable
                ? nameable.getDisplayName()
                : blockEntity.getBlockState().getBlock().getName();

        LmcSavedData savedData = LmcSavedData.get(level);
        LmcContainerRecord record = new LmcContainerRecord(
                UUID.randomUUID(),
                lootTableKey.location(),
                rc.getLootTableSeed(),
                LmcContainerRecord.TYPE_UNIVERSAL,
                blockId.toString(),
                rc.getContainerSize(),
                displayName,
                null
        );
        savedData.putRecord(pos, record);

        // 清空原方块实体的共享战利品表，避免被漏斗或其它模组以共享模式抽空
        rc.setLootTable(null);
        blockEntity.setChanged();
        ModNetworking.syncNewRecordToTracking(level, pos, record);

        // 冰火传说幽灵宝箱首次开启时保留一次原版幽灵伏击刷怪体验
        if (ICE_AND_FIRE_GHOST_CHEST.equals(blockId)
                && ModConfig.COMMON.preserveIceAndFireGhostAmbush.get()
                && blockEntity instanceof Container container) {
            container.startOpen(player);
            container.stopOpen(player);
        }

        return LmcLootrBridge.openCompatContainer(level, pos, player, record);
    }
}
