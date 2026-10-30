# Biomic

A world generator for Minecraft 1.7.10: [Origen](https://github.com/Rearth/Origen)'s terrain with [Regions Unexplored](https://github.com/Apollounknowndev/RegionsUnexplored) 
habitats, cave biomes, sky islands and a new Nether (with biomes ported from [Tartarus](https://github.com/PolyhedralDev/Tartarus), and [Gardens of the Dead](https://github.com/ochotonida/gardens-of-the-dead)). Create a world with the world type **Biomic**.

Native integration with [Biomes O' Plenty](https://github.com/Glitchfiend/BiomesOPlenty).

<!--

[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/biomic)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/biomic)
[![67](images/badges/67.png)](https://67minecraft.com/mod/biomic)
-->

[![hub](images/badges/github.png)](https://github.com/AngelicIndustries/Biomic/releases)
[![maven](images/badges/maven.png)](https://maven.angelic.industries/#/releases/industries/angelic/biomic/Biomic)
[![cord](images/badges/cord.png)](https://discord.gg/xAWCqGrguG)
![forge](images/badges/forge.png)

## Requirements

- [AngelLib](https://github.com/AngelicIndustries/AngelLib) 1.0.0 [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/angellib) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/angellib) [![67](images/icons/67.png)](https://67minecraft.com/mod/angellib) [![git](images/icons/git.png)](https://github.com/AngelicIndustries/AngelLib/releases)
- [Et Futurum Requiem](https://github.com/GTNewHorizons/Et-Futurum-Requiem) 2.6.54-GTNH [![git](images/icons/git.png)](https://github.com/GTNewHorizons/Et-Futurum-Requiem/releases) (GTNH's builds; CurseForge and Modrinth have only older ones)
- [EndlessIDs](https://github.com/GTMEGA/EndlessIDs) 1.7.4 [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/endlessids) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/endlessids) [![git](images/icons/git.png)](https://github.com/GTMEGA/EndlessIDs/releases)
- [ChunkAPI](https://github.com/LegacyModdingMC/ChunkAPI) 0.8.4 [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/chunkapi) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/chunkapi) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/ChunkAPI/releases)
- [GTNHLib](https://github.com/GTNewHorizons/GTNHLib) 0.11.39 [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/gtnhlib) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/gtnhlib) [![git](images/icons/git.png)](https://github.com/GTNewHorizons/GTNHLib/releases)
- [UniMixins](https://github.com/LegacyModdingMC/UniMixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)

On its first launch the client downloads modern Minecraft assets, through Et Futurum Requiem.

Recommended: [Hohen Angst](https://github.com/AngelicIndustries/Hohenangst) for 512-high worlds, with
[ArchaicFix](https://github.com/embeddedt/ArchaicFix) for its lighting engine.

For mods building on Biomic: [docs/api.md](docs/api.md) and [docs/biome-api.md](docs/biome-api.md).

## Building

```sh
./gradlew build
```

## License

Biomic's own code is available under the [MIT License](LICENSE-MIT) or the [GNU GPL version 3](LICENSE). The
Aeropelago port is GPL-3.0-only, so the jar as a whole is GPL-3.0. Third-party material and its licences are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

<br>

![license](images/license_small.png)
