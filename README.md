# Matter Overdrive for Minecraft 1.21.1 – 26.3 (NeoForge and Fabric)

![Matter Overdrive](docs/modrinth/banner.png)

An unofficial port of [Matter Overdrive](https://github.com/simeonradivoev/MatterOverdrive) by Simeon Radivoev
from Minecraft 1.7.10 to **Minecraft 1.21.1 – 1.21.10 and 26.1.2 – 26.3** on **NeoForge** and **Fabric** (Fabric API;
Team Reborn Energy for FE is included in the jar).

The port is a rewrite against modern APIs that keeps the original's behaviour, numbers, textures, sounds and
translations (English and Russian; the port adds Ukrainian, German, Spanish and Portuguese (Brazil), not by native
speakers). The 1.7.10 source (0.4.2) is the specification.

**Download:** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/matter-overdrive-unofficial-port) ·
[Modrinth](https://modrinth.com/mod/matter-overdrive-1.21-unofficial-port) ·
[GitHub releases](https://github.com/Vladeezyy/matter-overdrive-1.21/releases)

Version 1.0.2: a NeoForge jar and a Fabric jar for every Minecraft version.

| Minecraft | NeoForge | Fabric | Java | Source branch |
|---|---|---|---|---|
| 26.3 | 26.3.0.94 or newer | Fabric API 0.164.0 or newer | 25 | `mc/26.3` |
| 26.2 | 26.2.0.93 or newer | Fabric API 0.161.0 or newer | 25 | `mc/26.2` |
| 26.1.2 | 26.1.2.121 or newer | Fabric API 0.155.3 or newer | 25 | `mc/26.1.2` |
| 1.21.10 | 21.10.64 or newer | Fabric API 0.138.4 or newer | 21 | `fabric-1.21.10` |
| 1.21.8 | 21.8.54 or newer | Fabric API 0.136.1 or newer | 21 | `fabric-1.21.8` |
| 1.21.5 | 21.5.98 or newer | Fabric API 0.128.2 or newer | 21 | `fabric-1.21.5` |
| 1.21.4 | 21.4.158 or newer | Fabric API 0.119.4 or newer | 21 | `fabric-1.21.4` |
| 1.21.3 | 21.3.97 or newer | Fabric API 0.114.1 or newer | 21 | `fabric-1.21.3` |
| 1.21.1 | 21.1.197 or newer | Fabric API 0.116.17 or newer | 21 | `fabric-1.21.1` |

Install on the server and on every client. The `fabric-*` and `mc/26.*` branches build both loaders from one
`common` module; `main` and the plain `1.21.x` branches are the earlier NeoForge-only code (1.0.0 – 1.0.1).

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
Resources for simple content are generated from the original 1.7.10 assets by `tools/gen_resources.py`
(see MODLOG.md for the command).

## Performance
1.0.1 measured machines against vanilla furnaces in the same scene (`tools/perf.sh`, see MODLOG.md): idle inscribers and
pattern storages render as fast as furnaces, a big working base costs about half the frame rate and server time it did
in 1.0.0, gravitational anomalies scan their surroundings about 9 times cheaper.

## License and credits
GPL-3.0-or-later, the same license as the original — see [LICENSE.md](LICENSE.md).
Original mod, art, sounds and translations: Simeon Radivoev and the Matter Overdrive contributors.

<details>
<summary><b>🇷🇺 Русский — нажмите, чтобы открыть</b></summary>

## Matter Overdrive для Minecraft 1.21.1 – 1.21.10 (NeoForge)

Неофициальный порт [Matter Overdrive](https://github.com/simeonradivoev/MatterOverdrive) (Simeon Radivoev) с
Minecraft 1.7.10. Переписан под современные API с сохранением поведения, чисел, текстур, звуков и переводов оригинала
(английский и русский; добавлены украинский, немецкий, испанский и португальский (Бразилия) — переводы не от носителей).

**Скачать:** [Modrinth](https://modrinth.com/mod/matter-overdrive-1.21-unofficial-port) ·
[GitHub releases](https://github.com/Vladeezyy/matter-overdrive-1.21/releases) — файл `matteroverdrive-1.0.1+<версия Minecraft>.jar`
для 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8 или 1.21.10. Java 21; ставить на сервер и на каждый клиент.

Перенесено всё из оригинала: материалы и руда, машины и энергия, материя и репликация, аномалии и термоядерный реактор,
энергетическое оружие, андроиды, мир (андроиды-изгои, неудачные животные, постройки, безумный учёный), звёздная карта,
квесты и рынок контрактов. Подробности — в [MODLOG.md](MODLOG.md).

**1.0.1:** новые версии Minecraft (1.21.1 – 1.21.8); машины в руке, в рамке и на земле больше не огромные; простаивающие
инскрайберы и хранилища шаблонов рисуются как обычные блоки; большая работающая база стоит примерно вдвое меньше FPS и
времени тика сервера, гравитационные аномалии — примерно в 9 раз дешевле.

## Лицензия
GPL-3.0-or-later, как у оригинала — см. [LICENSE.md](LICENSE.md). Оригинальный мод, графика, звуки и переводы — Simeon
Radivoev и участники Matter Overdrive.

</details>
