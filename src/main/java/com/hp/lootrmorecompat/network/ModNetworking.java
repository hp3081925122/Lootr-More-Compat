package com.hp.lootrmorecompat.network;

import com.hp.lootrmorecompat.LootrMoreCompat;
import com.hp.lootrmorecompat.client.ClientCompatCache;
import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 模组网络注册与同步工具类（1.20.1 Forge SimpleChannel）。
 */
public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LootrMoreCompat.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetworking() {
    }

    public static void register() {
        CHANNEL.messageBuilder(SyncCompatContainersPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCompatContainersPayload::encode)
                .decoder(SyncCompatContainersPayload::decode)
                .consumerMainThread(ModNetworking::handleSyncOnClient)
                .add();
    }

    private static void handleSyncOnClient(SyncCompatContainersPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        ClientCompatCache.applySync(payload);
        ctxSupplier.get().setPacketHandled(true);
    }

    public static void syncSingleToPlayer(ServerPlayer player, BlockPos pos, boolean opened) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
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
        ChunkPos chunkPos = new ChunkPos(pos);
        SyncCompatContainersPayload payload = new SyncCompatContainersPayload(List.of(), List.of(pos.immutable()));
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
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
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncCompatContainersPayload(entries, List.of())
            );
        }
    }
}
