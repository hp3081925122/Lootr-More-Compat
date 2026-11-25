package com.hp.lootrmorecompat.network;

import com.hp.lootrmorecompat.LootrMoreCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端向客户端同步兼容容器状态的网络包，用于驱动客户端未开启粒子提示。
 */
public record SyncCompatContainersPayload(List<Entry> entries, List<BlockPos> removed) implements CustomPacketPayload {
    public static final Type<SyncCompatContainersPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LootrMoreCompat.MOD_ID, "sync_compat_containers")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncCompatContainersPayload> STREAM_CODEC =
            StreamCodec.of(SyncCompatContainersPayload::write, SyncCompatContainersPayload::read);

    public record Entry(BlockPos pos, boolean opened) {
    }

    private static void write(FriendlyByteBuf buf, SyncCompatContainersPayload payload) {
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

    private static SyncCompatContainersPayload read(FriendlyByteBuf buf) {
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
