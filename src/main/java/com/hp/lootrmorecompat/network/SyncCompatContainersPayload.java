package com.hp.lootrmorecompat.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端向客户端同步兼容容器状态的网络包（1.20.1 Forge）。
 */
public record SyncCompatContainersPayload(List<Entry> entries, List<BlockPos> removed) {
    public record Entry(BlockPos pos, boolean opened) {
    }

    public static void encode(SyncCompatContainersPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buf.writeBlockPos(entry.pos);
            buf.writeBoolean(entry.opened);
        }
        buf.writeVarInt(payload.removed.size());
        for (BlockPos pos : payload.removed) {
            buf.writeBlockPos(pos);
        }
    }

    public static SyncCompatContainersPayload decode(FriendlyByteBuf buf) {
        int entryCount = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            entries.add(new Entry(buf.readBlockPos(), buf.readBoolean()));
        }
        int removedCount = buf.readVarInt();
        List<BlockPos> removed = new ArrayList<>(removedCount);
        for (int i = 0; i < removedCount; i++) {
            removed.add(buf.readBlockPos());
        }
        return new SyncCompatContainersPayload(entries, removed);
    }
}
