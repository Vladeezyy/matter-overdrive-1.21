# Matter Overdrive for Minecraft 1.21.1 (NeoForge and Fabric)

![Matter Overdrive](docs/modrinth/banner.png)

An unofficial port of [Matter Overdrive](https://github.com/simeonradivoev/MatterOverdrive) by Simeon Radivoev
from Minecraft 1.7.10 to **Minecraft 1.21.1** on **NeoForge 21.1** and **Fabric** (Fabric API; Team Reborn Energy for
FE is included in the jar).

This branch builds both loaders from one `common` module (all game code, compiled on vanilla) plus thin `neoforge/`
and `fabric/` modules; the Minecraft 1.21.10 build lives on `main`. Both builds share the game code: the
1.21.10 APIs the code is written against are re-created for 1.21.1 in `matteroverdrive.compat`, and
`tools/backport_1_21_1.py` converts the generated resources.

The port is a rewrite against modern APIs that keeps the original's behaviour, numbers, textures, sounds and
translations (English and Russian; the port adds Ukrainian, German, Spanish and Portuguese (Brazil), not by native
speakers). The 1.7.10 source (0.4.2) is the specification.

**Download:** [Modrinth](https://modrinth.com/mod/matter-overdrive-1.21-unofficial-port) ·
[GitHub releases](https://github.com/Vladeezyy/matter-overdrive-1.21/releases)

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
