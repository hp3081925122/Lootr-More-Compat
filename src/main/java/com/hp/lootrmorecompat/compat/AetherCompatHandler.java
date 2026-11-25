package com.hp.lootrmorecompat.compat;

import com.aetherteam.aether.blockentity.TreasureChestBlockEntity;
import com.aetherteam.aether.item.miscellaneous.DungeonKeyItem;
import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.mixin.RandomizableContainerBlockEntityAccessor;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

/**
 * 天境（The Aether）1.20.1 地牢 Boss 宝箱专精兼容处理。
 * 仅在检测到 aether 模组已加载时才会被类加载。
 */
public final class AetherCompatHandler {
    private AetherCompatHandler() {
    }

    public static boolean isAetherTreasureChest(BlockEntity blockEntity) {
        return blockEntity instanceof TreasureChestBlockEntity;
    }

    public static boolean handleRightClick(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            InteractionHand hand,
            BlockEntity blockEntity
    ) {
        if (!ModConfig.COMMON.enableAetherTreasureChest.get()) {
            return false;
        }
        if (!(blockEntity instanceof TreasureChestBlockEntity treasureChest)) {
            return false;
        }

        LmcSavedData savedData = LmcSavedData.get(level);
        LmcContainerRecord record = savedData.getRecord(pos);
        RandomizableContainerBlockEntityAccessor accessor = (RandomizableContainerBlockEntityAccessor) treasureChest;

        // 若宝箱仍带原始战利品表且尚未登记，先接管其战利品表并清空共享战利品表
        ResourceLocation lootTableId = accessor.lmc$getLootTable();
        if (record == null && lootTableId != null) {
            ResourceLocation kind = treasureChest.getKind();
            record = new LmcContainerRecord(
                    UUID.randomUUID(),
                    lootTableId,
                    accessor.lmc$getLootTableSeed(),
                    LmcContainerRecord.TYPE_AETHER_TREASURE,
                    kind != null ? kind.toString() : "",
                    treasureChest.getContainerSize(),
                    treasureChest.getDisplayName(),
                    null
            );
            savedData.putRecord(pos, record);
            accessor.lmc$setLootTable(null);
            treasureChest.setChanged();
            ModNetworking.syncNewRecordToTracking(level, pos, record);
        }

        if (record == null) {
            return false;
        }

        if (treasureChest.getLocked()) {
            if (!ModConfig.COMMON.aetherPerPlayerKeyUnlock.get()) {
                // 共享解锁模式：放行给天境原版进行钥匙校验、音效与首次解锁
                return false;
            }

            // 独立钥匙解锁模式：每位玩家需各自用对应地牢钥匙解锁自己的独立战利品
            if (record.hasUnlocked(player.getUUID())) {
                return LmcLootrBridge.openCompatContainer(level, pos, player, record);
            }

            ItemStack held = player.getItemInHand(hand);
            ResourceLocation chestKind = treasureChest.getKind();
            if (!held.isEmpty()
                    && held.getItem() instanceof DungeonKeyItem keyItem
                    && chestKind != null
                    && chestKind.equals(keyItem.getDungeonType())) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                record.markUnlocked(player.getUUID());
                savedData.setDirty();
                level.playSound(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS,
                        0.8F,
                        1.15F
                );
                player.displayClientMessage(
                        Component.translatable("lootr_more_compat.aether.unlocked_personal").withStyle(ChatFormatting.GOLD),
                        true
                );
                LmcLootrBridge.openCompatContainer(level, pos, player, record);
                return true;
            }
            return false;
        }

        return LmcLootrBridge.openCompatContainer(level, pos, player, record);
    }
}
