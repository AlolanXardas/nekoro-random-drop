# Nekoro Random Drop

Extra random drops from mined blocks, killed mobs and (with Cobblemon) defeated or caught Pokémon, all set up in one config file. Rules can be limited to a height range, a dimension or a biome. With JEI installed, a Random Drop tab shows every rule.

Made mainly for my Nekoro Pokemon modpack, but it works with any items from any mod.

Download: [CurseForge](https://www.curseforge.com/minecraft/search?search=nekoro+random+drop)

## Versions

| Minecraft | Loaders |
|---|---|
| 1.21.1 | NeoForge, Forge, Fabric |
| 1.20.1 | Forge (also NeoForge 1.20.1), Fabric (also Quilt) |

Cobblemon and JEI are optional.

## Config

`config/Nekoro/random_drop.json` is created on first start. It explains every option and has commented examples. The default file ([source/common/resources/nekororandomdrop/default_config.json](source/common/resources/nekororandomdrop/default_config.json)) makes mined stone and deepslate drop ores like vanilla ore generation would, for worlds without ores; delete those rules if your world still has ores.

```json
{
  "rules": [
    { "source": "block", "match": "#c:stones", "item": "minecraft:flint", "chance": 0.02 },
    { "source": "block", "match": "minecraft:deepslate", "item": "minecraft:diamond", "chance": 0.002, "max_y": -40 },
    { "source": "pokemon_kill", "type": "fire", "item": "minecraft:blaze_powder", "chance": 0.1 }
  ]
}
```

Commands: `/randomdrop reload` and `/randomdrop list` (operators only).

## Building

Needs JDK 21. The code is shared between loaders in `source/common`; every loader and Minecraft version has its own Gradle project in `source/`.

```
cd source
.\build-all.ps1                  # all projects, jars go to ..\Gotowe\<version>
.\build-all.ps1 fabric-1.21.1    # selected ones
```

Or run `gradlew build` inside one project folder.

## License

MIT, see [LICENSE](LICENSE). Mod by Alolan Xardas, code written with Claude.
