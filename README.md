# Lootr More Compat

**Lootr More Compat** extends Lootr's per-player instanced loot mechanics to boss reward chests, key-locked dungeon chests, break-to-loot blocks, and custom modded loot containers from popular adventure and dimension mods — while preserving their original block models, unlock mechanics, and special interactions.

- **CurseForge**: https://www.curseforge.com/minecraft/mc-mods/lootr-more-compat
- **Branches**:
  - `1211-neoforge` — Minecraft 1.21.1 (NeoForge)
  - `1201-forge` — Minecraft 1.20.1 (Forge)

## Features

### Dedicated Adventure & Boss Compat
- **The Aether — Treasure Chests**: Per-player instanced Lootr chests preserving Bronze, Silver, and Gold Dungeon Key unlocking, lid animations, and unlock sounds (supports both shared-unlock and per-player key-unlock modes).
- **The Twilight Forest — All 8 Boss Chests**: Per-player instanced victory chests for Naga, Lich, Minoshroom, Hydra, Knight Phantoms, Ur-Ghast, Alpha Yeti, and Snow Queen, preserving original wood chests, sounds, particles, and fallback loot guarantee.
- **Aquamirae — Frozen Chests**: Per-player instanced loot for Frozen Chests unlocked with Frozen Keys (shared-melt or per-player key unlock).
- **Confluence: Otherworld — All 23 Chests, Tree Holes & Dungeon Lost Paper**: In-place per-player Lootr support for all 12 locked chests (Golden, Dungeon, Shadow with non-consumed Shadow Key, and all 9 Biome Chests with shared or per-player key unlock modes), both Death Chest traps (preserving mechanical trap activation, Wrench/Wire Cutter tool pass-through, and guaranteed per-player Dead Man's Sweater), all 9 unlocked themed chests, Tree Holes, and naturally generated Dungeon Lost Paper.
- **Ice and Fire — Ghost Chests**: Per-player instanced loot for Ghost Chests with optional first-open Ghost ambush preservation.

### Break-to-Loot Blocks
- Per-player loot generation for **Deeper and Darker Ancient Vases**, **Supplementaries Treasure Urns**, and configurable custom blocks (`customBreakLootBlocks`).

### Universal Modded Loot Containers
- Automatic in-place per-player Lootr inventories (1–6 rows / 9–54 slots) for modded loot-table containers without destroying original 3D models or animations, protected by configurable mod and block blacklists.

### Built-in Optional Convert Tags & QoL
- Ships optional Lootr conversion tags for Twilight Forest, BetterEnd, BetterNether, Blue Skies, Quark, and Ars Nouveau.
- Unopened golden sparkle particles, sneak-to-break protection, and `/lootrmorecompat` (`/lmc`) admin commands (`info`, `reset`, `list`).

## Requirements
- **Lootr** (Required)
- All supported adventure and content mods are optional.

---

# Lootr More Compat（Lootr 更多兼容）

**Lootr More Compat** 为热门冒险、Boss 与维度模组的特殊宝箱、钥匙宝藏箱、破坏掉落容器及自定义模组战利品容器扩展 Lootr 独立战利品支持——在让每位玩家都能各自获取完整奖励的同时，完整保留原模组的方块模型、解锁机制与特殊交互效果。

- **CurseForge**：https://www.curseforge.com/minecraft/mc-mods/lootr-more-compat
- **分支说明**：
  - `1211-neoforge` — Minecraft 1.21.1（NeoForge）
  - `1201-forge` — Minecraft 1.20.1（Forge）

## 核心功能

### 热门冒险与 Boss 专属兼容
- **天境（The Aether）— 宝藏箱**：支持青铜、白银、黄金地牢宝藏箱玩家独立战利品，保留地牢钥匙解锁机制、开盖动画与音效，支持共享解锁与单人独立消耗钥匙模式。
- **暮色森林（The Twilight Forest）— 全 8 大 Boss 战利品箱**：覆盖娜迦、巫妖、米诺菇、九头蛇、幻影骑士、暮初恶魂、雪怪首领与冰雪女王，保留原木宝箱外观、音效与粒子，并提供保底掉落。
- **深海迷航（Aquamirae）— 冰冻宝箱**：支持冰冻钥匙开启的冰冻宝箱玩家独立战利品（可配置共享解冻或单人独立消耗钥匙）。
- **汇流来世（Confluence: Otherworld）— 全部 23 种宝箱、树洞与地牢遗落纸张**：原地兼容全部 12 种上锁宝箱（金宝箱、地牢箱、暗影箱[暗影钥匙按原版不消耗]与 9 大地牢群系箱，支持共享解锁或每人独立钥匙解锁）、2 种死人宝箱陷阱（保留首次机械陷阱触发、扳手/钢丝钳拆线放行与每人必得死人毛衣）、9 种未上锁主题箱、树洞及地牢自然生成的遗落纸张。
- **冰与火之歌（Ice and Fire）— 幽灵箱**：支持玩家独立战利品，并可配置保留首次开启时的幽灵伏击机制。

### 破坏掉落型容器兼容
- 内置 **深暗之园（Deeper and Darker）远古花瓶** 与 **锦致装饰（Supplementaries）宝藏骨灰瓮**，支持通过 `customBreakLootBlocks` 配置项自由扩展任意模组方块。

### 通用模组战利品容器原地兼容
- 自动原地接管携带战利品表的模组容器（1–6 行 / 9–54 格），保留原方块 3D 模型与动画，并内置可配置模组/方块安全黑名单。

### 内置可选转换标签与实用功能
- 内置暮色森林、BetterEnd、BetterNether、蔚蓝浩空、夸克与新生魔艺的可选 Lootr 转换标签。
- 配套未开启粒子提示、潜行防误拆保护及 `/lootrmorecompat`（`/lmc`）管理员指令（`info`、`reset`、`list`）。

## 前置需求
- **Lootr**（必需）
- 所有兼容的目标冒险与内容模组均为可选依赖。
