# Order-independent population

Proposed 2026-10-22, built 2026-10-23 (both providers, 512 and 256). `-Dbiomic.liveOrderPopulation=true` restores the
old live path for comparison.

## The problem

The same seed gave the same world only if chunks were generated in the same order. The same windows generated in grid
order and in a shuffled order differed in 1,239 of 2,880 chunks (75,346 cells at 512): trees missing or cut short at
chunk borders, plants swapped, snow on a neighbour's canopy in one order and on the ground in the other, ores and
deposits, cave walls, mineshaft ceilings and spawners. A server explores in its players' order, so two servers on one
seed (or a bench and the review server) did not share their worlds.

## Rule

A chunk's population is a pure function of the terrain as provided. Where two populations reach the same cell, a
fixed rule decides it, whatever order they came in.

1. **Provided terrain** (`OrigenFeatureRuntime#baseline`): a chunk's raw terrain and the skins the provider paints on
   it, exactly as `provideChunk` builds the chunk. It is kept (an LRU of 256) when a chunk is provided, and painted the
   same way for a chunk not yet provided. The 256 provider borrows or recomputes its raw terrain with village ground
   shaping, as its workers do (`ChunkProviderBiomic#shapedRawTerrain`).
2. **Journal.** Every Biomic pass (trenches, Origen features, cave habitats, cave decor, mainland, sky, flora) reads the
   provided terrain and its own writes, never the world, and writes into the population's journal. Origen's authored
   feature overlay is the population's own, and the cross-population tree, mushroom and vine rollbacks are off. A
   chunk's authored terrain is always Origen's own copy (`createAuthoredChunk`) with the physical overlay
   (`authoredView`), never a payload's dressed companion or the per-column fallback.
3. **Apply** (`applyJournal`, bottom-up). The population's own box (its corner plus 8) wins. Outside it a write lands
   only on provided terrain, or over a neighbour's write that sorts after it (lower chunk x, then z, wins). The writer
   is recorded per cell (`PopulationTags`, saved with the chunk). A write the cell refuses (a plant on ground that
   cannot carry it) claims nothing. Support checks read the world. When a plant's ground goes to a later write, the
   plant goes, and the best write it had displaced comes back. A two-block plant lands whole or not at all.
4. **Unloaded neighbours.** Writes for a chunk that is not loaded are queued with every contender
   (`PendingFeatureData#journalOverflow`) and applied, losers first, when it loads.
5. **Structures last, in the box.** Mineshafts, villages and strongholds run after the journal. They are confined to
   the box, read the provided terrain and their own writes, and own what they write. Starts are drawn in a fixed order,
   each with its own random per box (`BoxedStructureStarts`). A mineshaft or stronghold piece is drawn only if no
   liquid touches its whole hull in the provided terrain (vanilla tested each box's slice, and the first box decided).
   A spider corridor gets one spawner at a spot chosen from the corridor alone. A written block's `onBlockAdded` (a
   torch or rail settling) and a generator's height queries (height map, top solid block) read the box, not the world. Village decoration protection comes from the village plans,
   never from the structure starts generated so far.
6. **Other mods' generators** (Forge's `IWorldGenerator` pass) run confined to the box the same way
   (`MixinChunkProviderServerForeignBox`). Inside a box a column's biome comes from the biome map, never from a loaded
   chunk (whether that chunk was loaded yet depended on timing: a Growthcraft hive came and went).

## Measurements (2026-10-23)

- Grid vs shuffled order, 3 windows of 25×25 chunks, final worlds cell by cell: **0 differing cells** at 512 and at 256
  (from 75,346 at 512).
- Same order twice, 17 windows: 0 at 512; at 256 one cell in 10,368 chunks, a Growthcraft bee hive. It is placed
  in both runs and then dropped, or not, by a block update after generation (the hive drops itself when a neighbour
  changes and it cannot stay): tick timing, outside population.
- 17 windows in grid order (the shape of player exploration) at 512, structures included: server thread 10.7 ms per
  chunk (9.0 before), all threads 31.7 ms (32.2 before). The server thread's extra is mostly painting the provided
  terrain of chunks a population reads before they are provided. A fully shuffled order costs about as much as before
  (populations then read many chunks long before they are provided).
- Unit tests: 510, no failures.

## Costs

- A feature no longer sees a neighbour's trees, so trees from neighbouring chunks can grow into each other; the rule
  decides each contested cell.
- Chunk NBT carries the population tags (16 bytes per cell a neighbour reached into) and, rarely, displaced writes.
