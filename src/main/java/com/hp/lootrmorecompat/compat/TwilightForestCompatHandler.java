package com.hp.lootrmorecompat.compat;

import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootTable;
import noobanidus.mods.lootr.common.api.LootrAPI;
import twilightforest.config.TFConfig;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightforest.loot.TFLootTables;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 暮色森林（The Twilight Forest）1.21.1 Boss 死亡战利品箱专精兼容处理。
 * 保留暮色森林原有箱子外观、音效与庆祝粒子，但将箱内掉落切换为每位玩家独立的 Lootr 战利品。
 */
public final class TwilightForestCompatHandler {
    private TwilightForestCompatHandler() {
    }

    /**
     * 尝试放置并登记暮色森林 Boss 独立战利品箱（含向上寻位与兜底掉落逻辑）。
     *
     * @return true 表示已由本模组接管处理；false 表示功能未启用，应回退原版逻辑。
     */
    public static <T extends LivingEntity & IBossLootBuffer> boolean depositLootrBossChest(
            T boss,
            BlockState chest,
            BlockPos pos,
            ServerLevel serverLevel
    ) {
        if (!ModConfig.COMMON.enableTwilightBossChest.get() || LootrAPI.isDisabled() || !TFConfig.bossDropChests) {
            return false;
        }
        if (boss.getItemStacks().isEmpty()) {
            return false;
        }

        if (tryDepositAt(boss, chest, pos, serverLevel)) {
            return true;
        }

        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int y = pos.getY(); y < serverLevel.getMaxBuildHeight(); y++) {
            cursor.setY(y);
            if (tryDepositAt(boss, chest, cursor, serverLevel)) {
                return true;
            }
        }

        // 所有垂直位置均无法放置箱子时，按原版兜底将物品弹出到地上
        for (int i = 0; i < IBossLootBuffer.CONTAINER_SIZE; i++) {
            Block.popResource(serverLevel, pos, boss.getItem(i));
            boss.setItem(i, ItemStack.EMPTY);
        }
        IBossLootBuffer.celebrateAt(boss, pos.getCenter(), serverLevel);
        return true;
    }

    private static <T extends LivingEntity & IBossLootBuffer> boolean tryDepositAt(
            T boss,
            BlockState chest,
            BlockPos pos,
            ServerLevel serverLevel
    ) {
        BlockState existing = serverLevel.getBlockState(pos);
        boolean canPlace = existing.is(chest.getBlock())
                || ((existing.canBeReplaced() || existing.getPistonPushReaction() != PushReaction.BLOCK)
                && serverLevel.getBlockEntity(pos) == null
                && serverLevel.setBlock(pos, chest, TFLootTables.DEFAULT_PLACE_FLAG));

        if (!canPlace || !(serverLevel.getBlockEntity(pos) instanceof Container container)) {
            return false;
        }

        NonNullList<ItemStack> bossStacks = boss.getItemStacks();
        List<ItemStack> fallbackItems = new ArrayList<>(IBossLootBuffer.CONTAINER_SIZE);
        for (int i = 0; i < IBossLootBuffer.CONTAINER_SIZE && i < bossStacks.size(); i++) {
            ItemStack stack = bossStacks.get(i);
            fallbackItems.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            bossStacks.set(i, ItemStack.EMPTY);
        }

        ResourceKey<LootTable> lootKey = boss.getLootTable();
        ResourceLocation lootTableId = lootKey != null ? lootKey.location() : null;
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        Component title = Component.translatable("lootr_more_compat.container.twilight_boss", boss.getDisplayName());

        LmcContainerRecord record = new LmcContainerRecord(
                UUID.randomUUID(),
                lootTableId,
                serverLevel.getRandom().nextLong(),
                LmcContainerRecord.TYPE_TWILIGHT_BOSS,
                entityTypeId != null ? entityTypeId.toString() : "",
                Math.max(27, container.getContainerSize()),
                title,
                fallbackItems
        );

        LmcSavedData.get(serverLevel).putRecord(pos, record);
        ModNetworking.syncNewRecordToTracking(serverLevel, pos, record);
        IBossLootBuffer.celebrateAt(boss, pos.getCenter(), serverLevel);
        return true;
    }
}
