# Position-aware biome API

Biomic exposes a deliberately small static API at
`industries.angelic.biomic.api.BiomicBiomeApi`:

```java
BiomeGenBase getBiome(World world, int x, int y, int z);
String getBiomeKey(World world, int x, int y, int z);
Set<String> getBiomeTags(World world, int x, int y, int z);
BiomeGenBase getSurfaceBiome(World world, int x, int z);
```

```java
// API version 2 (BiomicApi.VERSION >= 2)
BiomicBiomeApi.Lookup lookup(World world, int x, int y, int z);   // biome, key and tags in one lookup
List<BiomicBiomeApi.KeySpan> columnKeys(World world, int x, int z); // the keys of a loaded column by height
```

`getBiome` is the normal position-aware entry point for the planned JAS fork. In a
loaded Biomic chunk it resolves the quart-resolution ChunkAPI volume. A `biomic:surface`
cell delegates to that column's EndlessIDs surface biome. An absent volume, unloaded
chunk, non-Biomic world, or out-of-height query falls back to the ordinary surface
biome. That path can load or generate a chunk through the world's lazy surface lookup; `columnKeys` is the
read-only one.

`lookup` answers what `getBiome`, `getBiomeKey` and `getBiomeTags` answer, from one lookup instead of three. Tag sets
are resolved once per stored key (after Forge has finished loading) and shared, so a lookup allocates only its small
result. `columnKeys` lists a column's keys from the bottom up, each with the blocks it covers (`fromY` inclusive, `toY`
exclusive, at the volume's 4-block resolution), as `getBiomeKey` reports them (a surface cell under its surface
biome's key). It is read-only: an unloaded chunk gives an empty list and is not loaded. A spawner can use it to look
for a floor inside an island's span rather than on the column's top block.

## Biome catalogue (version 2)

`BiomeCatalogue` lists every key once, for reading at startup (default groups, config checks, commands):

```java
List<BiomeCatalogue.Entry> entries();   // stable order
BiomeCatalogue.Entry get(String key);    // null when Biomic does not know the key
Set<String> getTags(String key);         // what getBiomeTags reports for a cell with that key
boolean isComplete();                    // true once Forge has finished loading
record Entry(String key, Set<String> tags, BiomeGenBase biome, Kind kind) // kind: SURFACE, CAVE, FLOATING, NETHER
```

