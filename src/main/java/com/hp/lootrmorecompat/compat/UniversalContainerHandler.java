package com.hp.lootrmorecompat.compat;

import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.mixin.RandomizableContainerBlockEntityAccessor;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import noobanidus.mods.lootr.api.blockentity.ILootBlockEntity;
import noobanidus.mods.lootr.config.ConfigManager;

import java.util.UUID;

/**
 * 通用第三方模组战利品容器原地兼容处理器（1.20.1 Forge）。
 */
public final class UniversalContainerHandler {
    private static final ResourceLocation ICE_AND_FIRE_GHOST_CHEST =
            new ResourceLocation("iceandfire", "ghost_chest");

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
        if (!(blockEntity instanceof RandomizableContainerBlockEntity rcbe)) {
            return false;
        }
        if (blockEntity instanceof ILootBlockEntity) {
            return false;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (blockId == null) {
            return false;
        }
        if (ModConfig.COMMON.blacklistedMods.get().contains(blockId.getNamespace())) {
            return false;
        }
        if (ModConfig.COMMON.blacklistedBlocks.get().contains(blockId.toString())) {
            return false;
        }

        RandomizableContainerBlockEntityAccessor accessor = (RandomizableContainerBlockEntityAccessor) rcbe;
        ResourceLocation lootTableId = accessor.lmc$getLootTable();
        if (lootTableId == null) {
            return false;
        }
        if (ConfigManager.LOOT_TABLE_BLACKLIST.get().contains(lootTableId.toString())
                || ConfigManager.LOOT_MODID_BLACKLIST.get().contains(lootTableId.getNamespace())) {
            return false;
        }

        LmcSavedData savedData = LmcSavedData.get(level);
        LmcContainerRecord record = new LmcContainerRecord(
                UUID.randomUUID(),
                lootTableId,
                accessor.lmc$getLootTableSeed(),
                LmcContainerRecord.TYPE_UNIVERSAL,
                blockId.toString(),
                rcbe.getContainerSize(),
                rcbe.getDisplayName(),
                null
        );
        savedData.putRecord(pos, record);

        accessor.lmc$setLootTable(null);
        rcbe.setChanged();
        ModNetworking.syncNewRecordToTracking(level, pos, record);

        // 冰火传说幽灵宝箱首次开启时保留一次原版幽灵伏击刷怪体验
        if (ICE_AND_FIRE_GHOST_CHEST.equals(blockId)
                && ModConfig.COMMON.preserveIceAndFireGhostAmbush.get()) {
            rcbe.startOpen(player);
            rcbe.stopOpen(player);
        }

        return LmcLootrBridge.openCompatContainer(level, pos, player, record);
    }
}
