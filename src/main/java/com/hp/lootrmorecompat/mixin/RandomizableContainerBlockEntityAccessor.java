package com.hp.lootrmorecompat.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 1.20.1 原版 RandomizableContainerBlockEntity 的受保护战利品表与种子字段。
 */
@Mixin(RandomizableContainerBlockEntity.class)
public interface RandomizableContainerBlockEntityAccessor {
    @Accessor("lootTable")
    @Nullable
    ResourceLocation lmc$getLootTable();

    @Accessor("lootTable")
    void lmc$setLootTable(@Nullable ResourceLocation lootTable);

    @Accessor("lootTableSeed")
    long lmc$getLootTableSeed();
}
