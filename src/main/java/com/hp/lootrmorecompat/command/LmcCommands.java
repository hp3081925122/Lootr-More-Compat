package com.hp.lootrmorecompat.command;

import com.hp.lootrmorecompat.data.LmcContainerRecord;
import com.hp.lootrmorecompat.data.LmcSavedData;
import com.hp.lootrmorecompat.network.ModNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * 管理员与调试指令（1.20.1 Forge）：/lootrmorecompat（简写 /lmc）支持 info、reset、list。
 */
public final class LmcCommands {
    private LmcCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(buildRoot("lootrmorecompat"));
        dispatcher.register(buildRoot("lmc"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String name) {
        return Commands.literal(name)
                .then(Commands.literal("info")
                        .executes(ctx -> executeInfo(ctx.getSource())))
                .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> executeReset(ctx.getSource())))
                .then(Commands.literal("list")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> executeList(ctx.getSource())));
    }

    private static int executeInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("lootr_more_compat.command.player_only"));
            return 0;
        }
        HitResult hit = player.pick(8.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable("lootr_more_compat.command.no_target"));
            return 0;
        }
        BlockPos pos = blockHit.getBlockPos();
        ServerLevel level = player.serverLevel();
        LmcContainerRecord record = LmcSavedData.get(level).getRecord(pos);
        if (record == null) {
            source.sendSuccess(() -> Component.translatable(
                    "lootr_more_compat.command.not_tracked",
                    pos.toShortString()
            ).withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable(
                "lootr_more_compat.command.info",
                pos.toShortString(),
                record.getCompatType(),
                record.getLootTable() != null ? record.getLootTable().toString() : "fallback",
                record.getOpeners().size(),
                String.valueOf(record.hasOpened(player.getUUID()))
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int executeReset(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("lootr_more_compat.command.player_only"));
            return 0;
        }
        HitResult hit = player.pick(8.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable("lootr_more_compat.command.no_target"));
            return 0;
        }
        BlockPos pos = blockHit.getBlockPos();
        ServerLevel level = player.serverLevel();
        LmcSavedData data = LmcSavedData.get(level);
        LmcContainerRecord record = data.getRecord(pos);
        if (record == null) {
            source.sendFailure(Component.translatable("lootr_more_compat.command.not_tracked", pos.toShortString()));
            return 0;
        }
        record.resetForReloot();
        data.setDirty();
        ModNetworking.syncNewRecordToTracking(level, pos, record);
        source.sendSuccess(() -> Component.translatable(
                "lootr_more_compat.command.reset_ok",
                pos.toShortString()
        ).withStyle(ChatFormatting.AQUA), true);
        return 1;
    }

    private static int executeList(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int count = LmcSavedData.get(level).getRecordCount();
        source.sendSuccess(() -> Component.translatable(
                "lootr_more_compat.command.list",
                level.dimension().location().toString(),
                count
        ).withStyle(ChatFormatting.AQUA), false);
        return count;
    }
}
