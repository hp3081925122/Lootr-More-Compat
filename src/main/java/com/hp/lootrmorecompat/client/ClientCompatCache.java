package com.hp.lootrmorecompat.client;

import com.hp.lootrmorecompat.network.SyncCompatContainersPayload;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端兼容容器状态缓存（1.20.1 Forge）。
 */
public final class ClientCompatCache {
    private static final Map<BlockPos, Boolean> OPENED_BY_LOCAL_PLAYER = new ConcurrentHashMap<>();

    private ClientCompatCache() {
    }

    public static void applySync(SyncCompatContainersPayload payload) {
        for (SyncCompatContainersPayload.Entry entry : payload.entries()) {
            OPENED_BY_LOCAL_PLAYER.put(entry.pos().immutable(), entry.opened());
        }
        for (BlockPos removed : payload.removed()) {
            OPENED_BY_LOCAL_PLAYER.remove(removed);
        }
    }

    public static Map<BlockPos, Boolean> getAll() {
        return OPENED_BY_LOCAL_PLAYER;
    }

    public static void clear() {
        OPENED_BY_LOCAL_PLAYER.clear();
    }
}
