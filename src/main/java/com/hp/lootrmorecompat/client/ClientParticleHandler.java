package com.hp.lootrmorecompat.client;

import com.hp.lootrmorecompat.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Map;

/**
 * 客户端定时在尚未被当前玩家领取的兼容容器上方生成星光粒子提示（1.20.1 Forge）。
 */
public final class ClientParticleHandler {
    private static int tickCounter = 0;

    private ClientParticleHandler() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ModConfig.CLIENT.enableUnopenedParticles.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null || mc.isPaused()) {
            return;
        }
        int interval = ModConfig.CLIENT.particleIntervalTicks.get();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        BlockPos playerPos = player.blockPosition();
        for (Map.Entry<BlockPos, Boolean> entry : ClientCompatCache.getAll().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            BlockPos pos = entry.getKey();
            if (pos.distSqr(playerPos) > 24 * 24 || !level.isLoaded(pos)) {
                continue;
            }
            double x = pos.getX() + 0.25 + level.random.nextDouble() * 0.5;
            double y = pos.getY() + 0.95 + level.random.nextDouble() * 0.2;
            double z = pos.getZ() + 0.25 + level.random.nextDouble() * 0.5;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.025, 0.0);
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCompatCache.clear();
        tickCounter = 0;
    }

    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        ClientCompatCache.clear();
    }
}
