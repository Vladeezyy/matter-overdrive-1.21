# Matter Overdrive for Minecraft 1.21.8 (NeoForge)

![Matter Overdrive](docs/modrinth/banner.png)

An unofficial port of [Matter Overdrive](https://github.com/simeonradivoev/MatterOverdrive) by Simeon Radivoev
from Minecraft 1.7.10 to **Minecraft 1.21.8** on **NeoForge 21.8**.

This is the `1.21.8` branch. Every supported Minecraft version has its own branch with the same game code: `main`
(1.21.10), `1.21.8`, `1.21.5`, `1.21.4`, `1.21.3` and `1.21.1`. The APIs that differ are bridged in
`matteroverdrive.compat`, and `tools/backport.py` converts the generated resources for the branch's version.

The port is a rewrite against modern APIs that keeps the original's behaviour, numbers, textures, sounds and
translations (English and Russian). The 1.7.10 source (0.4.2) is the specification.

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

## Building
Requires JDK 21.
```bash
./gradlew build              # jar in build/libs/
./gradlew runClient          # dev client
./gradlew runGameTestServer  # headless in-game tests
./gradlew runScene           # scripted scene: screenshots of the machines and features to run/screenshots/
```
`runScene` needs a world named `mo_scene` in `run/saves/` (any copy of a dev world).
Resources for simple content are generated from the original 1.7.10 assets by `tools/gen_resources.py`, then
converted for the branch's Minecraft version by `tools/backport.py` (see MODLOG.md for the commands).

## License and credits
GPL-3.0-or-later, the same license as the original — see [LICENSE.md](LICENSE.md).
Original mod, art, sounds and translations: Simeon Radivoev and the Matter Overdrive contributors.
