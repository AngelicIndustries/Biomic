# Structure generation study (2026-10-20)

User request: before any village or structure tuning, study the mods that make structures sit well on terrain
(many of them for modern Minecraft) and pick an approach that makes Biomic's structures generate cleanly on
Origen at both heights. "Even in vanilla, villages and other structures often spawn like shit."

## 1. The problem, measured

Survey of village candidates on seed 42, 30×30 village cells (one candidate per 32×32-chunk cell, about
15,000 blocks square), using the terrain queries Biomic's placement already uses:

| | 512 (Hohen Angst) | 256 |
|---|---:|---:|
| Cells | 900 | 900 |
| Candidate centre on land | 473 | 491 |
| **Passes Biomic's current rule** | **8** | **2** |
| Passes Improved Village Placement's standard rule (r64, ±10) | 4 | 4 |
| Passes its small-village rule (r40, ±10) | 30 | 33 |
| Same, tolerance scaled with relief (±15 at 512) | 9 standard, 50 small | — |
| Cells with land anywhere in the cell | 707 | 708 |
| **Flattest spot of each cell passes the small rule** | **421** | **301** |
| Flattest spot passes the standard rule | 139 | 80 |

Relief (highest minus lowest ground) under the footprint of the village vanilla would build at each land
candidate: median about 40 blocks at both heights; above 56 blocks for 198 of 473 at 512 and 195 of 491 at 256. Vanilla 1.7.10 village footprints average
6,600 blocks² (about 80×80).

Two findings:
- **Placement is the main reason villages are rare.** Biomic tests one random point per cell. Its rule allows at
  most 2 blocks (3 at 512) between ground samples 4 blocks apart, plus 8 (12) over the footprint. On Origen
  that passes in under 1% of cells (about one village every 5,000 blocks). Even Improved Village Placement's
  rules, tuned for vanilla terrain, pass under 6%: Origen is far rougher than vanilla terrain.
  Searching each cell for its flattest spot instead finds suitable ground in most cells.
- **Flat enough is never flat.** Even a passing spot has up to ±15 blocks of relief under the village, and
  1.7.10 pieces cope badly with that (section 2).

## 2. How Biomic places structures today

- **Villages:** vanilla 1.7.10 `MapGenVillage` pieces behind Biomic's admission
  (`BiomicVillagePlacement`). At population each house measures the average surface height under its own
  footprint (`getAverageGroundLevel`, from `getTopSolidOrLiquidBlock`), drops its floor there, fills
  cobblestone pillars down to the ground under its walls (`func_151554_b`) and clears blocks above itself.
  Roads take the surface block by block. On slopes that gives stilted houses on one side and houses cut into
  the hill on the other, with sheer walls where the box was cleared.
- **Mineshafts:** admitted only when each planned piece is at least 80% embedded in rock (no open floors or
  roofs); fine as is.
- **Strongholds:** vanilla layout moved into rock below the surface; fine as is.
- **Nether fortresses:** vanilla at 48–70 and up, above Tartarus' lava sea; bridges cross open caverns and
  corridors tunnel through rock, as in vanilla.

## 3. What other mods do

**Vanilla 1.18+ terrain adaptation (the Beardifier).** Structures are planned before terrain noise
(structure starts are a chunk status that precedes noise), and every piece within 12 blocks of a chunk adds a
term to the terrain density while the chunk's noise is computed. Four modes per structure: `beard_thin` (raise
ground under a piece and clear above it, villages), `beard_box` (same, boxed, ancient cities), `bury` and
`encapsulate` (strongholds, trail ruins). The term is a 24³ Gaussian kernel around the piece's bounding box,
positive below the piece's ground level and negative above, scaled by vertical distance to that level.
Because it changes density, not blocks, surface rules then cover the new ground with grass and dirt naturally.
Villages are jigsaw structures: the start is projected to the surface heightmap, houses are rigid (one height,
taken from the street junction), streets are terrain-matching (they follow the surface), and junctions get a
lighter beard.

