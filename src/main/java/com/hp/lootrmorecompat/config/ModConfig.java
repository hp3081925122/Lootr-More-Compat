package com.hp.lootrmorecompat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 模组通用与客户端配置文件定义。
 */
public final class ModConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private ModConfig() {
    }

    public static final class Common {
        public final ModConfigSpec.BooleanValue enableAetherTreasureChest;
        public final ModConfigSpec.BooleanValue aetherPerPlayerKeyUnlock;
        public final ModConfigSpec.BooleanValue enableTwilightBossChest;
        public final ModConfigSpec.BooleanValue twilightBossRerollPerPlayer;
        public final ModConfigSpec.BooleanValue enableAquamiraeFrozenChest;
        public final ModConfigSpec.BooleanValue aquamiraePerPlayerKeyUnlock;
        public final ModConfigSpec.BooleanValue enableConfluenceChests;
        public final ModConfigSpec.BooleanValue confluencePerPlayerKeyUnlock;
        public final ModConfigSpec.BooleanValue preserveConfluenceDeathChestTrap;
        public final ModConfigSpec.BooleanValue enableConfluenceLostPaper;
        public final ModConfigSpec.BooleanValue preserveIceAndFireGhostAmbush;
        public final ModConfigSpec.BooleanValue enableBreakLootBlocks;
        public final ModConfigSpec.ConfigValue<List<? extends String>> customBreakLootBlocks;
        public final ModConfigSpec.BooleanValue enableUniversalModdedContainers;
        public final ModConfigSpec.BooleanValue protectCompatContainersFromBreak;
        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistedMods;
        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistedBlocks;

        public Common(ModConfigSpec.Builder builder) {
            builder.comment("天境（The Aether）地牢 Boss 宝箱兼容设置").push("aether");
            this.enableAetherTreasureChest = builder
                    .comment("是否启用天境青铜/白银/黄金地牢宝箱的 Lootr 独立战利品兼容")
                    .define("enableAetherTreasureChest", true);
            this.aetherPerPlayerKeyUnlock = builder
                    .comment("是否要求每位玩家各自消耗一把对应地牢钥匙才能解锁自己的独立战利品（false 表示一人解锁后所有玩家均可独立领取）")
                    .define("aetherPerPlayerKeyUnlock", false);
            builder.pop();

            builder.comment("暮色森林（The Twilight Forest）Boss 战利品箱兼容设置").push("twilightforest");
            this.enableTwilightBossChest = builder
                    .comment("是否将暮色森林 Boss 死亡时生成的战利品箱转为每位玩家独立的 Lootr 战利品箱")
                    .define("enableTwilightBossChest", true);
            this.twilightBossRerollPerPlayer = builder
                    .comment("是否为每位打开 Boss 战利品箱的玩家重新抽取一次该 Boss 的战利品表（false 表示向每位玩家复制一份击杀时的掉落物）")
                    .define("twilightBossRerollPerPlayer", true);
            builder.pop();

            builder.comment("深海秘境（Aquamirae）冰冻宝箱兼容设置").push("aquamirae");
            this.enableAquamiraeFrozenChest = builder
                    .comment("是否启用 Aquamirae 冰冻宝箱（aquamirae:frozen_chest）的 Lootr 独立战利品兼容")
                    .define("enableAquamiraeFrozenChest", true);
            this.aquamiraePerPlayerKeyUnlock = builder
                    .comment("是否要求每位玩家各自消耗一把冰冻钥匙解锁自己的独立战利品（false 表示一人解锁融冰后所有玩家均可独立领取）")
                    .define("aquamiraePerPlayerKeyUnlock", false);
            builder.pop();

            builder.comment("汇流来世（Confluence: Otherworld）宝箱与地牢遗落纸张兼容设置 / Confluence: Otherworld compat settings").push("confluence");
            this.enableConfluenceChests = builder
                    .comment("是否启用汇流来世全部主题箱、钥匙箱、9 大地牢群系箱、死人宝箱与树洞的 Lootr 独立战利品兼容 / Enable Lootr instanced loot compat for all Confluence chests and tree holes")
                    .define("enableConfluenceChests", true);
            this.confluencePerPlayerKeyUnlock = builder
                    .comment("是否要求每位玩家各自使用对应钥匙解锁上锁的汇流宝箱（false 表示一人开锁后全员均可独立领取；暗影钥匙按原版机制始终不消耗） / Require each player to unlock locked Confluence chests with their own key (Shadow Key is never consumed)")
                    .define("confluencePerPlayerKeyUnlock", false);
            this.preserveConfluenceDeathChestTrap = builder
                    .comment("首次开启汇流死人宝箱时，是否保留触发其连接的机械电路陷阱（飞镖/巨石/瞬爆 TNT） / Preserve mechanical trap activation on the first opening of a Confluence Death Chest")
                    .define("preserveConfluenceDeathChestTrap", true);
            this.enableConfluenceLostPaper = builder
                    .comment("是否允许右键交互地牢自然生成的遗落纸张（confluence:lost_paper）以打开每人独立的战利品界面 / Enable per-player Lootr loot for naturally generated Confluence dungeon Lost Paper")
                    .define("enableConfluenceLostPaper", true);
            builder.pop();

            builder.comment("冰火传说（Ice and Fire）幽灵宝箱设置").push("iceandfire");
            this.preserveIceAndFireGhostAmbush = builder
                    .comment("接管 iceandfire:ghost_chest 原地独立战利品时，是否在首次开启时保留原版幽灵伏击刷怪机制")
                    .define("preserveIceAndFireGhostAmbush", true);
            builder.pop();

            builder.comment("破坏掉落型战利品方块兼容（如深幽暗域远古花瓶、Supplementaries 洞穴陶罐）").push("break_loot_blocks");
            this.enableBreakLootBlocks = builder
                    .comment("是否允许右键交互自然生成的破坏掉落型战利品方块以打开独立 Lootr 战利品（不破坏原方块）")
                    .define("enableBreakLootBlocks", true);
            this.customBreakLootBlocks = builder
                    .comment("自定义破坏掉落型战利品方块列表，格式：方块ID|属性名=属性值|战利品表ID（属性条件可留空表示任意状态）")
                    .defineListAllowEmpty(
                            "customBreakLootBlocks",
                            List.of(
                                    "deeperdarker:ancient_vase|safe=false|deeperdarker:blocks/ancient_vase",
                                    "supplementaries:urn|treasure=true|supplementaries:blocks/urn"
                            ),
                            () -> "",
                            obj -> obj instanceof String s && !s.isBlank()
                    );
            builder.pop();

            builder.comment("通用第三方模组战利品容器原地兼容设置").push("universal");
            this.enableUniversalModdedContainers = builder
                    .comment("是否自动原地接管未被 Lootr 转换、但带有战利品表的第三方模组 RandomizableContainerBlockEntity（保留原方块模型与容量）")
                    .define("enableUniversalModdedContainers", true);
            this.protectCompatContainersFromBreak = builder
                    .comment("是否对已接入独立战利品的兼容容器启用防误拆保护（需潜行挖掘才能破坏）")
                    .define("protectCompatContainersFromBreak", true);
            this.blacklistedMods = builder
                    .comment("通用原地兼容忽略的模组命名空间列表")
                    .defineListAllowEmpty(
                            "blacklistedMods",
                            List.of("minecraft", "lootr", "eternal_starlight"),
                            () -> "",
                            obj -> obj instanceof String s && !s.isBlank()
                    );
            this.blacklistedBlocks = builder
                    .comment("通用原地兼容忽略的特定方块 ID 列表（如玩家墓碑、宝箱怪、索财灵宝箱）")
                    .defineListAllowEmpty(
                            "blacklistedBlocks",
                            List.of(
                                    "twilightforest:keepsake_casket",
                                    "aether:chest_mimic",
                                    "cobblemon:gimmighoul_chest"
                            ),
                            () -> "",
                            obj -> obj instanceof String s && !s.isBlank()
                    );
            builder.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue enableUnopenedParticles;
        public final ModConfigSpec.IntValue particleIntervalTicks;

        public Client(ModConfigSpec.Builder builder) {
            builder.comment("客户端未领取战利品视觉提示设置").push("visuals");
            this.enableUnopenedParticles = builder
                    .comment("是否在当前玩家尚未领取的兼容战利品容器上方生成星光粒子提示")
                    .define("enableUnopenedParticles", true);
            this.particleIntervalTicks = builder
                    .comment("客户端粒子提示刷新间隔（单位：tick，越大越省性能）")
                    .defineInRange("particleIntervalTicks", 12, 4, 100);
            builder.pop();
        }
    }
}
