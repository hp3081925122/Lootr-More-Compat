package com.hp.lootrmorecompat.mixin.twilightforest;

import com.hp.lootrmorecompat.compat.TwilightForestCompatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.boss.Naga;
import twilightforest.entity.boss.UrGhast;

/**
 * 重定向 1.20.1 暮色森林娜迦、巫妖与暮初恶魂的 Boss 战利品箱生成，接入 Lootr 独立战利品。
 */
@Mixin({Naga.class, Lich.class, UrGhast.class})
public abstract class TFBossRemoveMixin {
    @Redirect(
            method = "remove",
            at = @At(
                    value = "INVOKE",
                    target = "Ltwilightforest/entity/boss/IBossLootBuffer;depositDropsIntoChest(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)V",
                    remap = false
            )
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
