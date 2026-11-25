package com.hp.lootrmorecompat.compat;

import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 深海秘境（Aquamirae）冰冻宝箱（aquamirae:frozen_chest）专精兼容处理。
 * 无需强依赖 Aquamirae 类，通过注册表 ID 拦截冰冻钥匙解锁流程并接入 Lootr 独立战利品。
 */
public final class AquamiraeCompatHandler {
    private static final ResourceLocation FROZEN_CHEST_ID = ResourceLocation.fromNamespaceAndPath("aquamirae", "frozen_chest");
    private static final ResourceLocation FROZEN_KEY_ID = ResourceLocation.fromNamespaceAndPath("aquamirae", "frozen_key");
    private static final ResourceLocation FROZEN_LOOT_TABLE = ResourceLocation.fromNamespaceAndPath("aquamirae", "chests/frozen_chest");

    private AquamiraeCompatHandler() {
    }

    public static boolean isFrozenChest(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return FROZEN_CHEST_ID.equals(id);
    }

    public static boolean handleRightClick(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            InteractionHand hand,
            BlockState state
    ) {
        if (!ModConfig.COMMON.enableAquamiraeFrozenChest.get()) {
            return false;
        }
        if (!isFrozenChest(state)) {
            return false;
        }

        LmcSavedData savedData = LmcSavedData.get(level);
        LmcContainerRecord record = savedData.getRecord(pos);

        // 独立钥匙模式且该玩家此前已解锁过，直接打开其个人 Lootr 战利品
        if (ModConfig.COMMON.aquamiraePerPlayerKeyUnlock.get() && record != null && record.hasUnlocked(player.getUUID())) {
            return LmcLootrBridge.openCompatContainer(level, pos, player, record);
        }

        ItemStack held = player.getItemInHand(hand);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (held.isEmpty() || !FROZEN_KEY_ID.equals(itemId)) {
            // 未持冰冻钥匙：放行原版提示
            return false;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        if (record == null) {
            record = new LmcContainerRecord(
                    UUID.randomUUID(),
                    FROZEN_LOOT_TABLE,
                    level.getRandom().nextLong(),
                    LmcContainerRecord.TYPE_AQUAMIRAE_FROZEN,
                    FROZEN_CHEST_ID.toString(),
                    27,
                    state.getBlock().getName(),
                    null
            );
            savedData.putRecord(pos, record);
        }

        playUnlockEffects(level, pos);

        if (ModConfig.COMMON.aquamiraePerPlayerKeyUnlock.get()) {
            // 独立钥匙模式：保持冰冻方块外观，记录该玩家已解锁并立刻打开其独立 Lootr 战利品
            record.markUnlocked(player.getUUID());
            savedData.setDirty();
            ModNetworking.syncNewRecordToTracking(level, pos, record);
            player.displayClientMessage(
                    Component.translatable("lootr_more_compat.aquamirae.unlocked_personal").withStyle(ChatFormatting.AQUA),
                    true
            );
            LmcLootrBridge.openCompatContainer(level, pos, player, record);
            return true;
        }

        // 共享解锁模式：还原 Aquamirae 冰层融化为木箱的视觉效果，但保持箱子无共享战利品表、交由 Lootr 独立分发
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 3);
        ModNetworking.syncNewRecordToTracking(level, pos, record);
        return true;
    }

    private static void playUnlockEffects(ServerLevel level, BlockPos pos) {
        level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS,
                0.9F,
                1.1F
        );
        level.sendParticles(
                ParticleTypes.CLOUD,
                pos.getX() + 0.5,
                pos.getY() + 0.6,
                pos.getZ() + 0.5,
                12,
                0.35,
                0.35,
                0.35,
                0.02
        );
        level.sendParticles(
                ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.5,
                pos.getY() + 0.7,
                pos.getZ() + 0.5,
                14,
                0.4,
                0.4,
                0.4,
                0.04
        );
    }
}
