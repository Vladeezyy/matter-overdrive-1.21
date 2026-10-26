# Matter Overdrive for Minecraft 1.20.1 (Forge and Fabric)

![Matter Overdrive](docs/modrinth/banner.png)

An unofficial port of [Matter Overdrive](https://github.com/simeonradivoev/MatterOverdrive) by Simeon Radivoev
from Minecraft 1.7.10 to **Minecraft 1.20.1** on **Forge 47** (the jar also runs on NeoForge 47.1 for 1.20.1) and
**Fabric** (Fabric API; Team Reborn Energy for FE is included in the jar).

This branch (`mc/1.20.1`) builds both loaders from one `common` module (all game code) plus thin `forge/` and `fabric/`
modules. It is a backport of the 1.21.1 branch: the 1.20.5+ APIs the code is written against are re-created for 1.20.1
in `matteroverdrive.compat` (`tools/port/backport_1_20_1.py` rewrites the sources), and
`tools/backport_resources_1_20_1.py` converts the resources. The 1.21.x and 26.x builds live on the other branches.

The port is a rewrite against modern APIs that keeps the original's behaviour, numbers, textures, sounds and
translations (English and Russian; the port adds Ukrainian, German, Spanish and Portuguese (Brazil), not by native
speakers). The 1.7.10 source (0.4.2) is the specification.

**Download:** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/matter-overdrive-unofficial-port) ·
[GitHub release 1.0.3 for 1.20.1](https://github.com/Vladeezyy/matter-overdrive-1.21/releases/tag/v1.0.3-1.20.1)

## Status
Everything from the original is ported:

| Area | Content |
|---|---|
| Materials | Tritanium / dilithium ores and world generation, tritanium tools and 3D armor, isolinear circuits, `c:` tags |
| Machines | FE energy, machine GUIs, upgrades, batteries, wrench, solar panel, inscriber, transporter, holo signs |
| Matter | Matter values, decomposer, recycler, pipes, analyzer, pattern storage, pattern monitor, replicator, network filters |
| Anomalies | Gravitational anomaly, stabilizer, fusion reactor |
| Weapons | Phaser, phaser rifle, plasma shotgun, ion sniper, omni tool, weapon station and modules |
| Androids | Pills, android station with all 14 abilities, bionic parts, HUD, charging station |
| World | Rogue androids, failed animals, mutant scientist, structures, the mad scientist and his village house |
| Galaxy | Star map, galaxy, planets, buildings and ships, quests, contract market, Data Pad guide |

Details: [MODDING_PLAN.md](MODDING_PLAN.md) and the work log [MODLOG.md](MODLOG.md), including every deliberate
difference from 1.7.10.

## License and credits
GPL-3.0-or-later, the same license as the original — see [LICENSE.md](LICENSE.md).
Original mod, art, sounds and translations: Simeon Radivoev and the Matter Overdrive contributors.
