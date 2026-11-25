package com.hp.lootrmorecompat.compat;

import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.network.ModNetworking;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.ForgeRegistries;
import noobanidus.mods.lootr.config.ConfigManager;
import twilightforest.TFConfig;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightforest.loot.TFLootTables;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 暮色森林（The Twilight Forest）1.20.1 Boss 死亡战利品箱专精兼容处理。
 * 同时覆盖 IBossLootBuffer（娜迦/巫妖/暮初恶魂）与 TFLootTables.entityDropsIntoContainer（九头蛇/米诺菇/冰雪女王/雪怪首领）。
 */
public final class TwilightForestCompatHandler {
    private TwilightForestCompatHandler() {
    }

    public static <T extends LivingEntity & IBossLootBuffer> boolean depositLootrBossChest(
            T boss,
            BlockState chest,
            BlockPos pos,
            ServerLevel serverLevel
    ) {
        if (!ModConfig.COMMON.enableTwilightBossChest.get()
                || ConfigManager.DISABLE.get()
                || !TFConfig.COMMON_CONFIG.bossDropChests.get()) {
            return false;
        }

        boolean placed = serverLevel.setBlock(pos, chest, 2)
                || serverLevel.getBlockState(pos).is(chest.getBlock());
        if (!placed || !(serverLevel.getBlockEntity(pos) instanceof Container container)) {
            return false;
        }

        NonNullList<ItemStack> bossStacks = boss.getItemStacks();
        List<ItemStack> fallbackItems = new ArrayList<>(IBossLootBuffer.CONTAINER_SIZE);
        for (int i = 0; i < IBossLootBuffer.CONTAINER_SIZE && i < bossStacks.size(); i++) {
            ItemStack stack = bossStacks.get(i);
            fallbackItems.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            bossStacks.set(i, ItemStack.EMPTY);
        }

        return registerBossChest(serverLevel, pos, boss, container.getContainerSize(), fallbackItems);
    }

    public static boolean depositLootrEntityDrops(
            LivingEntity entity,
            DamageSource source,
            BlockState chest,
            BlockPos pos
    ) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!ModConfig.COMMON.enableTwilightBossChest.get()
                || ConfigManager.DISABLE.get()
                || !TFConfig.COMMON_CONFIG.bossDropChests.get()) {
            return false;
        }

        boolean placed = serverLevel.setBlock(pos, chest, 2)
                || serverLevel.getBlockState(pos).is(chest.getBlock());
        if (!placed || !(serverLevel.getBlockEntity(pos) instanceof Container container)) {
            return false;
        }

        // 先抽取一次击杀掉落物作为保底/共享模板
        List<ItemStack> fallbackItems = new ArrayList<>();
        ResourceLocation lootTableId = entity.getLootTable();
        if (lootTableId != null) {
            LootTable table = serverLevel.getServer().getLootData().getLootTable(lootTableId);
            LootParams params = TFLootTables.createLootParams(entity, true, source).create(LootContextParamSets.ENTITY);
            ObjectArrayList<ItemStack> rolled = table.getRandomItems(params);
            for (ItemStack stack : rolled) {
                if (!stack.isEmpty()) {
                    fallbackItems.add(stack.copy());
                }
            }
        }

        return registerBossChest(serverLevel, pos, entity, container.getContainerSize(), fallbackItems);
    }

    private static boolean registerBossChest(
            ServerLevel serverLevel,
            BlockPos pos,
            LivingEntity boss,
            int containerSize,
            List<ItemStack> fallbackItems
    ) {
        ResourceLocation lootTableId = boss.getLootTable();
        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
        Component title = Component.translatable("lootr_more_compat.container.twilight_boss", boss.getDisplayName());

        LmcContainerRecord record = new LmcContainerRecord(
                UUID.randomUUID(),
                lootTableId,
                serverLevel.getRandom().nextLong(),
                LmcContainerRecord.TYPE_TWILIGHT_BOSS,
                entityTypeId != null ? entityTypeId.toString() : "",
                Math.max(27, containerSize),
                title,
                fallbackItems
        );

        LmcSavedData.get(serverLevel).putRecord(pos, record);
        ModNetworking.syncNewRecordToTracking(serverLevel, pos, record);

        serverLevel.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 0.9,
                pos.getZ() + 0.5,
                24,
                0.45,
                0.35,
                0.45,
                0.05
        );
        return true;
    }
}
