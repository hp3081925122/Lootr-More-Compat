package com.hp.lootrmorecompat.compat;

import com.aetherteam.aether.blockentity.TreasureChestBlockEntity;
import com.aetherteam.aether.item.components.AetherDataComponents;
import com.aetherteam.aether.item.components.DungeonKind;
import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.UUID;

/**
 * 天境（The Aether）地牢 Boss 宝箱（青铜/白银/黄金宝箱）专精兼容处理。
 * 仅在检测到 aether 模组已加载时才会被类加载。
 */
public final class AetherCompatHandler {
    private AetherCompatHandler() {
    }

    public static boolean isAetherTreasureChest(BlockEntity blockEntity) {
        return blockEntity instanceof TreasureChestBlockEntity;
    }

    /**
     * 处理玩家右键天境地牢宝箱的交互。
     *
     * @return true 表示已由本模组接管并取消原版打开流程；false 表示放行给天境原版逻辑处理（如钥匙校验与首次解锁）。
     */
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

        // 若宝箱仍带原始战利品表且尚未登记，先接管其战利品表并清空共享战利品表，防止后续解锁时被单人抽空
        ResourceKey<LootTable> lootTableKey = treasureChest.getLootTable();
        if (record == null && lootTableKey != null) {
            ResourceLocation kind = treasureChest.getKind();
            record = new LmcContainerRecord(
                    UUID.randomUUID(),
                    lootTableKey.location(),
                    treasureChest.getLootTableSeed(),
                    LmcContainerRecord.TYPE_AETHER_TREASURE,
                    kind != null ? kind.toString() : "",
                    treasureChest.getContainerSize(),
                    treasureChest.getDisplayName(),
                    null
            );
            savedData.putRecord(pos, record);
            treasureChest.setLootTable(null);
            treasureChest.setChanged();
            ModNetworking.syncNewRecordToTracking(level, pos, record);
        }

        if (record == null) {
            // 非自然生成（无战利品表）的装饰性或历史已开宝箱，完全放行原版逻辑
            return false;
        }

        // 宝箱处于上锁状态时的两种配置策略
        if (treasureChest.getLocked()) {
            if (!ModConfig.COMMON.aetherPerPlayerKeyUnlock.get()) {
                // 共享解锁模式：放行给天境原版进行钥匙校验、音效与首次解锁；解锁完成后再次右键即进入独立战利品箱
                return false;
            }

            // 独立钥匙解锁模式：每位玩家需各自用对应地牢钥匙解锁自己的独立战利品，方块本体保持锁定以提示其他队友
            if (record.hasUnlocked(player.getUUID())) {
                return LmcLootrBridge.openCompatContainer(level, pos, player, record);
            }

            ItemStack held = player.getItemInHand(hand);
            DungeonKind itemKind = held.get(AetherDataComponents.DUNGEON_KIND);
            ResourceLocation chestKind = treasureChest.getKind();
            if (itemKind != null && chestKind != null && chestKind.equals(itemKind.id()) && !held.isEmpty()) {
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
            // 未持对应钥匙：放行给天境原版显示原生“需要对应钥匙”的提示
            return false;
        }

        // 宝箱已解锁：直接为当前玩家打开其专属 Lootr 战利品库存
        return LmcLootrBridge.openCompatContainer(level, pos, player, record);
    }
}
