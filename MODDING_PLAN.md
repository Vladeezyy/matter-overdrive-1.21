# Matter Overdrive → Minecraft 1.21.10 (NeoForge) — port plan

## Target
- Minecraft **1.21.10**, NeoForge **21.10.64** (ModDevGradle MDK, Parchment 2025.10.12), **Java 21**.
- Mod id `matteroverdrive`, package `matteroverdrive`, license **GPL-3.0-or-later** (required: the original is GPL-3.0).
- Reference build: Matter Overdrive **0.4.2 for 1.7.10** (jar in `_original/`, not shipped).

## Sources (outside the repo, in `~/mo-reference/`)
| Folder | What | Use |
|---|---|---|
| `mo-1.7.10` | simeonradivoev/MatterOverdrive, branch `1.7.10` (927 .java, GPL-3.0) | **Spec**: behaviour, numbers, assets, lang (incl. ru_RU) |
| `mo-ce-1.19.2` | ibonny/MatterOverdrive-Community-Edition-1.19.2 (392 .java, GPL-3.0, Forge 43) | Modern-API patterns for parts it already did (worldgen, basic matter machines). No androids/weapons. |

No decompiling needed: the jar's source is public. Other ports checked: 1.20.1 attempts (outtieTV, chauve-dev) are
non-compiling archives; nothing exists for 1.21.

## Route
Loader-API rewrite on NeoForge: **not** a mechanical port. 1.7.10 → 1.21.10 changes almost every API
(block metadata → blockstates, TileEntity → BlockEntity, TESR/ISBRH → JSON models + BER, NBT on stacks →
data components, CoFH RF → NeoForge `IEnergyStorage` capability, SimpleNetworkWrapper → payloads,
GUI containers → menus/screens, ore dictionary → tags, world gen → data-driven features/structures).
Each feature is rewritten against the 1.7.10 code as the spec and keeps its numbers, textures, sounds and lang.

## Phases (each ends working in `runClient`)
1. **Base content** ✅ — materials, ores + worldgen, tritanium block, tools, armor, isolinear circuits, recipes, tags, GameTests.
2. **Machine framework** ✅ — base machine, FE energy (transfer API), menus + screens in MO style, upgrades,
   redstone config, wrench, batteries, solar panel, inscriber + its recipes, DevScene screenshots.
3. **Matter core** ✅ — matter values (data map + recipe calculation), Decomposer, Matter Recycler, matter pipes,
   pattern drives, Matter Analyzer, network (pipes, router, switch), Pattern Storage, Pattern Monitor, **Replicator**.
   Matter Scanner and router/switch filters still to do.
4. **Power endgame** ✅ — Gravitational anomaly (+ world gen) and stabilizer, space-time equalizer, Fusion reactor multiblock.
5. **Weapons** ✅ — Phaser, Phaser Rifle, Plasma Shotgun, Ion Sniper, modules, Weapon Station, energy pack.
6. **Androids** ✅ — player attachment for android state, biotic stats tree, Android Station, HUD, abilities,
   charging station (it only charges androids).
7. **World & extras** — rogue androids, mad scientist, structures (sand/crashed ship etc.), Tritanium crate,
   holo sign, inscriber, transporter, star map / galaxy, quests & dialogs, guide book (Data Pad), foods.

Original module sizes for scale: gui 105 files, client/render 76, items 53, blocks 41, tile 33,
entity 22, machines 20, matter_network 20, starmap 17, biostats 15.
