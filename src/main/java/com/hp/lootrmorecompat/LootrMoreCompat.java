package com.hp.lootrmorecompat;

import com.hp.lootrmorecompat.client.ClientParticleHandler;
import com.hp.lootrmorecompat.command.LmcCommands;
import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.event.CommonEvents;
import com.hp.lootrmorecompat.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Lootr More Compat（Lootr 更多兼容）1.20.1 Forge 主入口。
 */
@Mod(LootrMoreCompat.MOD_ID)
public final class LootrMoreCompat {
    public static final String MOD_ID = "lootr_more_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LootrMoreCompat() {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_SPEC);

        ModNetworking.register();

        MinecraftForge.EVENT_BUS.addListener(CommonEvents.rightClickPriority(), CommonEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::onChunkWatch);
        MinecraftForge.EVENT_BUS.addListener(LmcCommands::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.addListener(ClientParticleHandler::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(ClientParticleHandler::onLoggingOut);
            MinecraftForge.EVENT_BUS.addListener(ClientParticleHandler::onClone);
        }

        logDetectedCompatMods();
    }

    private static void logDetectedCompatMods() {
        String[] knownMods = {
                "aether", "twilightforest", "aquamirae", "confluence", "iceandfire",
                "deeperdarker", "supplementaries", "alexscaves", "undergarden",
                "blue_skies", "ars_nouveau", "betterend", "betternether",
                "quark", "cobblemon"
        };
        List<String> detected = new ArrayList<>();
        for (String id : knownMods) {
            if (ModList.get().isLoaded(id)) {
                detected.add(id);
            }
        }
        LOGGER.info("Lootr More Compat initialized; detected optional compat mods: {}", detected);
    }
}
