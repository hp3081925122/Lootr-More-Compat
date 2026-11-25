package com.hp.lootrmorecompat.network;

import com.hp.lootrmorecompat.client.ClientCompatCache;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 模组网络注册与同步工具类。
 */
public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                SyncCompatContainersPayload.TYPE,
                SyncCompatContainersPayload.STREAM_CODEC,
                ModNetworking::handleSyncOnClient
        );
    }

    private static void handleSyncOnClient(SyncCompatContainersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCompatCache.applySync(payload));
    }

    public static void syncSingleToPlayer(ServerPlayer player, BlockPos pos, boolean opened) {
        PacketDistributor.sendToPlayer(
                player,
                new SyncCompatContainersPayload(
                        List.of(new SyncCompatContainersPayload.Entry(pos.immutable(), opened)),
                        List.of()
                )
        );
    }

    public static void syncNewRecordToTracking(ServerLevel level, BlockPos pos, LmcContainerRecord record) {
        ChunkPos chunkPos = new ChunkPos(pos);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
            syncSingleToPlayer(player, pos, record.hasOpened(player.getUUID()));
        }
    }

    public static void syncRemovedToTracking(ServerLevel level, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                new ChunkPos(pos),
                new SyncCompatContainersPayload(List.of(), List.of(pos.immutable()))
        );
    }

    public static void syncChunkToPlayer(ServerLevel level, ServerPlayer player, ChunkPos chunkPos) {
        LmcSavedData data = LmcSavedData.get(level);
        Set<BlockPos> positions = data.getPositionsInChunk(chunkPos);
        if (positions.isEmpty()) {
            return;
        }
        List<SyncCompatContainersPayload.Entry> entries = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            LmcContainerRecord record = data.getRecord(pos);
            if (record != null) {
                entries.add(new SyncCompatContainersPayload.Entry(pos, record.hasOpened(player.getUUID())));
            }
        }
        if (!entries.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new SyncCompatContainersPayload(entries, List.of()));
        }
    }
}
