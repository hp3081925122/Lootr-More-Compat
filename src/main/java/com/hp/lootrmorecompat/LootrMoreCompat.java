package com.hp.lootrmorecompat;

import com.hp.lootrmorecompat.client.ClientParticleHandler;
import com.hp.lootrmorecompat.command.LmcCommands;
import com.hp.lootrmorecompat.config.ModConfig;
import com.hp.lootrmorecompat.event.CommonEvents;
import com.hp.lootrmorecompat.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Lootr More Compat（Lootr 更多兼容）主入口。
 */
@Mod(LootrMoreCompat.MOD_ID)
public final class LootrMoreCompat {
    public static final String MOD_ID = "lootr_more_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LootrMoreCompat(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_SPEC);

        modEventBus.addListener(ModNetworking::register);

        NeoForge.EVENT_BUS.addListener(CommonEvents.rightClickPriority(), CommonEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onChunkWatch);
        NeoForge.EVENT_BUS.addListener(LmcCommands::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(ClientParticleHandler::onClientTick);
            NeoForge.EVENT_BUS.addListener(ClientParticleHandler::onLoggingOut);
            NeoForge.EVENT_BUS.addListener(ClientParticleHandler::onClone);
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
