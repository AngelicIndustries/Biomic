# Vanilla structures in Biomic

## Built (2026-10-23)

Approved list, all built at 512 and 256 (`BiomicScatteredFeatures`), drawn last in each chunk's population box from
the terrain as provided, so they do not depend on chunk order:

| Structure | How |
|---|---|
| Desert temple, jungle temple, witch hut | Vanilla's pieces. One candidate chunk per 32-chunk region, kept where its habitat is a desert, a jungle or a swamp and the ground under the whole footprint is flat enough; set at the real surface (a hut at the waterline), on the ground or on a floating island. Their height comes from the plan, never from the first box that draws them. |
| Igloo | Biomic's own layout (`BiomicIgloo`): snow dome, entrance tunnel, packed-ice windows, bed, furnace, workbench, chest. In snowy habitats. |
| Ruined portal | Biomic's own layout (`BiomicRuinedPortal`): broken obsidian frame with crying obsidian, a netherrack and magma splash, fallen pieces, a gold block now and then, a chest. One candidate per 40-chunk region, on any dry land. |
| Dungeon | Vanilla's generator, eight tries per chunk anywhere between the floor and a few blocks under the surface (island keels included). |
| Desert well | Vanilla's, 1 in 1,000 desert chunks. |
| Fossil | Removed (review, 2026-10-23: "bones structures ... are ugly, remove them altogether, except from the nether"), with Origen's rib cages and sea-floor bones and the hosted habitats' vanilla fossils. The Nether keeps Tartarus' rib cages. |

