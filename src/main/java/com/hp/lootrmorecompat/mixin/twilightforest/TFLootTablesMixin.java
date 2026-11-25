package com.hp.lootrmorecompat.mixin.twilightforest;

import com.hp.lootrmorecompat.compat.TwilightForestCompatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.loot.TFLootTables;

/**
 * 拦截 1.20.1 暮色森林九头蛇、米诺菇、冰雪女王与雪怪首领通过 TFLootTables.entityDropsIntoContainer 生成的 Boss 战利品箱。
 */
@Mixin(value = TFLootTables.class, remap = false)
public abstract class TFLootTablesMixin {
    @Inject(method = "entityDropsIntoContainer", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lmc$onEntityDropsIntoContainer(
            LivingEntity entity,
            DamageSource source,
            BlockState containerState,
            BlockPos pos,
            CallbackInfo ci
    ) {
        if (TwilightForestCompatHandler.depositLootrEntityDrops(entity, source, containerState, pos)) {
            ci.cancel();
        }
    }
}
