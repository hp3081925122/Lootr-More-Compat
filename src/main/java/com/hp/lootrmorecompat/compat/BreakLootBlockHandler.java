package com.hp.lootrmorecompat.compat;

import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.loot.LmcLootrBridge;
import com.hp.lootrmorecompat.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 破坏掉落型战利品方块兼容（如深幽暗域远古花瓶 ancient_vase、Supplementaries 宝藏陶罐 urn 及自定义配置条目）。
 * 玩家右键即可打开按人独立的 9 格 Lootr 战利品界面，无需破坏方块，方便联机队友共享。
 */
public final class BreakLootBlockHandler {
    private BreakLootBlockHandler() {
    }

    public static boolean handleRightClick(ServerLevel level, BlockPos pos, ServerPlayer player, BlockState state) {
        if (!ModConfig.COMMON.enableBreakLootBlocks.get()) {
            return false;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            return false;
        }

        ResourceLocation matchedLootTable = findMatchingLootTable(blockId.toString(), state);
        if (matchedLootTable == null) {
            return false;
        }

        LmcSavedData savedData = LmcSavedData.get(level);
        LmcContainerRecord record = savedData.getRecord(pos);
        if (record == null) {
            record = new LmcContainerRecord(
                    UUID.randomUUID(),
                    matchedLootTable,
                    level.getRandom().nextLong(),
                    LmcContainerRecord.TYPE_BREAK_LOOT,
                    blockId.toString(),
                    9,
                    state.getBlock().getName(),
                    null
            );
            savedData.putRecord(pos, record);
            ModNetworking.syncNewRecordToTracking(level, pos, record);
        }
        return LmcLootrBridge.openCompatContainer(level, pos, player, record);
    }

    @Nullable
    private static ResourceLocation findMatchingLootTable(String blockIdStr, BlockState state) {
        for (String raw : ModConfig.COMMON.customBreakLootBlocks.get()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 3) {
                continue;
            }
            String entryBlockId = parts[0].trim();
            if (!entryBlockId.equals(blockIdStr)) {
                continue;
            }
            String propCond = parts[1].trim();
            if (!propCond.isEmpty() && !matchesPropertyCondition(state, propCond)) {
                continue;
            }
            ResourceLocation table = ResourceLocation.tryParse(parts[2].trim());
            if (table != null) {
                return table;
            }
        }
        return null;
    }

    private static boolean matchesPropertyCondition(BlockState state, String condition) {
        int eq = condition.indexOf('=');
        if (eq <= 0 || eq >= condition.length() - 1) {
            return false;
        }
        String propName = condition.substring(0, eq).trim();
        String expectedVal = condition.substring(eq + 1).trim();
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(propName)) {
                Object val = state.getValue(property);
                return val != null && val.toString().equalsIgnoreCase(expectedVal);
            }
        }
        return false;
    }
}
