# BOP curation: complement RU + Origen, do not duplicate them

Decision implemented September 17, 2026, at the user's request to cut the repetitive
BOP review and remove redundant biomes. **Keep 11 of 75 Overworld profiles and all
6 Nether profiles. Cut 64 of 81 from default new-world generation.**

User correction on September 17: aggressive pruning applies only to the Overworld.
The initial three Nether cuts are reversed; thematic overlap is acceptable there
because Nether variety is more limited.
Lavender Fields is also retained by explicit user preference, despite its overlap
with the flower-meadow role. This is an intentional exception to the strict pruning rule.

This is an editorial judgement about the combined Biomic world, not a claim that
two mods' biomes are block-identical or that BOP's implementations are worse.
Keep a distinct landscape experience, ecology, material or feature at walking
distance. A different tree texture, grass tint, flower palette or tree density
alone does not justify another province. Judge the hosted implementation we have,
not a dramatic native terrain shape that Origen hosting does not reproduce.

## Keep: Overworld

| BOP key | What earns its place | Closest overlap and distinction |
| --- | --- | --- |
| `dead_swamp` | Sparse standing dead trees over wet ground; a drowned/dead wetland. | RU Bayou/Fen and Origen Murky Marshlands are predominantly living wetlands. Prefer one dead wetland over also keeping Dead Forest. |
| `garden` | Giant flowers replace some tree forms, with dense low ground cover. | RU Flower Fields/Poppy Fields are ordinary-sized flower meadows. The giant-flower silhouette is the reason, not a new flower colour. |
| `kelp_forest` | Concentrated underwater kelp-column habitat. | RU Hyacinth Deeps/Rocky Reef have different aquatic forms; keep this underwater forest instead of another coral reef. Origen aquatic plants do not by themselves make a dedicated kelp biome. |
| `lavender_fields` | Expansive lavender-coloured flower landscape; explicitly retained by the user. | Overlaps RU Flower Fields/Poppy Fields in ecological role, but the user wants this visual variant. |
| `mystic_grove` | Mixed magical grove: native unusual trees, flower vines, rich ground/water plants and poison pools. | RU Wisteria/Magnolia overlap the colourful-forest role, but not this combined fairy-grove ecology. This is the softer fantasy counterpart to Ominous Woods. |
| `ominous_woods` | Thorn/poison-ivy/poison-pool forest with distinctive native dark trees. | RU Blackwood Taiga and Origen Black Forest cover darkness; the hostile undergrowth/materials, not just darkness, justify this one. Native mob spawns are not promised by the habitat adapter. |
| `sacred_springs` | Rare enormous sacred oaks with shrub/lily-rich wet forest around them. | RU Redwoods and Origen Tall Timberland provide giant conifers/forest, not these sprawling sacred oaks. Pinned BOP 1.7.10 does **not** supply a dedicated hot-spring generator here. |
| `silkglades` | Cobweb nests and web-draped swamp trees. | No equivalent web-centred RU/Origen surface habitat. Retains its genuine Sludgepit parent relationship. |
| `sludgepit` | Dense dirty/poisonous bog, and required native parent for Silkglades. | Some overlap with RU Fen/Murky Marshlands is accepted to retain the toxic-bog/webbed-subregion pair without inventing a new parent mapping. Quagmire is unnecessary. |
| `volcano` | Volcanic ocean-island habitat and ash/lava contrast. | RU Ashen Woodland is an ashen forest, not an ocean volcanic island. Keep the ocean-island role; do not advertise an unimplemented native volcano shape. |
| `wasteland` | Dried-dirt barren terrain, native rock formations/piles, dead growth and poisonous pools. | Distinct from living RU Outback/Joshua Desert and Origen desert/badlands. A genuinely blasted landscape, not another scrub palette. |

The tightest judgement calls are Mystic Grove and Ominous Woods: retained for
their combined feature/ecology differences, not merely their colours. This list
is deliberately small, but it is still a design choice rather than an objective
similarity score. No new biome-specific mobs or structure/ore passes are implied.

## Cut: Overworld

Each row names the existing visual/ecological role to use instead. These are
comparators, **not registry aliases, automatic block replacements or migrations**.

