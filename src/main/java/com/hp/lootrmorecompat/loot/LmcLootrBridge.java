package com.hp.lootrmorecompat.loot;

import com.hp.lootrmorecompat.LootrMoreCompat;
import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.network.ModNetworking;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import noobanidus.mods.lootr.common.api.BuiltInLootrTypes;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.DefaultLootFiller;
import noobanidus.mods.lootr.common.api.data.ILootrInfoProvider;
import noobanidus.mods.lootr.common.api.data.LootFiller;
import noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 统一的 Lootr 1.21.1 桥接器：负责创建独立战利品容器、按类型填充战利品表、打开菜单与同步已领取状态。
 */
public final class LmcLootrBridge {
    private static final ResourceKey<LootTable> FALLBACK_TABLE_KEY = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LootrMoreCompat.MOD_ID, "internal_fallback")
    );

    private LmcLootrBridge() {
    }

    public static boolean openCompatContainer(ServerLevel level, BlockPos pos, ServerPlayer player, LmcContainerRecord record) {
        if (LootrAPI.isFakePlayer(player) || LootrAPI.isDisabled()) {
            return false;
        }

        ResourceKey<LootTable> tableKey = record.getLootTable() != null
                ? ResourceKey.create(Registries.LOOT_TABLE, record.getLootTable())
                : FALLBACK_TABLE_KEY;

        Component title = record.getDisplayName() != null
                ? record.getDisplayName()
                : level.getBlockState(pos).getBlock().getName();

        ILootrInfoProvider provider = ILootrInfoProvider.of(
                record.getLootrId(),
                pos.immutable(),
                record.getContainerSize(),
                tableKey,
                record.getLootTableSeed(),
                title,
                level.dimension(),
                null,
                BuiltInLootrTypes.CHEST
        );

        LootFiller filler = buildFiller(level, pos, record);
        ILootrInventory lootrInventory = LootrAPI.getInventory(provider, player, filler);
        if (lootrInventory == null) {
            return false;
        }

        // 记录已领取玩家并立即同步给该客户端以关闭未开启粒子提示
        if (record.markOpened(player.getUUID())) {
            LmcSavedData.get(level).setDirty();
            ModNetworking.syncSingleToPlayer(player, pos, true);
        }

        Container wrapped = new LmcTrackingContainer(lootrInventory, level, pos.immutable());
        int rows = Math.max(1, Math.min(6, record.getContainerSize() / 9));
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player openingPlayer) {
                return createChestMenu(rows, syncId, playerInventory, wrapped);
            }
        };

        player.openMenu(menuProvider);
        return true;
    }

    private static AbstractContainerMenu createChestMenu(int rows, int syncId, Inventory playerInv, Container container) {
        return switch (rows) {
            case 1 -> new ChestMenu(MenuType.GENERIC_9x1, syncId, playerInv, container, 1);
            case 2 -> new ChestMenu(MenuType.GENERIC_9x2, syncId, playerInv, container, 2);
            case 4 -> new ChestMenu(MenuType.GENERIC_9x4, syncId, playerInv, container, 4);
            case 5 -> new ChestMenu(MenuType.GENERIC_9x5, syncId, playerInv, container, 5);
            case 6 -> ChestMenu.sixRows(syncId, playerInv, container);
            default -> ChestMenu.threeRows(syncId, playerInv, container);
        };
    }

    private static LootFiller buildFiller(ServerLevel level, BlockPos pos, LmcContainerRecord record) {
        return switch (record.getCompatType()) {
            case LmcContainerRecord.TYPE_TWILIGHT_BOSS -> (provider, player, container) ->
                    fillTwilightBossLoot(level, pos, player, container, record);
            case LmcContainerRecord.TYPE_BREAK_LOOT -> (provider, player, container) ->
                    fillBreakLootBlock(level, pos, player, container, record);
            default -> (provider, player, container) -> {
                if (record.getLootTable() != null) {
                    DefaultLootFiller.getInstance().unpackLootTable(provider, player, container);
                }
                if (isContainerEmpty(container) && record.hasFallbackItems()) {
                    fillFromFallback(container, record.getFallbackItems());
                }
            };
        };
    }

    private static void fillTwilightBossLoot(
            ServerLevel level,
            BlockPos pos,
            Player player,
            Container container,
            LmcContainerRecord record
    ) {
        boolean filled = false;
        if (ModConfig.COMMON.twilightBossRerollPerPlayer.get()
                && record.getLootTable() != null
                && !record.getExtraMeta().isEmpty()
                && player instanceof ServerPlayer serverPlayer) {
            ResourceLocation entityId = ResourceLocation.tryParse(record.getExtraMeta());
            Optional<EntityType<?>> typeOpt = entityId == null
                    ? Optional.empty()
                    : BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
            if (typeOpt.isPresent()) {
                Entity transientBoss = typeOpt.get().create(level);
                if (transientBoss != null) {
                    Vec3 center = Vec3.atCenterOf(pos);
                    transientBoss.setPos(center.x, center.y, center.z);
                    ResourceKey<LootTable> tableKey = ResourceKey.create(Registries.LOOT_TABLE, record.getLootTable());
                    LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(tableKey);
                    LootParams params = new LootParams.Builder(level)
                            .withParameter(LootContextParams.THIS_ENTITY, transientBoss)
                            .withParameter(LootContextParams.ORIGIN, center)
                            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(serverPlayer))
                            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, serverPlayer)
                            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, serverPlayer)
                            .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, serverPlayer)
                            .withLuck(serverPlayer.getLuck())
                            .create(LootContextParamSets.ENTITY);
                    ObjectArrayList<ItemStack> rolled = lootTable.getRandomItems(params);
                    if (!rolled.isEmpty()) {
                        scatterItems(container, rolled, level.getRandom());
                        filled = true;
                    }
                }
            }
        }
        if (!filled && record.hasFallbackItems()) {
            fillFromFallback(container, record.getFallbackItems());
        }
    }

    private static void fillBreakLootBlock(
            ServerLevel level,
            BlockPos pos,
            Player player,
            Container container,
            LmcContainerRecord record
    ) {
        if (record.getLootTable() != null) {
            ResourceKey<LootTable> tableKey = ResourceKey.create(Registries.LOOT_TABLE, record.getLootTable());
            LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(tableKey);
            BlockState state = level.getBlockState(pos);
            LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.BLOCK_STATE, state)
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, player);
            if (player instanceof ServerPlayer serverPlayer) {
                builder.withLuck(serverPlayer.getLuck());
            }
            ObjectArrayList<ItemStack> rolled = lootTable.getRandomItems(builder.create(LootContextParamSets.BLOCK));
            if (!rolled.isEmpty()) {
                scatterItems(container, rolled, level.getRandom());
                return;
            }
        }
        if (record.hasFallbackItems()) {
            fillFromFallback(container, record.getFallbackItems());
        }
    }

    private static void fillFromFallback(Container container, NonNullList<ItemStack> fallback) {
        int limit = Math.min(container.getContainerSize(), fallback.size());
        for (int i = 0; i < limit; i++) {
            ItemStack stack = fallback.get(i);
            if (!stack.isEmpty()) {
                container.setItem(i, stack.copy());
            }
        }
    }

    private static void scatterItems(Container container, List<ItemStack> items, RandomSource random) {
        List<Integer> slots = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) {
            slots.add(i);
        }
        Util.shuffle(slots, random);
        int idx = 0;
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (idx >= slots.size()) {
                break;
            }
            container.setItem(slots.get(idx++), stack.copy());
        }
    }

    private static boolean isContainerEmpty(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 包装 ILootrInventory，在打开与关闭菜单时同步触发底层方块的箱盖动画与开关音效。
     */
    private static final class LmcTrackingContainer implements Container {
        private final ILootrInventory delegate;
        private final ServerLevel level;
        private final BlockPos pos;

        private LmcTrackingContainer(ILootrInventory delegate, ServerLevel level, BlockPos pos) {
            this.delegate = delegate;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int getContainerSize() {
            return delegate.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return delegate.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return delegate.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return delegate.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            delegate.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            delegate.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return delegate.stillValid(player);
        }

        @Override
        public void clearContent() {
            delegate.clearContent();
        }

        @Override
        public void startOpen(Player player) {
            delegate.startOpen(player);
            LmcLidTracker.onOpen(level, pos);
        }

        @Override
        public void stopOpen(Player player) {
            delegate.stopOpen(player);
            LmcLidTracker.onClose(level, pos);
        }
    }
}
