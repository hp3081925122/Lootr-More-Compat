package com.hp.lootrmorecompat.compat;

import com.hp.lootrmorecompat.LootrMoreCompat;
import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Nameable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootTable;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 汇流来世（Confluence: Otherworld）1.21.1 NeoForge 专精兼容处理器。
 * 零强依赖覆盖全部 23 种汇流宝箱（普通钥匙箱、9 大地牢群系箱、死人宝箱陷阱、主题箱）、树洞与地牢遗落纸张。
 */
public final class ConfluenceCompatHandler {
    public static final String MOD_ID = "confluence";

    private static final ResourceLocation DEATH_GOLDEN_CHEST_ID = id("death_golden_chest");
    private static final ResourceLocation DEATH_WOODEN_CHEST_ID = id("death_wooden_chest");
    private static final ResourceLocation TREE_HOLES_ID = id("tree_holes");
    private static final ResourceLocation LOST_PAPER_ID = id("lost_paper");
    private static final ResourceLocation ADV_BIG_BOOTY = id("achievements/big_booty");

    private static final Set<ResourceLocation> MECHANICAL_TOOLS = Set.of(
            id("red_wrench"),
            id("green_wrench"),
            id("blue_wrench"),
            id("yellow_wrench"),
            id("wire_cutter")
    );

    private static final Map<ResourceLocation, KeyRule> KEY_RULES = Map.ofEntries(
            Map.entry(id("golden_chest"), new KeyRule(id("golden_key"), true, false)),
            Map.entry(id("dungeon_chest"), new KeyRule(id("golden_dungeon_key"), true, false)),
            Map.entry(id("shadow_chest"), new KeyRule(id("shadow_key"), false, false)),
            Map.entry(id("jungle_chest"), new KeyRule(id("jungle_key"), true, true)),
            Map.entry(id("corruption_chest"), new KeyRule(id("corruption_key"), true, true)),
            Map.entry(id("crimson_chest"), new KeyRule(id("crimson_key"), true, true)),
            Map.entry(id("hallowed_chest"), new KeyRule(id("hallowed_key"), true, true)),
            Map.entry(id("ice_chest"), new KeyRule(id("frozen_key"), true, true)),
            Map.entry(id("desert_chest"), new KeyRule(id("desert_key"), true, true)),
            Map.entry(id("ocean_chest"), new KeyRule(id("ocean_key"), true, true)),
            Map.entry(id("universe_chest"), new KeyRule(id("universe_key"), true, true)),
            Map.entry(id("mechanic_safe_chest"), new KeyRule(id("mechanic_safe_key"), true, true))
    );

    private record KeyRule(ResourceLocation keyItemId, boolean consumeKey, boolean isBiomeChest) {
    }

