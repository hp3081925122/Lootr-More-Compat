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
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import noobanidus.mods.lootr.api.LootFiller;
import noobanidus.mods.lootr.api.LootrAPI;
import noobanidus.mods.lootr.api.inventory.ILootrInventory;
import noobanidus.mods.lootr.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 统一的 Lootr 1.20.1 桥接器：负责创建独立战利品容器、按类型填充战利品表、打开菜单与同步已领取状态。
 */
public final class LmcLootrBridge {
    private static final ResourceLocation FALLBACK_TABLE_ID =
            new ResourceLocation(LootrMoreCompat.MOD_ID, "internal_fallback");
    private static final ResourceLocation CONFLUENCE_DEAD_MANS_SEATER =
            new ResourceLocation("confluence", "dead_mans_seater");
    private static final ResourceLocation CONFLUENCE_DEAD_MANS_SWEATER =
            new ResourceLocation("confluence", "dead_mans_sweater");

    private LmcLootrBridge() {
    }

    public static boolean openCompatContainer(ServerLevel level, BlockPos pos, ServerPlayer player, LmcContainerRecord record) {
        if (LootrAPI.isFakePlayer(player) || ConfigManager.DISABLE.get()) {
            return false;
        }

        ResourceLocation tableId = record.getLootTable() != null ? record.getLootTable() : FALLBACK_TABLE_ID;
        Component title = record.getDisplayName() != null
                ? record.getDisplayName()
                : level.getBlockState(pos).getBlock().getName();

        LootFiller filler = buildFiller(level, pos, record);
        ILootrInventory lootrInventory = LootrAPI.getInventory(
                level,
                record.getLootrId(),
                pos.immutable(),
                player,
                record::getContainerSize,
                () -> title,
                filler,
                () -> tableId,
                record::getLootTableSeed
        );
        if (lootrInventory == null) {
            return false;
        }

        // 记录已领取玩家并立即同步给该客户端以关闭未开启粒子提示
        if (record.markOpened(player.getUUID())) {
            LmcSavedData.get(level).setDirty();
            ModNetworking.syncSingleToPlayer(player, pos, true);
        }

        Container wrapped = new LmcTrackingContainer(lootrInventory, level, pos.immutable(), record.getLootrId());
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
            case LmcContainerRecord.TYPE_TWILIGHT_BOSS -> (player, container, table, seed) ->
                    fillTwilightBossLoot(level, pos, player, container, record);
            case LmcContainerRecord.TYPE_BREAK_LOOT -> (player, container, table, seed) ->
                    fillBreakLootBlock(level, pos, player, container, record);
            case LmcContainerRecord.TYPE_CONFLUENCE_DEATH_CHEST -> (player, container, table, seed) ->
                    fillConfluenceDeathChest(level, pos, player, container, record, seed);
            case LmcContainerRecord.TYPE_CONFLUENCE_LOST_PAPER -> (player, container, table, seed) ->
                    fillConfluenceLostPaper(level, pos, player, container, record);
            default -> (player, container, table, seed) ->
                    fillStandardChestLoot(level, pos, player, container, record, seed);
        };
    }

    private static void fillConfluenceDeathChest(
            ServerLevel level,
            BlockPos pos,
            Player player,
            Container container,
            LmcContainerRecord record,
            long seed
    ) {
        // 与汇流原版 DeathChestBlock.BEntity.unpackLootTable 保持一致：先放必掉死人毛衣，再解包战利品表
        Item sweaterItem = ForgeRegistries.ITEMS.getValue(CONFLUENCE_DEAD_MANS_SEATER);
        if (sweaterItem == null || sweaterItem == Items.AIR) {
            sweaterItem = ForgeRegistries.ITEMS.getValue(CONFLUENCE_DEAD_MANS_SWEATER);
        }
        if (sweaterItem != null && sweaterItem != Items.AIR) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, sweaterItem.getDefaultInstance());
                    break;
                }
            }
        }
        fillStandardChestLoot(level, pos, player, container, record, seed);
    }

    private static void fillConfluenceLostPaper(
            ServerLevel level,
            BlockPos pos,
            Player player,
            Container container,
            LmcContainerRecord record
    ) {
        if (record.getLootTable() != null) {
            LootTable lootTable = level.getServer().getLootData().getLootTable(record.getLootTable());
            if (lootTable != LootTable.EMPTY) {
                int rolls = parsePositiveInt(record.getExtraMeta(), 1, 4);
                LootParams.Builder builder = new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, player);
                if (player instanceof ServerPlayer serverPlayer) {
                    builder.withLuck(serverPlayer.getLuck());
                }
                LootParams params = builder.create(LootContextParamSets.GIFT);
                List<ItemStack> rolled = new ArrayList<>();
                for (int i = 0; i < rolls; i++) {
                    rolled.addAll(lootTable.getRandomItems(params));
                }
                if (!rolled.isEmpty()) {
                    scatterItems(container, rolled, level.getRandom());
                    return;
                }
            }
        }
        if (record.hasFallbackItems()) {
            fillFromFallback(container, record.getFallbackItems());
        }
    }

    private static int parsePositiveInt(String raw, int fallback, int max) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1, Math.min(max, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void fillStandardChestLoot(
            ServerLevel level,
            BlockPos pos,
            Player player,
            Container container,
            LmcContainerRecord record,
            long seed
    ) {
        if (record.getLootTable() != null) {
            LootTable lootTable = level.getServer().getLootData().getLootTable(record.getLootTable());
            LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos));
            if (player != null) {
                builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
            }
            long actualSeed = ConfigManager.RANDOMISE_SEED.get() ? level.getRandom().nextLong() : seed;
            lootTable.fill(container, builder.create(LootContextParamSets.CHEST), actualSeed);
        }
        if (isContainerEmpty(container) && record.hasFallbackItems()) {
            fillFromFallback(container, record.getFallbackItems());
        }
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
            EntityType<?> entityType = entityId == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(entityId);
            if (entityType != null) {
                Entity transientBoss = entityType.create(level);
                if (transientBoss != null) {
                    Vec3 center = Vec3.atCenterOf(pos);
                    transientBoss.setPos(center.x, center.y, center.z);
                    LootTable lootTable = level.getServer().getLootData().getLootTable(record.getLootTable());
                    LootParams params = new LootParams.Builder(level)
                            .withParameter(LootContextParams.THIS_ENTITY, transientBoss)
                            .withParameter(LootContextParams.ORIGIN, center)
                            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(serverPlayer))
                            .withOptionalParameter(LootContextParams.KILLER_ENTITY, serverPlayer)
                            .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, serverPlayer)
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
            LootTable lootTable = level.getServer().getLootData().getLootTable(record.getLootTable());
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
        ObjectArrayList<Integer> slots = new ObjectArrayList<>(container.getContainerSize());
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
        private final UUID lootrId;

        private LmcTrackingContainer(ILootrInventory delegate, ServerLevel level, BlockPos pos, UUID lootrId) {
            this.delegate = delegate;
            this.level = level;
            this.pos = pos;
            this.lootrId = lootrId;
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
            if (player.isRemoved() || !player.level().dimension().equals(level.dimension())) {
                return false;
            }
            if (level.getBlockState(pos).isAir()) {
                return false;
            }
            LmcContainerRecord current = LmcSavedData.get(level).getRecord(pos);
            if (current == null || !current.getLootrId().equals(lootrId)) {
                return false;
            }
            // 对无 BlockEntity 的破坏型战利品方块跳过 Lootr 的 BlockEntity 非空强制校验，改为距离校验
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                return Container.stillValidBlockEntity(be, player);
            }
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void clearContent() {
            delegate.clearContent();
        }

        @Override
        public void startOpen(Player player) {
            // 不转发给 delegate.startOpen，避免 Lootr 绕过本模组去触发底层方块的 ContainerOpenersCounter（如死人箱陷阱与双份开关音效）
            LmcLidTracker.onOpen(level, pos);
        }

        @Override
        public void stopOpen(Player player) {
            delegate.setChanged();
            LmcLidTracker.onClose(level, pos);
        }
    }
}