It holds what a chunk's 3D volume can store and the surface identities Biomic generates or hosts: the surface marker
`biomic:surface` (no tags of its own: a surface cell reports its surface biome), the caves (Origen's, Regions
Unexplored's, Alex's Caves'), both island families (Origen's floating profiles, Sky Donut, Aeropelago and its
`biomic:aeropelago/origen/<profile>` hosts, and `biomic:hosted/<namespace>/<biome>` for the habitats islands host), and
the surface and Nether identities (Origen's, Regions Unexplored's, Biomic's own, the vanilla, BOP and SaltyMod habitats
it hosts, Tartarus and the other mods' Nether biomes it takes in). `biome` is null where a key resolves to the column's
own surface biome (`biomic:surface`, Alex's Caves keys). Entries are resolved by the same code as the position lookups.
The order is the volume's keys as declared, then the island habitats, then the surface and Nether identities, each by
key. BOP's, SaltyMod's and Netherlicious's entries depend on what is installed, so the catalogue is complete once Forge
has finished loading (`isComplete`); asked earlier it lists what is registered so far. It lists what a new world can
store with this installation and its current config: BOP's curated habitats (all of them with `bopFullCatalogue`), the
Nether biomes its switches allow, and none of the keys Biomic registers but never generates (Biomic Temperate
Lowlands, Temperate Archipelago, Windswept Highlands, Estuary and Lush Caves, and Alex's Caves' retired Forlorn Hollows
and Primordial Caves). A given world stores a subset: its height decides the island keys, and saved worlds keep the
selection they were made with.

Which island keys a world stores: 512-high worlds (Hohen Angst) store `biomic:hosted/...` on their sky islands only.
256-high worlds store Origen's floating profiles, Sky Donut, Aeropelago and Aeropelago's hosts in the islands' bodies,
and `biomic:hosted/...` over their surfaces. Keys are stable: one that has to change keeps the old one as an alias.

Every island key is distinct from the ground's: Origen's eight floating profiles are stored as
`biomic:floating/origen/<profile>` (`lush_loops`, `sakura_streams`, `snowy_spires`, `arid_arboretum`, `rocky_refuge`,
`frozen_fungi`, `marine_monoliths`, `foliage_fortress_inner`), resolving to the profile's Origen biome with
`FLOATING_ISLAND` added; Aeropelago's hosts are `biomic:aeropelago/origen/<profile>`; islands' hosted habitats are
`biomic:hosted/<namespace>/<biome>`. (Before 2026-10-30 the floating profiles stored their ground biome's own key;
256-high worlds saved earlier keep that in their islands' bodies.)

Stable authored keys are constants in `BiomeKeys`; semantic tag constants are in
`BiomeTags`. If a volume contains an unknown future key, `getBiomeKey` preserves that
key while `getBiome` falls back to the surface biome and `getBiomeTags` returns an
empty set. An ordinary non-Biomic biome without a stable Biomic registration is
reported as `legacy:<numeric-id>`.

In game, operators can open the biome browser (`/biomes`, or a key bound under Controls): it lists every biome with
its key, tags and source, and takes them there (see `biome-browser.md`).

Generation has one authority boundary per chunk. `ChunkProviderBiomic` samples a single
immutable volume before terrain generation, passes that exact volume to cave shapes and
both cave and floating-island surface rules, then attaches it to the resulting chunk. Population adapters such as
EFR lush-cave vegetation read the attached volume instead of re-running selection noise.
Vertical identities are declared by stable key in `VerticalBiomeCatalogue`; palette
indices are only chunk-local compression details and must never acquire behavior.

Aeropelago may store a composite key such as `biomic:aeropelago/origen/lush_loops`. The runtime biome resolves to the
registered Origen identity, while `getBiomeKey` preserves the composite host key and `getBiomeTags` includes both the
Origen semantics and `FLOATING_ISLAND`/`ARCHIPELAGO`. This lets a future JAS adapter distinguish a Lush Loops habitat
in the sky from the same ecosystem on the ground without allocating another EndlessIDs numeric biome for every
combination.

The temporary scaffold identities introduced by generator version 2 remain readable during the terrain reset.
Clover Plains is the first final RU-derived identity activated by generator version 9:

| Stable key | Default numeric ID | Core tags |
| --- | ---: | --- |
| `biomic:temperate_lowlands` | 6000 | `SURFACE`, `TEMPERATE`, `HUMID` |
| `biomic:temperate_archipelago` | 6001 | `SURFACE`, `OCEAN`, `ARCHIPELAGO`, `TEMPERATE`, `HUMID` |
| `biomic:windswept_highlands` | 6002 | `SURFACE`, `MOUNTAIN`, `TEMPERATE`, `HUMID` |
| `biomic:lush_caves` | 6003 | `CAVE`, `TEMPERATE`, `HUMID` |
| `biomic:clover_plains` | 6004 | `SURFACE`, `PLAINS`, `FLORAL`, `WARM` |
| `biomic:sky_donut` | 6012 | `FLOATING_ISLAND`, `PLAINS`, `TEMPERATE`, `HUMID` |
| `biomic:aeropelago` | 6013 | `FLOATING_ISLAND`, `ARCHIPELAGO`, `PLAINS`, `TEMPERATE` |

Numeric IDs are configurable pack-assembly details and are included in the immutable
world settings signature. Stable keys—not IDs—are the persistence and integration
identity.
