# Matter Overdrive for Minecraft 1.21.10 (NeoForge)

An unofficial port of [Matter Overdrive](https://github.com/simeonradivoev/MatterOverdrive) by Simeon Radivoev
from Minecraft 1.7.10 to **Minecraft 1.21.10** on **NeoForge 21.10**. Work in progress.

The port is a rewrite against modern APIs that keeps the original's behaviour, numbers, textures, sounds and
translations (English and Russian). The 1.7.10 source (0.4.2) is the specification.

## Status
| Phase | Content | State |
|---|---|---|
| 1 | Materials, tritanium/dilithium ores + world gen, tritanium block, tools, armor, isolinear circuits, recipes, `c:` tags | ✅ |
| 2 | Machine framework: FE energy, machine GUIs, upgrades, batteries, charging station, solar panel, inscriber | ⏳ |
| 3 | Matter: decomposer, recycler, pipes, pattern storage, analyzer, replicator, network | — |
| 4 | Gravitational anomaly, fusion reactor | — |
| 5 | Weapons: phaser, phaser rifle, plasma shotgun, ion sniper, weapon station | — |
| 6 | Androids | — |
| 7 | Mobs, structures, transporter, star map, quests | — |

Details: [MODDING_PLAN.md](MODDING_PLAN.md) and the work log [MODLOG.md](MODLOG.md).

## Building
Requires JDK 21.
```bash
./gradlew build              # jar in build/libs/
./gradlew runClient          # dev client
./gradlew runGameTestServer  # headless in-game tests
```
Resources for simple content are generated from the original 1.7.10 assets by `tools/gen_resources.py`
(see MODLOG.md for the command).

## License and credits
GPL-3.0-or-later, the same license as the original — see [LICENSE.md](LICENSE.md).
Original mod, art, sounds and translations: Simeon Radivoev and the Matter Overdrive contributors.
This port was written with the help of an AI coding agent (Claude Code).