**On floating islands** (2026-10-23, review: "I also want to see all structures in a floating setting, generated
naturally"): two island clusters in three hold one structure, at the cluster's best spot. A 16-block grid over the
cluster is read on the islands' estimated tops; a spot holds the kind its island's habitat names (a ruined portal where
none does) when the estimates keep its footprint on the island, and the eight flattest are confirmed on the islands'
exact columns, as on the ground: the whole footprint on island columns within the kind's relief, its corners and
centre on islands of that kind's habitat (a cluster's islands may each have their own). Islands take four more
habitats so every kind has islands of its own: RU's saguaro desert, bayou and rainforest, and vanilla's snowy plains
(RU's tundra at first; see below). An island at least
22 blocks thick holds Et Futurum's amethyst geode in its core, one island box in 20.

Seed 42 within about 6,000 blocks of spawn: on islands 7 desert temples, 11 igloos, 3 jungle temples, 10 witch huts
and 60 ruined portals; on the ground 7, 28, 1, 31 and 240 (flat rainforest is rare on the ground).

**After the first review of them** (2026-10-23):
- No structure stands within 8 blocks of a village, on the ground or in the sky ("a broken portal inside a village,
  this makes no sense"; two witch huts and a desert temple had landed in sky villages). An island cluster then takes its
  next-best spot.
- One habitat per island ("Mixed biomes on a single island look quite bad"): an archipelago's habitats were chosen per
  144-block cell of sky, so an island across a cell border had two. Each island is now its own connected land (the
  cluster's coast plan on a 4-block lattice, flood-filled once per island and remembered) and chooses once.
- Igloos only where snow lies: RU's tundra is cold but dry, so its islands had no snow. The snowy islands are now
  vanilla's snowy plains; igloos keep to snowy habitats.
- Snow is never kept off a structure's surroundings (a portal had left a bare square in a snowfield), and a portal in a
  snowy habitat has snow on its blocks.
- Trees keep clear of structures as a whole: a tree rooted outside a structure's margin that would reach into its
  footprint is not grown (an Origen tree, a hosted one, BoP's native ones and the fruit trees alike); before, only its
  root was checked, and BoP's native trees and the fruit trees not at all.
- Island geodes are whole: EFR's geode grows from its origin towards +x and +z and is wider than a population box, so
  each box it reaches now draws it from its own seed and keeps its part (before, the box clipped two of its sides).

**Second review** (2026-10-23 night): portals follow each column's ground and are snowed on only on their own blocks;
structures lay still water (a fountain half-built at the edge of the explored area spilled); swamp islands have pools
and swamp olive; snowy islands snow; mineshafts that cross caves stand on fence posts down to the cave floor.

**Mineshafts in rock pockets** (2026-10-24): caves leave solid pockets of rock at mineshaft depth (CavePockets), and
a mineshaft starts only at a pocket's centre, so it runs through rock instead of across caverns.

Habitats are read from Biomic's habitat keys (`origen:DESERT`, `regions_unexplored:fen`, island habitats), not from
biome objects: those carry no useful type. Decoration stays off a planned footprint (the hosted habitats' guard and
Origen's surface protection read the plans). Mineshafts are sunk whole, when they are planned, until every piece lies at
least 12 blocks under the ground of the columns it spans (vanilla tops them out ten blocks under the sea, so valleys and
canyon floors laid them open); one that would reach the floor is not built. No piece is cut away.

## Original proposal (2026-10-22)

Plan item (2026-10-21): "Vanilla structures: list them all; after approval, spawn every one naturally." Inventory taken
2026-10-22 from the code and from 3,000 chunks of the 512 bench world (seed 42).

## What generates today

| Structure | Source | Status in Biomic |
|---|---|---|
| Village | vanilla 1.7.10, Village Names layouts | Yes. Biomic placement and terrain fitting at both heights, sky villages, styles from the 3D biome |
| Mineshaft | vanilla 1.7.10 | Yes. Raised onto shifted terrain, kept inside rock (`MineshaftRockEnvelope`) |
| Stronghold | vanilla 1.7.10 | Yes. Raised by 128 at 512 |
| Nether fortress | vanilla 1.7.10 | Yes. Lifted into the Tartarus Nether's open caverns |
| Ocean monument | Village Names (a 1.8 backport) | Yes. Raised with the sea at 512 |
| Amethyst geode | Et Futurum | Yes (3,047 amethyst blocks in 3,000 chunks) |

## What does not generate

| Structure | Source available | Notes |
|---|---|---|
| Desert temple | vanilla 1.7.10 `MapGenScatteredFeature` | Not wired into Biomic's providers |
| Jungle temple | vanilla 1.7.10 `MapGenScatteredFeature` | Not wired |
| Witch hut | vanilla 1.7.10 `MapGenScatteredFeature` | Not wired; needs a swamp biome list that includes Biomic's and RU's swamps |
| Dungeon | vanilla 1.7.10 `WorldGenDungeons` | Not run (population reports 0 ms for dungeons); at 512 they must sit in cave air below the surface, not in the vanilla y band |
| Desert well | vanilla 1.7.10 `WorldGenDesertWells` | Not run |
| Fossil | Et Futurum `WorldGenFossil` | None in the bench world (0 bone blocks); EFR's generator does not reach Biomic's terrain |
| Nether fossil | Et Futurum `WorldGenNetherFossil` | Suppressed with EFR's other Nether generators in Tartarus; Soul Valley already has Tartarus' rib cages |
| Igloo, shipwreck, ocean ruin, ruined portal, pillager outpost, woodland mansion, trail ruins, ancient city | none in the stack | Not in 1.7.10 or any installed mod. Mojang's designs are all rights reserved, so they would be Biomic's own reimplementations, not copies |

## Proposal

Each structure goes through Biomic's structure terrain (`StructureTerrain`, the approach the structure study chose):
it starts only where its footprint fits the ground, at its biome's real surface at both heights, keeps decoration off
its footprint, and is deterministic per region like villages.

1. **Desert temple, jungle temple, witch hut** (vanilla `MapGenScatteredFeature` layouts): start in deserts, jungles
   and swamps by the 3D biome (Origen profiles and hosted habitats), on ground flat enough for the footprint; the
   jungle temple may cut into a slope as vanilla's does. Effort: about 1 day.
2. **Dungeons**: in cave air with a solid floor and roof, at any depth below the surface, at vanilla's rate per chunk.
   Effort: half a day.
3. **Desert wells**: on desert sand, rare, as vanilla. Effort: an hour.
4. **Fossils** (EFR's): wire EFR's generator to Biomic's rock at both heights (buried, and sometimes exposed in canyon
   walls as in 1.17+). Effort: half a day.
5. **On floating islands** (already approved for later): desert temple and well on desert islands, jungle temple on
   jungle islands, witch hut on swampy islands with a pond, a dungeon or fossil inside the keel. Igloos and ruined
   portals only if (6) is approved.
6. **Optional, needs your decision**: Biomic's own igloos and ruined portals (simple, recognisable layouts written for
   Biomic, not copied). Shipwrecks, ocean ruins, outposts, mansions, trail ruins and ancient cities are larger jobs
   and not proposed.

Nothing here is built until you approve the list.
