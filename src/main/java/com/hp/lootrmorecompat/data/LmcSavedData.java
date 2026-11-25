package com.hp.lootrmorecompat.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 按维度保存的兼容容器映射表（1.20.1 Forge），附带按区块分组索引以便高效同步至客户端。
 */
public final class LmcSavedData extends SavedData {
    private static final String DATA_NAME = "lootr_more_compat";

    private final Map<BlockPos, LmcContainerRecord> records = new HashMap<>();
    private final Map<ChunkPos, Set<BlockPos>> chunkIndex = new HashMap<>();

    public static LmcSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                LmcSavedData::load,
                LmcSavedData::new,
                DATA_NAME
        );
    }

    @Nullable
    public LmcContainerRecord getRecord(BlockPos pos) {
        return this.records.get(pos);
    }

    public boolean hasRecord(BlockPos pos) {
        return this.records.containsKey(pos);
    }

    public void putRecord(BlockPos pos, LmcContainerRecord record) {
        BlockPos immutable = pos.immutable();
        this.records.put(immutable, record);
        this.chunkIndex.computeIfAbsent(new ChunkPos(immutable), k -> new HashSet<>()).add(immutable);
        this.setDirty();
    }

    @Nullable
    public LmcContainerRecord removeRecord(BlockPos pos) {
        LmcContainerRecord removed = this.records.remove(pos);
        if (removed != null) {
            ChunkPos chunkPos = new ChunkPos(pos);
            Set<BlockPos> set = this.chunkIndex.get(chunkPos);
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) {
                    this.chunkIndex.remove(chunkPos);
                }
            }
            this.setDirty();
        }
        return removed;
    }

    public Set<BlockPos> getPositionsInChunk(ChunkPos chunkPos) {
        Set<BlockPos> set = this.chunkIndex.get(chunkPos);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    public int getRecordCount() {
        return this.records.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, LmcContainerRecord> entry : this.records.entrySet()) {
            CompoundTag item = entry.getValue().save();
            item.putLong("Pos", entry.getKey().asLong());
            list.add(item);
        }
        tag.put("Records", list);
        return tag;
    }

    private static LmcSavedData load(CompoundTag tag) {
        LmcSavedData data = new LmcSavedData();
        if (tag.contains("Records", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag item = list.getCompound(i);
                BlockPos pos = BlockPos.of(item.getLong("Pos"));
                LmcContainerRecord record = LmcContainerRecord.load(item);
                data.records.put(pos, record);
                data.chunkIndex.computeIfAbsent(new ChunkPos(pos), k -> new HashSet<>()).add(pos);
            }
        }
        return data;
    }
}