**YUNG's API (`EnhancedTerrainAdaptation`).** The same mechanism made configurable: kernel size and falloff,
separate top action (carve or none) and bottom action (beard or none), a ground offset, per-axis padding, and
an aquifer override so carved rooms don't flood. Per piece or per structure. Its `LargeCarvedTopNoBeard`, for
example, carves above a piece and leaves the ground below untouched.
Source: [YUNG-GANG/YUNGs-API](https://github.com/YUNG-GANG/YUNGs-API), `EnhancedBeardifierHelper` and
`EnhancedTerrainAdaptation`.

**Moog's Structure Lib.** A placement gate (terrain heights at chunk centres within a radius must stay within an
allowed range), the same enhanced terrain adaptation as YUNG's, and a pillar processor (from Repurposed
Structures) that extends foundations to the ground. Its recent fix made its Beardifier chain with YUNG's
instead of switching it off. Source: [FinnSetchell/MoogsStructureLib](https://github.com/FinnSetchell/MoogsStructureLib).

**Improved Village Placement** (Lithostitched spawn conditions, data only). A standard village needs a grid of
points every 16 blocks within 64 of the start, each within ±10 of the start's surface height; otherwise a
*small* village (size 4, within 40) may spawn, with its own check (radius 40, every 20). Both keep vanilla's
`beard_thin`. Source: [Apollounknowndev/improved-village-placement](https://github.com/Apollounknowndev/improved-village-placement).

**Structurify.** Per-structure configuration, including a flatness check (terrain within a distance must stay
within a threshold). Same idea as above, as a user setting.

**Recurrent Complex (1.7.10).** Blending after placement, with no access to terrain density: blocks marked
"natural" in a template grow a randomized cloud (4 blocks out, ±6) filled with the biome's own top and filler
blocks down to the ground, and a companion "natural air" cloud carves above. The best a 1.7.10 mod could do
when terrain was generated by someone else. Source: [Ivorforce/RecurrentComplex](https://github.com/Ivorforce/RecurrentComplex),
`TransformerNatural`.

Patterns:
1. **Choose the site:** reject or search by flatness (Improved Village Placement, Structurify, Moog's).
2. **Adapt the terrain to the structure:** density terms around planned pieces (vanilla, YUNG's, Moog's).
   This is what makes modern villages look built *into* the land.
3. **Patch after placement:** pillars and natural clouds (Repurposed Structures, Recurrent Complex), for
   generators that cannot touch terrain.

## 4. What fits Biomic

Biomic generates terrain on worker threads, one chunk at a time, as a pure function of the seed (Origen
terrain, cave expansion, Abyssal Chasm, sky islands, the Tartarus Nether). That is exactly the precondition
vanilla's terrain adaptation needs: if village layouts are a pure function of the seed too, a terrain worker
can ask which village pieces lie near its chunk and shape the ground for them before anything is placed. 1.7.10
lets us do this:
- A village's layout (`MapGenVillage.Start`) needs only the seed, the cell's chosen spot and the biome manager
  (for desert or plains houses); Biomic's biome manager is pure. Whether it is safe on worker threads, and
  whether other mods' village pieces (Forge's `VillagerRegistry`) stay pure, is the first thing to verify; if
  not, plans are made on the server thread once per cell, which is cheap.
- Each house will later measure the average ground under its footprint. On untouched terrain Biomic can
  compute that number itself, from the same pure terrain, so the pad height is known in advance; after the
  terrain is shaped to it, the house measures the same value and sits flush. No stilts, no cut-outs.
- Roads keep following the surface; between pads, the beard's smooth falloff makes that surface gentle.

## 5. Recommendation

Three layers, in this order:

1. **Site search.** For each village cell, sample a coarse grid of candidate spots (the survey used 6×6) and take
   the flattest by ground relief over the village area, on land, away from rivers and the sea. Graded size:
   a standard village where the spot passes a standard check, a small village (vanilla's pieces, layout radius
   capped near 40) where only the small check passes, none otherwise. Tolerances scale with relief (1.5× at
   512). Expected: villages in about 47% of cells at 512 (15% standard) and 33% at 256 (9% standard), against
   under 1% now. The 256 ground query is about ten times slower than the 512 one (the survey took 17 minutes
   against 1.7), so the search needs a cheaper query there: the 512 path's column search over Origen's authored density,
   mapped through the 256 vertical transform.
2. **Plan on the workers.** A pure, cached village plan per cell: the layout from vanilla's piece generator, and
   each house's pad height from the average pure ground under it. The server thread's `MapGenVillage` asks
   the same plan, so placement and terrain always agree.
3. **Terrain adaptation in the terrain workers.** Vanilla's `beard_thin` kernel applied to Origen's terrain for
   every planned house within 12 blocks of the chunk: ground raised to the pad underneath, cleared above, with
   the Gaussian falloff blending into the surrounding slope; a lighter term at road junctions. Applied in
   density where Origen's raster allows it (the surface palette then covers the new ground), otherwise in
   materials with the column's own top and filler blocks.

Then the same machinery covers other surface structures when they come (desert and jungle temples, witch huts),
and YUNG-style carve-only adaptation fits any later underground ones.

Not recommended: placement-only fixes (they keep villages rare on Origen and leave the ±15 blocks of relief to
1.7.10's stilts), or Recurrent Complex-style patching after placement (it runs on the server thread, cannot
reach unloaded neighbours, and needs templates marked up for it).

Cost: the search runs once per village cell and caches; the plan once per cell; the beard only in chunks within
12 blocks of a planned house. Nothing new on the server thread except reading cached plans.

## 6. Questions for the user

- Village frequency: one village candidate per 32×32-chunk cell is vanilla's spacing. With the search, villages
  appear in most cells that have usable land. Is that the density you want, or sparser?
- Small villages (fewer houses) where the land only fits a small one: wanted?
- Should the same terrain adaptation also apply to Nether fortresses later (bridges blended into cavern
  walls), or keep them vanilla?

## 7. Decisions (user, 2026-10-20) and implementation order

- Go ahead with the recommendation.
- Structure frequencies are configurable in a JSON file, per structure type.
- Small villages: yes.
- Nether fortresses belong outside the terrain, in the open caverns, not tunnelling through rock.
- Village Names integration, designed in before implementation. Village Names (GTNH fork, 4.5.19-GTNH) swaps
  in its own village generator, with 1.14-style buildings, names, signs and random village sizes, through
  Forge's `InitMapGenEvent`. Biomic builds its village generator directly, so today Village Names villages never
  appear in Biomic worlds. With Village Names installed and its new generator enabled, Biomic builds each
  planned village with Village Names' `Start` and size rules (its random sizes give the small villages), and
  keeps its own site search, frequency and terrain shaping. Village Names pieces set their floor at the median
  ground height under the piece, so pre-shaped ground works for them as for vanilla's average.

Order:
1. Village Names as an optional dependency (compile against it, load it only when present).
2. `structures.json`: frequency and size settings per structure type.
3. Village plans: per-cell site search, graded size, vanilla or Village Names layout, cached and shared by
   placement and terrain.
4. Terrain shaping for planned villages in the terrain workers, at 512 then 256.
5. Fortresses placed in the Tartarus Nether's open caverns.


Added during implementation (user): Village Names villages must match the biomes they spawn in, taking 3D biomes
into account, and villages also generate on sky terrain.

## 8. What was built (2026-10-20)

**Configuration.** `config/biomic-structures.json`: per structure type `enabled` plus its frequency (villages:
`spacing_chunks` 32, `separation_chunks` 8, `chance` 1.0, `small_villages`, `sky_villages`, `village_names`;
mineshafts: `chance_per_chunk` 0.004; strongholds: `count`, `distance`, `spread`; Nether fortresses:
`spacing_chunks` 16, `chance` 1/3). Written with these defaults when missing; out-of-range values fail at startup.

**Plans** (`BiomicVillagePlacement`), pure and cached, made on the terrain workers and read by the server thread:
- Ground: one per village cell. The flattest land on a 6×6 grid of the cell's range; standard if every point within
  64 (every 16) is within ±10 of the start (±15 at 512), small if the same holds within 40 (every 20).
- Sky: one per floating-island cluster (clusters lie hundreds of blocks apart, at most one per 704-block region). A
  24-block grid over the cluster on cheap island-top estimates, scored by how much of a 48-block disc stays on the
  island, refined around the best hits and confirmed on the exact island layer; checks are discs (ring bands run
  diagonally) with 1.5× the tolerance (island tops are domed). Pieces the island cannot carry are dropped; a sky
  village keeps at least three houses.
- Layouts: vanilla's pieces or Village Names', capped at 56 blocks from the well (20 for small villages); vanilla's
  house limits usually stop it within about 50, Village Names' do not. Forge's village handlers run in class-name
  order, so a seed lays out the same village in every session.

**Terrain** (`VillageTerrain`): one height field per village, a pure function of its plan, applied on the terrain
workers at both heights before anything reads the chunk (at 512 Origen's intrinsic check owns the changed cells):
- Under each house the ground is its pad (the mean ground under it, widened by 6). Around houses it blends toward
  the weighted mean of nearby pads over about twice the height difference, into the natural ground smoothed over
  the village and fading out 8 blocks past it.
- The road network is a set of parts, each at one height: every cross-section of a road, every piece at a road
  (plaza, houses, street decor) with a one-block apron at its floor, every lamp or arch with the road it spans. Their
  heights are solved together (`RoadNetworkHeights`, an exact minimum cut): each part close to the ground its cells
  want, every height change between neighbours costing 2 per block of shared edge (8 times that between a piece and
  its road, so houses open level onto their road), never more than a block between neighbours, diagonals included.
  A ridge or notch shorter than about four cross-sections costs more than it fits, so none is left; slopes the
  ground holds are kept in single steps. Over water roads are causeways.
- Ground within 6 of a road stays within a block per block of the road; pits up to 3 deep inside the village are
  filled to where they would spill.
- Pieces other than roads keep a block apart in the layout, so no lamp post or wall is glued to a house (about 8%
  fewer pieces per village).
Island decoration keeps 16 blocks clear of sky villages, and mainland decoration of ground villages.

**Population** (`VillageContext`): while Biomic places a village's pieces, vanilla's (and bugtorch's) and Village
Names' ground queries look only in the village's layer: below the islands for a ground village, on the island
(nothing off its edge) for a sky village.

**Style** (`VillageStyle`), from the village's own site in its 3D biome (a sky village's island habitat):
- Evidence: an Origen province's ground palette sampled at the village's height around its start (with name hints
  for badlands and deserts) and its tree features; an RU habitat's surface style, climate, tags and the logs of its
  trees; or a native biome's top block, climate, dictionary types and name.
- Rule: snow or cold gives taiga (spruce, or the forest's wood) when forested, otherwise snowy; swamp; badlands
  (mesa) and sand deserts; jungle; savanna (acacia, or hot and dry); spruce forests give taiga; mycelium mushroom;
  otherwise plains in the commonest wood (oak, birch, dark oak, jungle).
- Village Names builds the layout with this style: its biome mapping is answered per village, its config list is
  untouched. Vanilla layouts in a desert style are built as vanilla's desert (sandstone).

Seed 42 examples at 512: Saguaro Desert desert/sand, Snowy Mountains snowy, Sludgepit and Fossilized Fenlands swamp,
Lavender Fields plains/oak, Bamboo Forest plains/jungle; sky villages on Maple Forest, Clover Plains and Silver Birch
islands (plains, oak or birch) and a Taiga island (taiga/spruce); at 256 a Redwoods ring (taiga/spruce).

**Cost**, 512 default window (seed 42, 25×25): server thread 7.9–8.4 ms/chunk against 6.8 before, total 30.7–31.1
against 28.8; the difference is planning and populating villages that now exist. A sky cluster's plan takes about
0.5 s at 512 and 1 s at 256, once, usually on a worker.

**Fortresses** (`TartarusFortresses`): vanilla's layouts and region placement (one region per `spacing_chunks`²,
`chance` of them, the start in the region's middle half), lifted to the height where the largest share of the
pieces' volume is open air in the pure Tartarus terrain (each piece judged on its centre column, weighted by its
footprint; between 40, above the lava sea at 32, and 24 below the top). Seed 42's first fortress: 66% open at
vanilla's 48, 96% at 99. Planned per region by the terrain workers.