| BOP key | Existing role / reason to cut |
| --- | --- |
| `alps` | RU Icy Heights and Origen Snowy Mountains already provide large snowy relief. |
| `alps_forest` | RU Pine Slopes/Frozen Pine Taiga on Origen mountain landforms. |
| `arctic` | RU Tundra/Icy Heights and Origen frozen landscapes. |
| `bamboo_forest` | RU Bamboo Forest and Origen Bamboo Basin. |
| `bayou` | RU Bayou/Old Growth Bayou. |
| `bog` | RU Fen/Marsh and Origen Murky Marshlands; keep Sludgepit as the unusual bog. |
| `boreal_forest` | RU Boreal Taiga and its old-growth/cold variants. |
| `brushland` | RU Dry Bushland/Shrubland. |
| `canyon` | Origen Badlands Balconies and other authored warm rock landforms supply the spectacle. |
| `canyon_ravine` | Same canyon role; no extra sub-biome needed once Canyon is cut. |
| `chaparral` | RU Dry Bushland/Shrubland plus warm Origen hosts. |
| `cherry_blossom_grove` | RU Magnolia/Wisteria cover the flowering-grove experience; another blossom canopy is insufficient. BOP cherry blocks remain available. |
| `coniferous_forest` | RU Pine Taiga/Boreal Taiga/Redwoods. |
| `coral_reef` | Origen Coral Ocean already owns a coral habitat; RU Rocky Reef adds another distinct reef. Keep BOP Kelp Forest instead. |
| `crag` | Origen Rocky Refuge/Vertical Vistas and RU Towering Cliffs supply bare dramatic rock; crag-rock texture alone is insufficient. |
| `dead_forest` | Choose Dead Swamp for the dead-tree motif; RU Ashen Woodland covers another damaged-looking forest. |
| `deciduous_forest` | RU Old Growth Forest/Maple/Silver Birch and vanilla forest blends. |
| `dense_forest` | RU Old Growth Forest; density alone is not another biome. |
| `eucalyptus_forest` | RU Eucalyptus Forest. |
| `fen` | RU Fen. |
| `flower_field` | RU Flower Fields/Poppy Fields. |
| `frost_forest` | RU Frozen Pine Taiga and Origen frozen environments; pale foliage is not a new regional role. |
| `fungi_forest` | Origen Scarlet Sanctuary/Frozen Fungi and RU Fungal Fen already cover major mushroom environments. |
| `glacier` | RU Icy Heights plus Origen ice/snow relief; dependent Arctic variant. |
| `grassland` | RU Grassland/Clover Plains. |
| `grove` | RU/vanilla mixed woodland and Origen Flowering Forest. |
| `heathland` | RU Shrubland/Highland Fields; small plant changes can be flora choices. |
| `highland` | RU Highland Fields/Pine Slopes over Origen uplands. |
| `jade_cliffs` | RU Pine Slopes on Origen Rocky Refuge/Vertical Vistas supplies pine-covered cliffs. Native BOP height alone is not a hosted distinction. |
| `land_of_lakes` | Existing wetland and lake-bearing Origen foundations with RU vegetation; another wet-forest province is low value. |
| `land_of_lakes_marsh` | RU Marsh/Fen and Salty Marsh; dependent variant of the cut lake forest. |
| `lush_desert` | RU Joshua Desert/Dry Bushland on warm Origen hosts cover vegetated drylands. |
| `lush_swamp` | RU Bayou/Fen and Origen Mangrove Swamp/Canopy Cascades. |
| `mangrove` | Origen Mangrove Swamp already supplies mangrove ecology. Native ocean-parent distinction alone is insufficient. |
| `maple_woods` | RU Maple Forest/Autumnal Maple Forest. |
| `marsh` | RU Marsh and separately integrated Salty Marsh. |
| `meadow` | RU Grassland/Clover Plains/Highland Fields. |
| `meadow_forest` | Meadow/woodland transitions using the retained RU palette; dependent variant. |
| `moor` | RU Fen/Highland Fields cover open wet/upland vegetation; no strongly distinct hosted structure. |
| `mountain` | Origen mountain/cliff terrain and RU mountain vegetation. |
| `oasis` | Keep the idea as a future local desert feature, not the current oversized grass province; vegetated dryland pockets already occur in RU Joshua Desert. No new oasis feature is implemented in this change. |
| `orchard` | RU Orchard plus the existing optional fruit-provider integration; do not duplicate orchard providers. |
| `origin_valley` | RU Alpha Grove already supplies the nostalgic Alpha role. Origen is a different project, not this biome. |
| `outback` | RU Outback. |
| `prairie` | RU Prairie. |
| `quagmire` | Sludgepit is retained; another muddy sub-variant adds little. |
| `rainforest` | RU Rainforest/Sparse Rainforest and Origen Canopy Cascades. |
| `redwood_forest` | RU Redwoods/Sparse Redwoods; retain the approved Gloomy Gorge + RU Redwoods blend. |
| `scrubland` | RU Dry Bushland/Shrubland/Baobab Savanna. |
| `seasonal_forest` | RU Autumnal Maple/Maple/Silver Birch. |
| `seasonal_forest_clearing` | Clearing/density variant of a role already supplied by RU. |
| `shield` | RU conifers on Origen Rocky Refuge/Gloomy Gorge supply rocky wooded country. |
| `shrubland` | RU Shrubland. |
| `snowy_coniferous_forest` | RU Frozen Pine Taiga/Cold Boreal Taiga. |
| `spruce_woods` | RU Pine Taiga and vanilla taiga/forest blends. |
| `steppe` | RU Grassland/Prairie/Dry Bushland; open dry grass need not get another province. |
| `temperate_rainforest` | RU Redwoods/Old Growth Forest on lush Origen terrain. |
| `thicket` | RU Shrubland and wooded transition vegetation; density difference only. |
| `tropical_rainforest` | RU Rainforest/Tropics. |
| `tropics` | RU Tropics and Origen Watery Wilds; another tropical ocean island palette is redundant. |
| `tundra` | RU Tundra. |
| `wetland` | RU Fen/Bayou/Marsh and Origen wetlands. |
| `woodland` | RU/vanilla woodland and Origen Flowering Forest. |
| `xeric_shrubland` | RU Dry Bushland/Joshua Desert/Saguaro Desert. |

