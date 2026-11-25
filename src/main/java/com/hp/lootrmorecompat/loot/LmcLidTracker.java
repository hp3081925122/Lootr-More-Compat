package com.hp.lootrmorecompat.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一管理原地兼容容器的箱盖动画与开关音效计数（1.20.1 Forge）。
 */
public final class LmcLidTracker {
    private static final Map<Key, Integer> OPEN_COUNTS = new HashMap<>();

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private LmcLidTracker() {
    }

    public static void onOpen(ServerLevel level, BlockPos pos) {
        Key key = new Key(level.dimension(), pos.immutable());
        int prev = OPEN_COUNTS.getOrDefault(key, 0);
        int next = prev + 1;
        OPEN_COUNTS.put(key, next);

        BlockState state = level.getBlockState(pos);
        if (prev == 0) {
            playSound(level, pos, state, true);
            updateBarrelOpenState(level, pos, state, true);
        }
        level.blockEvent(pos, state.getBlock(), 1, next);
    }

    public static void onClose(ServerLevel level, BlockPos pos) {
        Key key = new Key(level.dimension(), pos.immutable());
        int prev = OPEN_COUNTS.getOrDefault(key, 0);
        int next = Math.max(0, prev - 1);
        if (next == 0) {
            OPEN_COUNTS.remove(key);
        } else {
            OPEN_COUNTS.put(key, next);
        }

        BlockState state = level.getBlockState(pos);
        if (prev > 0 && next == 0) {
            playSound(level, pos, state, false);
            updateBarrelOpenState(level, pos, state, false);
        }
        level.blockEvent(pos, state.getBlock(), 1, next);
    }

    public static void clearPosition(ServerLevel level, BlockPos pos) {
        OPEN_COUNTS.remove(new Key(level.dimension(), pos.immutable()));
    }

    private static void updateBarrelOpenState(ServerLevel level, BlockPos pos, BlockState state, boolean open) {
        if (state.hasProperty(BarrelBlock.OPEN) && state.getValue(BarrelBlock.OPEN) != open) {
            level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
        }
    }

    private static void playSound(ServerLevel level, BlockPos pos, BlockState state, boolean open) {
        boolean isBarrel = state.getBlock() instanceof BarrelBlock || state.hasProperty(BarrelBlock.OPEN);
        SoundEvent sound;
        if (isBarrel) {
            sound = open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE;
        } else {
            sound = open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE;
        }
        level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                sound,
                SoundSource.BLOCKS,
                0.5F,
                level.random.nextFloat() * 0.1F + 0.9F
        );
    }
}
