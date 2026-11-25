package com.hp.lootrmorecompat.event;

import com.hp.lootrmorecompat.compat.AetherCompatHandler;
import com.hp.lootrmorecompat.compat.AquamiraeCompatHandler;
import com.hp.lootrmorecompat.compat.BreakLootBlockHandler;
import com.hp.lootrmorecompat.compat.UniversalContainerHandler;
import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLidTracker;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

/**
 * 通用服务端事件分发（1.20.1 Forge）：拦截右键交互接入 Lootr、防误拆保护、区块同步。
 */
public final class CommonEvents {
    private static final boolean AETHER_LOADED = ModList.get().isLoaded("aether");

    private CommonEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSecondaryUseActive() && !player.getItemInHand(event.getHand()).isEmpty()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);
        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);

        // 1. 天境地牢宝箱专精处理
        if (AETHER_LOADED && blockEntity != null && AetherCompatHandler.isAetherTreasureChest(blockEntity)) {
            if (AetherCompatHandler.handleRightClick(serverLevel, pos, player, event.getHand(), blockEntity)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }

        // 1.5 深海秘境冰冻宝箱专精处理
        if (AquamiraeCompatHandler.isFrozenChest(state)) {
            if (AquamiraeCompatHandler.handleRightClick(serverLevel, pos, player, event.getHand(), state)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }

        // 2. 已登记的兼容容器
        LmcContainerRecord existing = LmcSavedData.get(serverLevel).getRecord(pos);
        if (existing != null) {
            if (LmcLootrBridge.openCompatContainer(serverLevel, pos, player, existing)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }

        // 3. 破坏掉落型战利品方块
        if (BreakLootBlockHandler.handleRightClick(serverLevel, pos, player, state)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        // 4. 通用第三方模组 RandomizableContainerBlockEntity 原地接管
        if (blockEntity != null && UniversalContainerHandler.handleRightClick(serverLevel, pos, player, blockEntity)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        LmcSavedData savedData = LmcSavedData.get(serverLevel);
        if (!savedData.hasRecord(pos)) {
            return;
        }

        if (ModConfig.COMMON.protectCompatContainersFromBreak.get()
                && event.getPlayer() instanceof ServerPlayer player
                && !player.isShiftKeyDown()
                && !player.getAbilities().instabuild) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("lootr_more_compat.message.sneak_to_break").withStyle(ChatFormatting.GOLD),
                    true
            );
            return;
        }

        savedData.removeRecord(pos);
        LmcLidTracker.clearPosition(serverLevel, pos);
        ModNetworking.syncRemovedToTracking(serverLevel, pos);
    }

    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ModNetworking.syncChunkToPlayer(event.getLevel(), event.getPlayer(), event.getPos());
    }

    public static EventPriority rightClickPriority() {
        return EventPriority.HIGH;
    }
}
