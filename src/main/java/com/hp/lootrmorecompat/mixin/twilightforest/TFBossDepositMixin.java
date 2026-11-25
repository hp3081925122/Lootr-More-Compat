package com.hp.lootrmorecompat.mixin.twilightforest;

import com.hp.lootrmorecompat.compat.TwilightForestCompatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.BaseTFBoss;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightforest.entity.boss.KnightPhantom;

/**
 * 重定向 1.21.1 暮色森林全部 Boss（BaseTFBoss 子类与幻影骑士）的死亡宝箱生成逻辑，
 * 将其接入 Lootr 每位玩家独立战利品系统。
 */
@Mixin(value = {BaseTFBoss.class, KnightPhantom.class}, remap = false)
public abstract class TFBossDepositMixin {
    @Redirect(
            method = "postRemoval",
            at = @At(
                    value = "INVOKE",
                    target = "Ltwilightforest/entity/boss/IBossLootBuffer;depositDropsIntoChest(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)V",
                    remap = false
            ),
            remap = false
    )
    private <T extends LivingEntity & IBossLootBuffer> void lmc$redirectDepositDrops(
            T boss,
            BlockState chest,
            BlockPos pos,
            ServerLevel serverLevel
    ) {
        if (!TwilightForestCompatHandler.depositLootrBossChest(boss, chest, pos, serverLevel)) {
            IBossLootBuffer.depositDropsIntoChest(boss, chest, pos, serverLevel);
        }
    }
}