    private ConfluenceCompatHandler() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isConfluenceTarget(BlockState state, @Nullable BlockEntity blockEntity) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null || !MOD_ID.equals(blockId.getNamespace())) {
            return false;
        }
        if (LOST_PAPER_ID.equals(blockId) || TREE_HOLES_ID.equals(blockId)) {
            return true;
        }
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof RandomizableContainer
                || KEY_RULES.containsKey(blockId)
                || isDeathChest(blockId);
    }

    public static boolean isLostPaper(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return LOST_PAPER_ID.equals(blockId);
    }

    private static boolean isDeathChest(ResourceLocation blockId) {
        return DEATH_GOLDEN_CHEST_ID.equals(blockId) || DEATH_WOODEN_CHEST_ID.equals(blockId);
    }

    /**
     * 处理汇流来世宝箱、树洞与地牢遗落纸张的右键交互。
     *
     * @return true 表示已由本模组接管并取消原版流程；false 表示放行给汇流原版处理。
     */
    public static boolean handleRightClick(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            InteractionHand hand,
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null || !MOD_ID.equals(blockId.getNamespace())) {
            return false;
        }

        if (LOST_PAPER_ID.equals(blockId)) {
            return handleLostPaper(level, pos, player, state, blockEntity);
        }

        if (!ModConfig.COMMON.enableConfluenceChests.get()) {
            return false;
        }
        if (blockEntity == null || blockEntity instanceof ILootrBlockEntity || LootrAPI.resolveBlockEntity(blockEntity) != null) {
            return false;
        }

        boolean deathChest = isDeathChest(blockId);
        // 死人宝箱手持汇流扳手或钢丝钳时放行，保留机械电路查线与拆线交互
        if (deathChest && isHoldingMechanicalTool(player)) {
            return false;
        }

        BooleanProperty unlockedProp = findBooleanProperty(state, "unlocked");
        boolean locked = unlockedProp != null && !state.getValue(unlockedProp);

        LmcSavedData savedData = LmcSavedData.get(level);
        LmcContainerRecord record = savedData.getRecord(pos);
        boolean firstClaim = false;

        // 若箱子仍带有原始战利品表且尚未登记，则接管其战利品表并清空共享表
        if (record == null && blockEntity instanceof RandomizableContainer rc) {
            ResourceKey<LootTable> lootTableKey = rc.getLootTable();
            if (lootTableKey != null && !LootrAPI.isLootTableBlacklisted(lootTableKey)) {
                Component displayName = blockEntity instanceof Nameable nameable
                        ? nameable.getDisplayName()
                        : state.getBlock().getName();
                String compatType = deathChest
                        ? LmcContainerRecord.TYPE_CONFLUENCE_DEATH_CHEST
                        : LmcContainerRecord.TYPE_CONFLUENCE_CHEST;
                record = new LmcContainerRecord(
                        UUID.randomUUID(),
                        lootTableKey.location(),
                        rc.getLootTableSeed(),
                        compatType,
                        blockId.toString(),
                        rc.getContainerSize(),
                        displayName,
                        null
                );
                savedData.putRecord(pos, record);
                rc.setLootTable(null);
                blockEntity.setChanged();
                ModNetworking.syncNewRecordToTracking(level, pos, record);
                firstClaim = true;
            }
        }

        if (record == null) {
            // 玩家自建箱子、已开箱或无战利品表的装饰箱完全放行（不影响光明/夜光钥匙召唤宝箱怪）
            return false;
        }

        // 上锁宝箱：强制按钥匙规则校验，严禁绕过钥匙锁
        if (locked) {
            return handleLockedChest(level, pos, player, hand, state, unlockedProp, blockId, savedData, record);
        }

        // 未上锁死人宝箱首次被开启时，脉冲触发一次底层机械电路陷阱（飞镖/巨石/TNT）
        if (firstClaim && deathChest
                && ModConfig.COMMON.preserveConfluenceDeathChestTrap.get()
                && blockEntity instanceof Container container) {
            container.startOpen(player);
            container.stopOpen(player);
        }

        // 补发汇流原版箱子交互成就（地下金箱 i_am_loot、死人金箱 dead_men_tell_no_tales）
        ItemStack held = player.getItemInHand(hand);
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, held);
        if (DEATH_GOLDEN_CHEST_ID.equals(blockId)) {
            CriteriaTriggers.DEFAULT_BLOCK_USE.trigger(player, pos);
        }

        return LmcLootrBridge.openCompatContainer(level, pos, player, record);
    }

    private static boolean handleLockedChest(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            InteractionHand hand,
            BlockState state,
            BooleanProperty unlockedProp,
            ResourceLocation blockId,
            LmcSavedData savedData,
            LmcContainerRecord record
    ) {
        KeyRule rule = KEY_RULES.get(blockId);
        if (rule == null) {
            return false;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean perPlayerKey = ModConfig.COMMON.confluencePerPlayerKeyUnlock.get();

        // 每人独立钥匙模式且当前玩家此前已解锁过，直接打开其个人独立战利品
        if (perPlayerKey && record.hasUnlocked(player.getUUID())) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, held);
            return LmcLootrBridge.openCompatContainer(level, pos, player, record);
        }

        ResourceLocation heldId = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (held.isEmpty() || !rule.keyItemId.equals(heldId)) {
            // 未持对应钥匙：放行给汇流原版保持锁定失败反馈
            return false;
        }

        // 先以未消耗前的钥匙副本触发原版物品交互成就（保证 dungeon_heist / i_am_loot 正常判定）
        ItemStack triggerSnapshot = held.copy();
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, triggerSnapshot);

        // 暗影钥匙按泰拉瑞亚原版机制永不消耗，其余钥匙在非创造模式下消耗 1 把
        if (rule.consumeKey && !player.getAbilities().instabuild) {
            held.shrink(1);
        }

        playUnlockEffects(level, pos, state);
        if (rule.isBiomeChest) {
            awardAdvancement(player, ADV_BIG_BOOTY, "never");
        }

        if (perPlayerKey) {
            // 独立钥匙模式：方块保持上锁外观以提示队友，记录本玩家已解锁并立刻打开独立战利品
            record.markUnlocked(player.getUUID());
            savedData.setDirty();
            player.displayClientMessage(
                    Component.translatable("lootr_more_compat.confluence.unlocked_personal").withStyle(ChatFormatting.GOLD),
                    true
            );
            LmcLootrBridge.openCompatContainer(level, pos, player, record);
            return true;
        }

        // 共享解锁模式：还原汇流原版碎链解锁为无锁外观，玩家再次右键即可各自领取独立战利品
        level.setBlock(pos, state.setValue(unlockedProp, true), Block.UPDATE_ALL);
        return true;
    }

    private static boolean handleLostPaper(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {
        if (!ModConfig.COMMON.enableConfluenceLostPaper.get()) {
            return false;
        }

        LmcSavedData savedData = LmcSavedData.get(level);
        LmcContainerRecord record = savedData.getRecord(pos);
        if (record != null) {
            return LmcLootrBridge.openCompatContainer(level, pos, player, record);
        }

        if (blockEntity == null) {
            return false;
        }

        // 仅接管带有 LootTable 的自然生成地牢遗落纸张，忽略玩家自摆纸张以防刷神秘笔记
        CompoundTag savedTag = blockEntity.saveWithoutMetadata(level.registryAccess());
        if (!savedTag.contains("LootTable", Tag.TAG_STRING)) {
            return false;
        }
        ResourceLocation lootTableId = ResourceLocation.tryParse(savedTag.getString("LootTable"));
        if (lootTableId == null) {
            return false;
        }

        int layer = readIntProperty(state, "layer", 0);
        int rolls = Math.max(1, Math.min(4, layer + 1));

        record = new LmcContainerRecord(
                UUID.randomUUID(),
                lootTableId,
                level.getRandom().nextLong(),
                LmcContainerRecord.TYPE_CONFLUENCE_LOST_PAPER,
                String.valueOf(rolls),
                9,
                state.getBlock().getName(),
                null
        );
        savedData.putRecord(pos, record);
        clearLostPaperLootTable(blockEntity);
        ModNetworking.syncNewRecordToTracking(level, pos, record);
        return LmcLootrBridge.openCompatContainer(level, pos, player, record);
    }

    private static void clearLostPaperLootTable(BlockEntity blockEntity) {
        try {
            Method setter = blockEntity.getClass().getMethod("setLootTable", ResourceLocation.class);
            setter.invoke(blockEntity, new Object[]{null});
        } catch (ReflectiveOperationException e) {
            LootrMoreCompat.LOGGER.debug("Failed to clear Confluence LostPaper loot table via reflection: {}", e.getMessage());
        }
    }

    private static void playUnlockEffects(ServerLevel level, BlockPos pos, BlockState state) {
        level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS);
        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction relativeDir = ChestBlock.getConnectedDirection(state);
            posX += relativeDir.getStepX() * 0.5;
            posZ += relativeDir.getStepZ() * 0.5;
        }
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHAIN.defaultBlockState()),
                posX, pos.getY() + 0.5, posZ,
                200, 0.0625, 0.0625, 0.0625, 0.15
        );
    }

    private static void awardAdvancement(ServerPlayer player, ResourceLocation advancementId, String criterion) {
        AdvancementHolder holder = player.server.getAdvancements().get(advancementId);
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private static boolean isHoldingMechanicalTool(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem());
        return itemId != null && MECHANICAL_TOOLS.contains(itemId);
    }

    @Nullable
    public static BooleanProperty findBooleanProperty(BlockState state, String name) {
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof BooleanProperty boolProp && prop.getName().equals(name)) {
                return boolProp;
            }
        }
        return null;
    }

    private static int readIntProperty(BlockState state, String name, int defaultValue) {
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof IntegerProperty intProp && prop.getName().equals(name)) {
                return state.getValue(intProp);
            }
        }
        return defaultValue;
    }
}
