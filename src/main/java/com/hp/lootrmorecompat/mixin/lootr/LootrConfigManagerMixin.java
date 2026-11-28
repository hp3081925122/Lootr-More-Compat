package com.hp.lootrmorecompat.mixin.lootr;

import com.hp.lootrmorecompat.compat.ConfluenceCompatHandler;
import com.hp.lootrmorecompat.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import noobanidus.mods.lootr.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.20.1 Forge 下拦截 Lootr 将汇流来世（Confluence）主题箱与死人陷阱箱强行替换为原版木箱，
 * 交由本模组 ConfluenceCompatHandler 原地接管以保留外观、钥匙锁、死人毛衣与机械电路陷阱。
 */
@Mixin(value = ConfigManager.class, remap = false)
public abstract class LootrConfigManagerMixin {
    @Inject(method = "replacement", at = @At("HEAD"), cancellable = true)
    private static void lmc$skipConfluenceReplacement(BlockState original, CallbackInfoReturnable<BlockState> cir) {
        if (original == null) {
            return;
        }
        if (ModConfig.COMMON_SPEC.isLoaded() && !ModConfig.COMMON.enableConfluenceChests.get()) {
            return;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(original.getBlock());
        if (blockId != null && ConfluenceCompatHandler.MOD_ID.equals(blockId.getNamespace())) {
            cir.setReturnValue(null);
        }
    }
}