## Nether: keep all six

| BOP key | Decision | Reason |
| --- | --- | --- |
| `boneyard` | Keep | Bone spines rising from floors and hanging from ceilings. Origen surface fossils do not replace a Nether bone environment. |
| `polar_chasm` | Keep | Frozen hard-ice Nether habitat; none of the four integrated RU Nether habitats fills that role. |
| `visceral_heap` | Keep | Flesh terrain and blood pools; genuinely different materials/environment. |
| `corrupted_sands` | Keep | Soul-sand/thorn landscape; not a direct RU duplicate. Initial cut reversed. |
| `phantasmagoric_inferno` | Keep | Ash/fire/lava theme overlaps RU Infernal Holt, but its native ecology still adds welcome Nether variety. Initial cut reversed. |
| `undergarden` | Keep | Lush/fungal theme overlaps RU Mycotoxic Undergrowth/Glistering Meadow, but retain its native overgrown environment as another Nether option. Initial cut reversed. |

BOP's End biome was never integrated and is outside this 81-profile selection.
Salt Marsh is independent and remains enabled. Origen-only generation is untouched.

## How the cut is applied

- `BopBiomeSelection` is the explicit allowlist. Native availability is intersected
  with it; absent mods and native-disabled profiles are never enabled by the filter.
- `expansion.bopFullCatalogue=false` is the default for **new `biomic` saves**.
  Set it to `true` before creating an exhaustive audit world to restore all available
  supported Overworld profiles. All six available Nether profiles remain selected
  in either mode. `bopHabitats` and `bopNetherHabitats` remain independent master gates.
- Selection is frozen through the existing per-save surface/Nether manifests,
  fingerprinted from only the selected native bindings. No schema changes.
- Mainland, landmark and sky-island/donut choices consume that saved selection.
  Removing a biome from the initial selection therefore also removes its future
  blend/sky occurrences without globally altering the selector used by old worlds.
- Registrations, block/item IDs, adapters, tests and old hosted aliases remain.
  Axed means **not selected by default**, not deleting blocks out of someone's save.
- Existing saves retain their old selections, even for newly generated chunks.
  No seam-producing mid-world migration, regeneration or silent registry replacement.
- Native `BIOMESOP` reference generation is untouched. BOP remains optional.
- The live 82-stop paired tour is deliberately **not** converted/restarted by this
  change. Use the 17-stop retained-biome tour in those
  same worlds. Its coordinates were verified during September 16 preparation;
  this change does not claim a fresh visual or block-parity pass.

This reduces default content duplication and review scope, not necessarily tick
time: retained native tree/features still cost work. It is not a performance fix.

## Evidence and validation

Decisions use the actual pinned BOP **1.7.10-2.1.0.2308** source recovered under
`run/bop-native-source-LqlEkO/decompiled/biomesoplenty/common/biome/`, its current
Biomic feature bindings in `BopNativeBiomes` / `BopNativeNetherBiomes`, the 55-entry
RU catalogue, and `OrigenBiomeProfile` plus the
existing hosted-habitat pools. In particular the source's giant flowers, sacred
oak, cobweb nests, poisonous pools and Nether soils are concrete feature differences.
Modern BOP screenshots and biome names alone were not used to infer 1.7 features.
Upstream project identities: [BOP](https://github.com/Glitchfiend/BiomesOPlenty),
[RU](https://github.com/UHQ-GAMES-MODS/RegionsUnexplored); local pinned sources,
not their current branches, are authoritative for this selection.

Tests cover exact keep counts, installed-profile intersection, missing parents,
deterministic ordering, immutable results, full-catalogue opt-in, unchanged old
75/6 manifest round trips, retained Salty selection, and actual pure habitat/sky/Nether
selectors. These are automated policy/storage checks, not a new ingame approval.

Initial curation verification: **40 tests, zero failures/errors** in
`run/bop-curation-tests-sep17.log`. A separate catalogue check confirms this matrix
accounts for all 81 supported BOP profiles exactly once. All 17 short-tour coordinate
pairs match the saved-world verification report from September 16. No servers were
restarted and no existing world was modified during curation.

After restoring all six Nether biomes and Lavender Fields: **41 tests pass** in
`run/bop-curation-final-tests-sep17.log`. The 17-stop tour and complete 81-profile
decision matrix were rechecked; all coordinate pairs match the original saved evidence.
