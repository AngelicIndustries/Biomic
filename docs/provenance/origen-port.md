# Origen port provenance and profile manifest

Biomic generator version 68 executes terrain adapted from [Rearth/Origen](https://github.com/Rearth/Origen) 1.0.0 at
revision `c01cd79717ed1c1172f615ebedd90db37119ff25`. The root license is CC BY 4.0 and `pack.yml`
credits Astrash, Sancires, Aureus, and Rearth. This is a modified Forge 1.7.10 adaptation and is not
endorsed by the upstream authors.

The audited source contains 810 tracked files and 48 production variants under
`biomes/rearth/variants`. Every one has a stable `biomic:origen/*` identity. The complete resolved
pipeline also references 37 inherited vanilla-style, river, coast, ocean, and cave profiles, for 85
runtime profiles total. No referenced density profile is omitted. Regions Unexplored-derived content
overlays are enabled by default again after the terrain-geometry comparison passed.

## Height and engine changes

- Origen's complete authored Y=-64..319 range is mapped monotonically into Minecraft 1.7.10's
  Y=0..255 range. Since generator version 37, upstream sea level 62 is pinned to Biomic sea level 63, the
  visually and structurally busy authored Y=62..158 band at an exact one-block translation, and
  applies a tangent quadratic squeeze above it. The required compression is consequently concentrated
  in deep rock and rare upper-altitude space instead of flattening every visible landform uniformly.
  Integer physical-Y inverse samples are cached, so the curved projection adds no square-root work to
  the hot per-block density path.
- Feature selection, collision checks, and recursive TerraScript execution remain in the exact
  384-high authored space. Accepted writes are projected through the same transform as terrain only at
  the Forge-world boundary; this prevents the projected world from changing later upstream feature
  decisions and keeps surface structures aligned with the protected one-to-one band. Where upper-world
  compression folds neighboring authored layers into one physical voxel, structural wood wins over foliage
  from the sibling layer; this prevents compressed tree crowns from erasing their own branches.
- `tools/import_origen_terrain.py` resolves the pinned pack's inheritance, YAML splices, global
  functions, samplers, biome tags, blend settings, elevation samplers, and three extrusion stages
  into a generated runtime manifest. It currently contains 85 profiles and 75 unique base-density
  graphs.
- `tools/compare_origen_volume.py` reads modern and legacy Anvil formats without third-party Python modules. It maps
  FML registry IDs and 1.7 metadata back to compatibility-normalized modern states and compares every voxel in
  selected chunks. Its default exact mode covers physical Y=64..159, where authored Y=63..158 has a one-to-one
  translation. The explicit `lower` projection compares physical Y=0..63 with authored Y=-64,-62,...,62 and can
  reconstruct Biomic's pre-population volume from a fresh-decoration audit; it must be paired with a modern
  population-stage-disabled reference because skipped odd authored feature writes intentionally collide into nearby
  physical cells. Ore exclusion recognizes both ordinary registry names and legacy unknown-state names carrying a
  trailing `#metadata`, so EFR deepslate ores cannot be misreported as terrain drift.
- The disposable-server `capture` suite accepts an arbitrary seed-42 center and radius through Gradle properties.
  New profile audits can therefore produce the complete terrain, point, block, state, and summary evidence set
  without adding a temporary hard-coded server mode.
- Biomic executes Terra 6.4.2's sampler semantics with a small attributed MIT-licensed sampler
  subset and the same Paralithic expression engine version. Density uses Terra's native 4x4x4
  lattice interpolation, per-biome blend distance/step/weight, and radius-four elevation smoothing.
- Generator v66 also reproduces Terra 6.4.2's stateful chunk-local lazy carver interpolation. A carver
  cache node is evaluated at the first positive-density voxel which requests it, rather than eagerly at
  its nominal 4x2x4 lattice coordinate. This observable traversal-order behavior matters in sparse upper
  terrain and removes a seven-block Secluded Valley terrain sliver without approximating its density graph.
- Generator v67 makes the lower squeeze palette-aware rather than merely density-aware. For each column, Biomic now
  resolves Origen's complete authored material stack before selecting the even authored layers represented by
  physical Y=0..63. Deep palette runs therefore count skipped solid layers exactly instead of becoming half as thick.
  EFR dripstone is now a validated hard-dependency feature: disabling `enableDripstone` fails bootstrap rather than
  silently turning Abyssal Alleys' dripstone backing into air.
- Generator v68 makes TerraScript `MATCH_SET` predicates state-aware. The earlier compatibility layer reduced each
  entry to its legacy `Block`, so a request for ordinary sand also accepted red sand because both are metadata states
  of `minecraft:sand` in 1.7.10. Biomic now compares the resolved block and metadata together, matching modern
  block-state semantics. This removes false Sandy Splits cactus anchors and the false Secluded Valley giant
  glow-berry stream; a dedicated regression test covers both a one-state set and an explicitly multi-state set.
- Generator v69 begins the final in-game artifact-polish pass while keeping the imported Terra graph as its immutable
  oracle. It removes five complete uncarved upper-terrain crumbs (component sizes 1, 2, 7, 11, and 11) emitted by
  Overgrown Cliffs/Watery Wilds density near `(5772, -1312)` and `(-5264, -4152)` before projection or population,
  so dependent foliage cannot survive the removal. The visually Marine-adjacent seven-block column is profile-owned
  by Overgrown Cliffs, so genuine Marine Monoliths terrain is not admitted to the filter. It also applies a
  pair-scoped coarse-grid majority rule at the Frosty Fingers/Murky
  Marshlands boundary near `(-6948, 3725)`, replacing Origen's rectangular three-by-one marsh finger with a tapered
  snowy join while retaining the main marsh. Focused fresh seed-42 captures and regressions pin both deliberate
  upstream divergences; focused populated captures pin all 32 removed terrain cells and the complete
  fresh-decoration gate remains the global compatibility guard.
- Generator v70 supplies Sakura Streams' missing visual compatibility rather than trying to reproduce modern
  biome-tinted jungle foliage globally. Origen's `LARGE_LEAF_VINES` and `CROSSING_VINES` streams remain exact jungle
  leaves in authored space and in the Paper parity trace, but their physical writes become EFR cherry leaves only
  when the owning 3D profile is Sakura Streams. The reviewed seed-42 cell at `(29169, 101, -31928)` is pinned as
  persistent `etfuturum:leaves:5`; other profiles and uncolored/cave vine streams remain unchanged.
- Lush Loops adds one intentional boundary-polish rule beyond upstream Terra: each visible analytic
  sine lobe is admitted as a complete feature. Its full four-block density footprint plus Terra's
  twelve-block blending stencil may contain `LUSH_LOOPS` and `LOOPS_RIVER`; if it reaches any other
  profile, the whole lobe uses a compiled copy of the same density graph with only `arches` replaced
  by its authored `-3` floor. Admission samples the authored asymmetric `archMask(z, 0)` call in that
  exact argument order and applies before surface/decorative locators, so rejected terrain cannot leave
  trees, leaves, vines, lichen, or other dependent residue. Interior loops, ground noise, rivers, and
  forced-profile parity remain unchanged, while biome borders can no longer slice an arch in half.
- The biome provider follows the resolved Origen pipeline and its exact extrusion order. The saved
  quart-resolution ChunkAPI volume is sampled from that same extruded profile, so terrain, F3,
  position-aware API/JAS queries, and cave behavior cannot select competing cave identities.
- Origen defines four Sinkhole profiles, but its active `biome-distribution/presets/rearth.yml` does not include
  `stages/special/add_sinkholes.yml`, and its README labels the family “Currently disabled.” Biomic retains the
  resolved definitions for compatibility and tooling but deliberately leaves them unreachable in normal placement.
- A Terra-driven seed-42 comparison over 4,096 columns matched every profile count. After the only
  intentional Y transform, mean absolute surface error is 0.305 blocks, maximum error is 0.985
  blocks, and all sampled columns are within one block. Native graph, blend, elevation, extrusion,
  and final-density fixtures match Terra to nine decimal places.
- Generator v71 gives the production ring-basin grammar its own `biomic:sky_donut` identity instead of presenting it
  as an Origen biome. The other five independently implemented grammars and their licensed Origen preview hosts are
  dormant outside structural comparison tooling pending individual review; none of this layer is copied from
  SkyLands.
- Generator v72 adds the separately licensed `biomic:aeropelago` 3D identity above admitted Origen ocean. Its GPL-3.0
  sampler and flora remain isolated and attributed; Origen continues to own the ground density, surface identities,
  and decoration beneath the floating layer.
- Modern palette families are mapped to 1.7 vanilla, hard-dependent EFR blocks, or Biomic-owned
  backports where EFR has no equivalent. The current mapping
  includes grass/dirt, stone, gravel, sand, snow, ice, mycelium, terracotta, moss, overworld
  basalt/tuff, and EFR magma. Inferno Isles deliberately places no netherrack. The generated runtime
  manifest contains every 229 authored features, 160 structures, 22 ores, and all resolvable references
  from the pinned pack. Exact execution and visual parity remain protected biome by biome rather than
  being inferred from catalogue completeness alone.
- Lush Loops executes Origen's authored `SMALL_DRIPLEAF` and `BIG_DRIPLEAF` features with their exact
  positive-white-noise salts, thresholds, locator ranges, plantable-block predicates, random heights,
  and horizontal facings. Since EFR 2.6.54-GTNH deliberately does not backport dripleaf, generator
  version 42 supplies Biomic-owned small-leaf, big-leaf, and big-stem blocks and renders their vanilla
  geometry instead of degrading all four upstream states to ordinary wall vines.
- Generator version 43 removes the remaining fake-vine substitutions from the wet/lush land family.
  `CAVE_SPORE_BLOSSOMS` now places a Biomic-owned ceiling blossom with the exact vanilla 1.20.4
  artwork/model geometry in Lush Caves, Vine Vault, and Secluded Valley. Origen mangrove trees retain
  their scripted hanging propagule ages through a dedicated hanging-state block while reusing EFR's
  exact vanilla texture. EFR's mangrove logs, leaves, roots, muddy roots, and standing propagule remain
  the shared content source; Biomic now refuses startup when that experimental EFR family is disabled,
  preventing silent oak/dirt fallbacks in Mangrove Swamp, Murky Marshlands, and Bamboo Basin.
- Generator version 44 removes every aquatic compatibility substitution used by Origen. Biomic now
  supplies animated, water-preserving kelp and seagrass; all five living/dead coral plants, floor
  fans, and blocks; and the exact vanilla model families. `KELP`, `SEAGRASS`, `CORAL_PLANTS`,
  `CORAL_FANS`, `BALL_REEF`, and `DENDRIFORM_REEF` retain their imported salts, locators, scripts,
  and profile ownership. The three dead-coral surface-palette states now resolve to the same coral
  block family instead of stained terracotta. This makes Coral Ocean, Abyssal Alleys, Stonegate
  Seas, Canopy Cascades, and Vertical Jungle River visibly execute their authored aquatic layer.
- Generator version 45 completes the visible Deep Dark vocabulary instead of collapsing three authored
  states into EFR's plain sculk cube. Sensors and can-summon shriekers use Biomic-owned blocks and exact
  vanilla model geometry while reusing the matching textures already shipped by EFR. Sculk veins retain
  all six TerraScript attachment properties without per-block tile entities: four internal block IDs encode
  the up/down pair and legacy metadata encodes north/east/south/west. Projection-aware placement keeps cave
  flora and decals in air when two authored underground layers squeeze onto one 1.7 block. Sensor vibration,
  shrieking, Wardens, and catalyst gameplay are intentionally outside terrain-generation scope; Origen's
  `SCULK_WEBS`, `SCULK_PATCHES`, `SCULK_TENDRIL`, and `SCULK_FLORA` generation grammar is retained.
- Generator version 46 restores Origen's frozen-surface deposit vocabulary. EFR 2.6.54-GTNH does not
  register powder snow, so Biomic supplies a dedicated traversable block with the exact vanilla 1.20.4
  texture instead of replacing it with solid snow. `POWDER_SNOW_DEPOSITS` retains Origen's salt 5825,
  positive-white-noise `1/256` threshold, Y=50..319 locator, full-snow support predicate, 32-block ore
  grammar, and exact ownership across thirteen alpine, tundra, frozen-fungi, vista, and river profiles.
  The block implements terrain-relevant sink, slow, and extinguish behavior; modern freezing damage and
  leather-boot entity rules remain outside the current world-generation scope.
- Generator version 47 removes another set of terrestrial compatibility stand-ins. EFR's exact beetroot,
  cornflower, lily-of-the-valley, smooth-stone, and extra-stone-slab blocks now represent Origen's crop,
  flower, dam, and mossy-slab states; startup validation rejects EFR configurations that disable them.
  EFR does not register rooted dirt, so Biomic supplies that missing ground block with the exact vanilla
  1.20.4 texture and uses it in Origen's Bamboo Ponds and xeric palettes, plantable predicates, and great
  azalea tunnel script. Flowering Forest beetroot patches and the eight cornflower and nine lily-profile
  owner sets retain their exact imported distributor salts, thresholds, locator grammar, and block states.
  Single-block crops and flowers are aligned to real post-compression ground without changing Terra's
  authored-space feature admission or structure decisions.
- Generator version 48 corrects the last newer-Seismic samplers in the natural biome-provider pipeline.
  Canyon masks, patchwork gradients and warps, rivers, and pipeline blending now use the same Terra 6.4.2
  OpenSimplex2/OpenSimplex2S equations as the exact density compiler. A natural seed-42 Foliage Fortress
  fixture covering 1,024 columns now matches Terra's profile and projected-surface hash exactly, including
  the nested outer, middle, and inner borders and their neighboring canyon profiles.
- Generators 49 and 50 restore Snowy Spires' authored decoration instead of treating its clean density
  match as complete visual parity. Thin snow layers and the frozen crossing-vine ice bridges retain their
  imported distributors, locators, block states, and write order. A disposable Java 8 seed-42 server now
  guards this stage through Gradle's `freshDecorationParityTest`, which is also part of `check`: it captures
  the untouched base state for a 196-chunk sentinel, rejects populated/recomputed inputs, and pins both the
  final block audit and per-feature state stream.
- Generator version 51 treats each connected Snowy Spires cellular pillar as one authored landform at a
  biome boundary. Admission examines the complete solid mask plus the same twelve-block blend footprint
  Terra can use to feed the pillar; if that footprint reaches a foreign profile, every column in the
  connected component uses a compiled copy of the upstream graph with the pillar term removed. Neighboring
  clean components and all interior spires retain the exact upstream graph.
- Generator version 52 removes the terrace left where a rejected Snowy Spires component met the adjacent
  low coastal graph. Mixed Snowy-boundary columns blend the contributors' highest base zero crossings and
  evaluate density relative to that compatible height, beginning only above authored Y=48 and reaching full
  weight at Y=64 so caves and deep density remain untouched. The reported seed-42 boundary keeps at least
  sixteen blocks of relief, limits neighboring surface steps to three blocks, and limits the former exact
  67-to-81 shelf to two blocks without restoring the clipped spire.
- Generator version 53 reproduces Terra 6.4.2's pinned four-argument TerraScript sampler dispatch. The
  upstream loader selects the declared 3D sampler but invokes its 2D overload, ignoring the middle
  coordinate; Origen's procedural boulders and mushrooms were authored against that behavior. Matching it
  makes all three Scarlet Sanctuary giant boulders exact across 73,338 Paper block writes and also restores
  shared giant-mushroom geometry. A regression test documents the otherwise surprising compatibility rule.
- The fresh-decoration gate also covers the natural seed-42 Foliage Fortress at `(1184, -768)`. The
  reference was captured by instrumenting Terra 6.4.2 inside the actual Paper 1.20.4 Origen server, not
  Terra's CLI block stub: that stub labels every non-air block as solid and incorrectly turns leaves into
  supports for `MATCH_SOLID`. Biomic matches Paper coordinate-for-coordinate for all plateau-pillar,
  crossing-vine, ceiling-vine, and leaf-vine anchors in the 128-by-128 review window. Pillar, ceiling-vine,
  leaf-vine, and crossing-vine authored writes are exact. Generator v64's TerraScript compatibility correction
  removed the former three-write/five-cell crossing-vine discrepancy. `freshDecorationParityTest` audits Snowy
  Spires and Foliage Fortress in the same untouched disposable world and rejects reused or recomputed base chunks
  for both windows.
- Canopy Cascades at `(-384, -7872)` is the first dense terrace profile covered by that same real-Paper
  workflow. All nine audited showcase features match Paper's distributor coordinates exactly: jungle
  trees, dendriform and ball reefs, persistent leaf webs, melons, both dripleaf forms, coral plants, and
  coral fans. Melons and small dripleaf match anchor and authored-block coordinates exactly, while ball
  reefs produce the exact same 1,447 authored cells despite locator-order swaps. The remaining small
  differences are bounded cross-chunk ordering effects: an earlier tree or reef changes the top/support
  block seen by a later candidate. The largest is three shifted jungle-tree roots (972 differing authored
  attempts out of roughly 15,500), not a distributor, structure, or density mismatch. The fresh-world gate
  pins the exact shared distributor hashes, Biomic's deterministic locator and state-aware authored-block
  streams, and each measured Paper symmetric delta so that an order caveat cannot conceal a real content
  regression.
- Vertical Vistas at `(-8448, -1088)` extends the real-Paper gate to a naturally tall terrace window whose
  uncompressed surface reaches authored Y=217. All nine feature distributor sets match Paper exactly. Sparse
  birch trees and the poppy, oxeye-daisy, cornflower, and allium patches match both accepted anchors and
  authored block writes coordinate-for-coordinate. The sampled powder-snow distributor has no valid deposit
  in either runtime. Generator v68's state-exact `MATCH_SET` correction removes the old metadata-aliasing
  fringe: the controlled four-chunk core now matches all 98,304 comparable non-ore voxels, and the complete
  64-chunk window differs in only four stone/andesite cells among 1,572,864 voxels after excluding 27,353
  EFR-owned ore cells. The disposable Biomic world captured 169 untouched base chunks and recomputed none;
  its deterministic snow, slant, and grass streams remain pinned alongside the exact shared streams.
- Badlands Balconies at `(-9536, 16064)` covers a natural terrace interior that reaches physical Y=172.
  Badlands oak trees, oak bushes, sparse dead trees, and sparse grass all match Paper's distributor
  coordinates exactly. The 47 primary-oak roots are exact and their roughly 10,650-block authored streams
  differ by only four leaf writes. Bush, dead-tree, and grass locators retain one, one, and eleven
  cross-chunk-order anchor cells respectively. After normalizing Paper's explicit waterlogged, honey-level,
  and vertical-axis defaults to their equivalent imported states, the corresponding authored-block deltas
  are 21, 16, and 12 cells. The fixture captures all 169 base chunks without recomputation and pins each
  deterministic Biomic stream plus the measured Paper delta.
- Mountain Mirrors at `(12288, 9376)` covers a 100-percent-native frozen terrace window peaking at physical
  Y=213. All seven audited distributor sets match Paper exactly. The three ice-spike roots, twelve
  cold-mountain-tree roots, and 31 powder-snow anchors are exact; after normalizing Paper's explicit vertical
  log/wood axis and waterlogged defaults, all 3,091 tree writes are coordinate-for-coordinate exact. Ice
  spikes differ by one extra blue-ice write. Tree snow, powder snow, snow layers, and textured-stone slant
  retain bounded cross-chunk-order deltas of 10, 30, 116, and 13 authored cells respectively, while the
  high-altitude coastline locator remains empty in both runtimes. The fixture captures 169 untouched base
  chunks and recomputes none.
- Scarlet Sanctuary at `(-6272, -10752)` adds a 100-percent-native tilted plateau whose projected surface
  spans Y=99..163. All sixteen authored distributor sets match Paper exactly. The three giant-boulder roots
  and all 73,338 stone writes are coordinate-for-coordinate and state-for-state exact. The single dead-swamp
  tree is likewise exact, and 15 of 16 shared giant-mushroom roots have exact coordinate streams; the last
  reaches the outer edge of the traced generation region. Grass, flowers, ferns, dripleaf, small mushrooms,
  dead trees, giant forest trees, giant mushrooms, and both vine systems retain measured cross-chunk locator
  order deltas. The large raw tree/vine counts come from four extra accepted giant-tree roots, not different
  distributor or structure math. The gate pins all sixteen deterministic Biomic streams and their measured
  Paper cell deltas across 169 captured base chunks with no recomputation.
  Generator v54 additionally fixes the final block-model and tint translation found during side-by-side visual
  review: modern bare red/brown caps map to vanilla 1.7 cap metadata instead of white stems, face-less cap states
  map to pores, and Scarlet's foliage/vines use the authored `0xEC5448` tint. Its grass and water use authored
  `0x599C2C` and `0x5DB7EF`. Frozen Fungi's next-review colors are likewise pinned to the pack values.
  Generator v55 records the canonical Terra writer order with every cached authored feature block. If ordinary
  player-driven loading populates a later neighbouring canopy first, Scarlet's giant-tree and giant-mushroom TOP
  locators now ignore that reversed dependency while retaining genuine earlier collisions and later-writer ownership.
  The normal Z-major oracle remains byte-for-byte unchanged. A reverse-traversal audit additionally restores the
  brown mushroom rooted at `(-6300, -10767)`, including the reviewed local 423 pore, 546 cap, and 179 stem cells,
  rather than losing the whole structure behind the neighbouring canopy. `/biomic auditfeaturesreverse` keeps that
  adversarial traversal available to operators.
  Generator v56 also heals the mushroom's dependent foliage when its cap reaches a neighbour whose flora stage has
  already completed. Only touched columns in already-populated neighbouring chunks replay Origen's deterministic
  `LARGE_LEAF_VINES_UNCOLORED` feature; the root chunk and not-yet-populated neighbours still use the normal flora
  stage. Replayed cells retain the mushroom's canonical ownership, so later authored writers remain authoritative.
  This intentionally removes Terra's live chunk-arrival artifact instead of treating its missing foliage as visual
  parity. A final-state fixture pins all 59 persistent EFR azalea-leaf cells in the reviewed cap window around
  `(-6300, -10767)` while the original Z-major Paper/Terra stream oracle remains unchanged.
  Generator v57 makes the other reviewed mushroom-first result deterministic. Terra's live OG world omitted the
  sparse dead tree rooted at `(-6294, 133, -10778)` when the neighbouring brown mushroom populated first, whereas the
  opposite arrival order retained an isolated log cluster below its cap. Biomic records each eligible tree's exact
  authored cells and writer, then rolls back only a tree whose root column is actually touched by a later cross-chunk
  giant mushroom. It restores the underlying authored terrain and will not erase cells another feature has since
  claimed. The position key now uses Minecraft's full signed 26-bit X/Z packing; regression coverage includes this
  negative-quadrant fixture and both 30-million-block borders. The fresh gate pins zero wood in the reviewed footprint
  and EFR moss at its restored root.

  The same revision distinguishes two semantic uses which Origen encodes with the same modern block state. Literal
  `minecraft:mushroom_stem` trunk writes remain vanilla 1.7 brown-huge-mushroom metadata 10. Only `spotBlock` in
  `large_mixed_mushroom_procedural.tesf` and `red_mushroom_disk.tesf` is translated to
  `biomic:mushroom_flesh`, an all-sided cube using Minecraft 1.20.4's exact
  `assets/minecraft/textures/block/mushroom_stem.png` (SHA-256
  `9633dd875492cf5240be3e2442e4729c2350912f4b0b565c9f01640195466401`). Debug traces normalize that visual-only
  distinction back to the authored `minecraft:mushroom_stem` state. The initial final-state snapshot pinned all 213
  translated cap-spot cells in the 128-by-128 Scarlet review window.
  Generator v58 extends that translation to the exposed final trunk layer of
  `large_mixed_mushroom_procedural.tesf`; interior trunk layers deliberately remain vanilla metadata 10. It also
  retains rejected giant-mushroom writes until cross-chunk tree rollback has resolved, then replays only the still-
  authoritative cap cells. The reviewed red cap at `(-6361, -10832)` therefore remains whole regardless of arrival
  order. Black Forest redwoods now take part in cross-chunk canopy/root reconciliation, matching the OG omission of
  the two reviewed birches around `(-6435, -10865)` and `(-6421, -10869)`. EFR glow-lichen side masks are initialized
  through their persisted tile state rather than the public update-notifying setter, which prevents recursive chunk
  population during reversed traversal. Forward and reverse fresh-world fixtures pin these exact final states, while
  the main snapshot pins the resulting 242 cap-spot and exposed-top Mushroom Flesh cells. All three disposable-server
  gates require an explicit pass report and run from `check` on separate ports in a fixed order.
  Generator v59 extends tree rollback to decoration causality. Every `TREE_VINES` write records the tree origin that
  admitted its locator, including transitive support through an earlier dependent vine structure. Rejecting that
  tree rolls back only cells still owned by those dependent structures before restoring the tree. A direct Anvil
  volume comparison against the live Paper/Origen oracle found 38 such leftovers in the affected chunk: four
  persistent oak-leaf cells and 34 vines. The v59 comparison removes all 38, and the forward/reverse western Scarlet
  fixture pins their complete footprint rather than one reported column.
  Generator v60 introduced an explicitly non-upstream visual correction to the shared `EQ_PILLARS` base used by
  Snowy Spires and Mesa Monuments. Origen's `basePillarsExistance` NoiseLookup gates a site after ordinary Voronoi
  ownership has already been decided, so a disabled neighbouring site can cut an enabled radial pillar along its
  straight cell border. Generator v61 makes that correction strictly additive: the untouched imported pillar result
  remains the baseline, and the filtered next-enabled-site result contributes only its positive overlap where the
  original owner was disabled. This matters because a filtered cellular query can have no candidate in its local
  search neighborhood; allowing that sentinel distance into the signed pillar equation would subtract terrain.
  A seed-42 Mesa/Overgrown-Cliffs boundary fixture samples 236,600 graph points and pins 210 intentional changes,
  42 newly solid samples, and zero removals. The seed-42 Snowy corner around `(2309, -4246)` and its west/south
  shoulders remain pinned in unit and fresh-world tests.
  The Mesa Monuments interior at `(3712, -2112)` is now covered by a natural terrain hash and fresh-world authored
  streams for its cacti, dead bushes, absent sparse dead-tree candidates, granite monuments and scatter, oak bushes,
  and textured slant. A direct 16-chunk Anvil comparison checks 397,312 lossless-band voxels against the live
  Paper/Origen world: every non-ore block is exact. The 291 remaining cells are only coal, copper, and iron placement,
  which intentionally remains under EFR's ore policy. The comparison normalizer also maps legacy
  `minecraft:deadbush` to modern `minecraft:dead_bush`, alongside the earlier full-block/layered-snow aliases.
  Bare Boulderfields at `(-3168, -448)` is now locked the same way. Its tilted-plateau natural window and all six
  profile-owned streams are under regression coverage: powder-snow and snow-layer candidates, sparse giant-pillar
  admission, small boulder patches, full boulders, and textured slants. A controlled four-chunk interior comparison
  checks 98,304 one-to-one-band voxels and is exact after excluding 1,207 ore-placement cells owned by EFR. The direct
  comparator's explicit `--ignore-ore-placement` mode performs that exclusion without weakening comparisons for any
  terrain, palette, surface, or decoration block.
  Generator v62 extends the same treatment to Arid Arboretum at `(13984, 13952)`. It preserves Terra 6.4.2's shipped
  value-noise interpolation argument order and the platform generator's chunk-local slant probes: a negative
  fractional probe at a north or west chunk edge is truncated within the owning `ChunkInterpolator` and extrapolates
  its first 4x4 cell rather than sampling the neighbouring chunk. Those quirks decide both the terracotta strata and
  whether trees or later bushes can occupy an edge column. The disposable-server gate pins all fifteen visible
  profile-owned streams. Its four-chunk interior matches the Paper/Origen oracle across all 98,304 comparable
  physical-Y=64..159 voxels after excluding 1,375 EFR-owned ore cells.
  Black Forest at `(5376, -1408)` adds the next natural-window and disposable-server fixture. All eight visible
  profile-owned streams are pinned, and its controlled four-chunk interior matches Paper/Origen across 98,304
  comparable non-ore voxels; 468 EFR-owned ore cells are excluded explicitly. A temporary instrumented build of
  Terra's feature stage independently confirmed all 5,409 distributor points in a 160x160 oracle window and exact
  tree and large-boulder anchors. Repeated clean Paper worlds differed by 321 spruce cells at the parallel-generation
  fringe, while mutable-world small-boulder/slant locators left 90 bounded final-state differences. Those
  schedule-dependent cells are documented rather than treated as a deterministic reference contract.
  Tall Timberland at `(9008, 7216)` completes the same review for the second mountain-clearings profile. Its natural
  256x256 window contains 1,024/1,024 Tall Timberland samples with physical surface Y=72..124, and its fresh fixture
  pins all fourteen visible profile-owned streams. The four-chunk interior is exact across 98,304 comparable non-ore
  voxels after excluding 583 EFR-owned ore cells. A structure-disabled 64-chunk comparison covers 1,572,864 voxels;
  the remaining 436 cells are a bounded mutable-column fringe. Instrumented Terra confirms all 10,717 distributor
  evaluations for the six causal tree, boulder, flower, and slant streams. Paper alone accepts one additional giant
  redwood anchor at `(8959, 97, 7170)`, accounting for 420 of those cells. This is retained as an explicitly measured
  parallel chunk-scheduling effect, not folded into Biomic's deterministic generation contract.
  Bamboo Basin at `(-8960, -3520)` is the next completed natural-window review. All 1,024 samples in its 256x256
  window resolve to Bamboo Basin, with the full-resolution surface spanning physical Y=54..112. The fresh fixture
  pins eleven visible streams: eucalyptus, mangrove and dead-swamp trees, jungle bushes, bamboo, potatoes, grass,
  mossy boulders, mangrove seagrass, floating-water containment, and textured slants. Its central four chunks match
  Paper/Origen across all 98,304 comparable non-ore voxels after excluding 808 EFR-owned ore cells. Across a
  structure-disabled 64-chunk comparison, 514 of 1,572,864 cells remain schedule-dependent after excluding 9,572
  ore cells. Instrumented Terra confirms all 12,085 distributor evaluations for the seven causal structure and
  slant streams exactly; only mutable surface-anchor acceptance/Y differs at the parallel fringe.
  Carving Creaks at `(7424, -11264)` adds a pure canyon-profile window. Its 1,024-point terrain signature spans
  sampled physical surface Y=57..113 and its fresh fixture pins ten surface streams: cacti, dead bushes and trees,
  oak bushes, both granite-boulder families, floating-water containment, textured slants, kelp, and seagrass. The
  four-chunk interior is exact across 98,304 comparable non-ore voxels after excluding 1,218 EFR ore cells. The full
  structure-disabled 64-chunk comparison differs at only one of 1,572,864 cells after excluding 15,894 ore cells:
  Paper retains granite at `(7429, 114, -11278)`. Instrumented Terra matches all 64,669 unique distributor entries
  and all 342 accepted anchors in the surrounding 160x160 window, proving that cell is write-order scheduling beside
  a small-boulder anchor rather than terrain, RNG, locator, or structure-geometry drift.
  Fossilized Fenlands at `(3840, 11264)` completes the first segmented-lowland review. Its natural 256x256 window is
  pure Fossilized Fenlands at all 1,024 samples and spans physical surface Y=63..81. The fresh fixture pins fifteen
  profile streams, including fossil ribs, swamp and mesquite trees, bamboo, ceiling vines, flowers, grass, dripleaf,
  aquatic features, containment, and textured slants. Its central four chunks match Paper/Origen across all 98,304
  comparable non-ore voxels after excluding 110 EFR ore cells. Across the structure-disabled 64-chunk comparison,
  48 of 1,572,864 cells remain after excluding 2,529 ore cells. Instrumented Terra confirms all 48,352 distributor
  coordinates and all four fossil-rib anchors in the 128x128 gate window. Paper's parallel mutable surface leaves a
  42-cell anchor delta only in bamboo, grass, and lilac; the final fossil/slant/surface residue is bounded scheduling
  and write ordering. The comparison tool now preserves `minecraft:bone_block[axis]` and translates EFR bone
  metadata to the same state, so rotated rib blocks are checked without false mismatches.
  Frosty Fingers at `(-6912, 3584)` adds the frozen-dune family. Its 1,024-point natural window is pure Frosty
  Fingers with sampled physical surface Y=59..104. The fresh fixture pins ice spikes, snow layers, coastline ice,
  tree snow, containment, textured slants, kelp, and seagrass. The central four chunks differ from Paper/Origen at
  one of 98,304 comparable non-ore cells after excluding 77 EFR ore cells. The structure-disabled 64-chunk window
  leaves 171 of 1,572,864 cells after excluding 1,824 ore cells. Instrumented Terra confirms all 90,251 distributor
  coordinates and all 21 ice-spike anchors in the 128x128 gate window. The only locator variation is a 25-cell Snow
  Layers anchor delta at Paper's parallel-generation fringe; the ice/snow final-state residue is therefore bounded
  scheduling and write order, not terrain, RNG, spike admission, or structure geometry.
  Frozen Fungi at `(-20352, -18304)` completes the cold tilted-plateau review and stress-tests the vertical squeeze.
  Its natural 256x256 window is pure Frozen Fungi at all 1,024 samples and spans physical surface Y=66..136. The
  fresh fixture pins nine visible streams: cold fungi, mossy boulders, powder-snow deposits, small mushrooms, snow
  layers, containment, textured slants, kelp, and seagrass. Its central four chunks match Paper/Origen exactly across
  all 98,304 comparable non-ore voxels after excluding 1,052 EFR ore cells. The structure-disabled 64-chunk window
  differs at only four of 1,572,864 cells after excluding 14,739 ore cells: two snow layers and two red mushrooms.
  Instrumented Terra confirms all 58,078 distributor coordinates plus every cold-fungus, mossy-boulder, and slant
  anchor in the 128x128 gate window. The only locator variation is a 69-cell symmetric delta confined to Snow Layers
  and Small Mushrooms under Paper's parallel chunk schedule.
  Generator v63 reproduces Terra 6.4.2's exact slant-vector normalization, including its one-iteration fast inverse
  square root and arithmetic order. Four Arctic surface cells whose exact normalized Y component is just below the
  authored `0.95` threshold now select blue ice rather than the dark base palette. The corrected classification also
  updates a bounded set of surface-sensitive reef, grass, snow, and powder-snow fixture coordinates; the complete
  fresh-world suite pins and verifies all 18 reviewed windows after that global change.
  Arctic Arches at `(2752, 1728)` completes the frozen-arch ocean review. Its natural 256x256 window is pure Arctic
  Arches at all 1,024 samples and spans physical surface Y=44..125. The fresh fixture pins ceiling icicles, clay
  deposits, water containment, kelp, seagrass, and textured slants. Instrumented Terra agrees on all 42,673
  distributor evaluations and all 15,567 accepted anchors. The structure-disabled 64-chunk comparison is exact for
  all 1,572,864 compatibility-normalized voxels in physical Y=64..159, including ore placement.
  Generator v64 reproduces another pinned Terra 6.4.2 TerraScript quirk: `check(...)` rounds each horizontal
  relative coordinate before adding the structure origin, and its implementation discards the vector returned by
  the rotation helper. Biomic had instead sampled fractional, rotated X/Z offsets. Matching the shipped behavior
  removes a false granite boulder body in Gloomy Gorge and corrects the same support test for every shared procedural
  boulder, mushroom, and vine script; a focused interpreter regression prevents either quirk from being cleaned up
  accidentally.
  Gloomy Gorge at `(4608, -4224)` is the nineteenth reviewed fresh-world window. Its natural 256x256 signature is
  pure Gloomy Gorge at all 1,024 samples and spans physical surface Y=66..135. The gate pins all seventeen visible
  streams, including bamboo, both granite-boulder families, mesquite trees, ceiling and crossing-stone vines, glow
  berries, flowers, grass, dripleaf, aquatic features, floating-block cleanup, containment, and textured slants.
  Instrumented Terra matches all 67,238 distributor decisions and every scheduling-stable anchor. The central four
  chunks differ at nine of 98,304 comparable non-ore voxels after excluding 816 EFR ore cells; all nine are one
  Paper-only bamboo stalk at `(4623, -4239)`. Across the structure-disabled 64-chunk window, 70 of 1,572,864 non-ore
  voxels remain after excluding 13,945 EFR ore cells, confined to mutable locator and parallel-population ordering at
  the fringe rather than terrain, RNG, or structure geometry.
  Alpine Ascendancy at `(-2816, 576)` is the twentieth reviewed fresh-world window and covers Origen's high-mountain
  family. Its natural 256x256 signature is pure Alpine Ascendancy at all 1,024 samples and spans physical surface
  Y=69..220. The fixture pins nineteen visible streams: both sharp-terrace passes, waterfalls, spruce trees, both
  mossy-boulder families, five flower/grass families, snow layers, tree snow, powder snow, textured slants,
  containment, kelp, and seagrass. Instrumented Terra agrees on all 110,654 distributor coordinates. Its stable
  flower, waterfall, and slant anchors are exact; boulder, spruce, and snow locator differences are bounded effects
  of Paper's parallel mutable surface. The central four chunks match all 98,304 comparable non-ore voxels after
  excluding 1,329 EFR ore cells. The structure-disabled 64-chunk window retains 2,389 of 1,572,864 non-ore cells after
  excluding 19,519 EFR ore cells, confined to the scheduling-sensitive fringe rather than terrain, distributor RNG,
  or structure geometry.
  White Wallows at `(3456, 12224)` is the twenty-first reviewed fresh-world window and covers the pale counterpart of
  Origen's eroded-valley family. Its natural 256x256 signature is pure White Wallows at all 1,024 samples and spans
  physical surface Y=66..137. The fixture pins fourteen visible streams: great azalea and mesquite trees, azalea
  ground blocks, mixed bushes, ferns, grass, white flower cover, ceiling vines, mossy boulders, slants, containment,
  kelp, and seagrass. Instrumented Terra agrees on all 46,921 distributor coordinates, with exact stable great-azalea,
  mesquite, mixed-bush, fern, and flowering-azalea anchors. The central four chunks match all 98,304 comparable
  non-ore voxels after excluding 1,472 EFR ore cells. After generator v68 stops metadata variants from satisfying
  one-state match sets, the structure-disabled 64-chunk window retains only two of 1,572,864 non-ore cells after
  excluding 18,478 EFR ore cells: one leaf/grass cell and one tall-grass/sunflower upper-half cell.
  Murky Marshlands at `(-8256, 8704)` is the twenty-second reviewed fresh-world window and covers Origen's dense
  wetland/canopy family. Its natural 256x256 signature is pure Murky Marshlands at all 1,024 samples and spans
  physical surface Y=59..76. The deterministic fixture pins fourteen visible streams covering dark-swamp, dead-swamp,
  and mangrove trees, roots, vines, aquatic plants, marsh flora, containment, and slants. Instrumented Terra agrees on
  all 45,660 distributor coordinates, while isolated shared trees have identical per-origin authored block streams.
  Two repeated Paper traces at the same seed prove the remaining mutable-locator ambiguity is upstream: one
  dark-swamp root moves and 1,424 normalized block attempts change among common swamp-tree roots solely from worker
  order. Consequently, the 3,052 central and 13,683 wider non-ore volume deltas are recorded as scheduling evidence,
  not mislabeled as an exact oracle; 2,955 central cells cluster around one Paper-only giant tree. Compatibility
  comparison also canonicalizes legacy `waterlily` and `reeds` names to modern lily pads and sugar cane.
  Generator v65 adds Marine Monoliths as the twenty-third reviewed fresh-world window. Because the authored coast is
  deliberately narrow, the terrain gate combines a natural 256x256 ecotone signature at `(-5192, -4180)` with an
  all-Marine 32x32 interior at `(-5216, -4096)` whose 1,024 columns sit at physical surface Y=114..116. The central
  four chunks match Paper/Origen across all 98,304 comparable non-ore voxels after excluding 56 EFR-owned ore cells.
  The focused fixture pins every Marine-owned landform, flower, grass, kelp, and seagrass stream plus all five BASE
  preprocessors. Instrumented Terra agrees coordinate-for-coordinate on every distributor and stable anchor in the
  core. That trace exposed one importer edge case: Marine's comments-only `global-preprocessors` YAML key loads as
  `null`, and Tectonic skips null layers and inherits BASE, while Biomic had mistaken it for an empty override. The
  importer now reproduces Tectonic's null-fallback rule, restoring lava-floor, water-containment, cave-lichen, and
  textured-slant passes; a manifest regression prevents the inherited list from disappearing again.
  Sakura Streams at `(29120, -31936)` is the twenty-fourth reviewed fresh-world window and covers Origen's
  summit-stream/cherry family. Its natural 256x256 signature is pure Sakura Streams at all 1,024 samples and spans
  physical surface Y=64..113. The fixture pins sixteen visible streams: both cherry-tree families, both
  mossy-boulder families, grass, pink petals, rose bushes, two vine families, cave lichen, textured slants, lava
  floors and columns, floating-water containment, kelp, and seagrass. Instrumented Terra agrees on all 79,815
  distributor coordinates, and both cherry-tree root sets and ordinary-tree authored block streams are exact. The
  central four chunks match all 98,304 comparable non-ore voxels after excluding 909 EFR ore cells. The
  structure-disabled 64-chunk window retains only 46 of 1,572,864 non-ore cells after excluding 13,152 EFR ore
  cells, all bounded leaf/petal fringe effects from Paper's parallel mutable surface. The Anvil comparator now also
  decodes EFR's log/wood/stripped-log/stripped-wood subtype bits, so all-bark cherry wood is compared with its modern
  canonical state instead of being misreported as an oriented log.
  Verdant Valleys at `(-16512, -31552)` is the twenty-fifth reviewed fresh-world window and covers Origen's
  low mountain/pillar valley family. Its natural 256x256 signature is pure Verdant Valleys at all 1,024 samples and
  spans physical surface Y=69..125. The fixture pins fourteen visible streams: sparse acacia trees, grass, five
  flower/melon families, lava floors and columns, floating-water containment, cave lichen, textured slants, kelp,
  and seagrass. Instrumented Terra agrees on all 61,407 distributor coordinates and, unusually for a decorated
  profile, every anchor and every authored block write is exact. The structure-disabled 64-chunk window retains only
  three of 1,572,864 non-ore cells after excluding 6,764 EFR ore cells: two orphan upper tall-grass halves whose
  species cannot be encoded independently in 1.7 metadata, plus one Paper-order melon overwrite. The comparator also
  canonicalizes legacy `melon_block` to the flattened modern `melon` identifier.
  Watery Wilds at `(20736, -30336)` is the twenty-sixth reviewed fresh-world window and covers Origen's wet
  stone-savanna family. Its natural 256x256 signature is pure Watery Wilds at all 1,024 samples and spans physical
  surface Y=58..81. The fixture pins twenty streams: red-rock outcrops, acacia trees, jungle bushes, grass and tall
  grass, six flower/cactus/melon/sugar-cane families, both seagrass passes, and all five BASE preprocessors plus
  kelp. Instrumented Terra agrees on all 75,631 distributor coordinates. Seventeen complete locator/block streams
  are exact; the only measured platform deltas are five grass anchor cells, fifteen tall-grass anchor cells, and one
  cornflower cell along the vertically compressed sea-level shelf. The structure-disabled 64-chunk volume retains
  only 22 of 1,572,864 non-ore cells after excluding 169 EFR ore cells: nine jungle-bush/grass ordering cells and
  thirteen double-plant upper-half representation/order cells. The fresh-world regression gate now regenerates all
  twenty-six reviewed windows from seed 42.
  Tundra Tracks at `(29312, -29504)` is the twenty-seventh reviewed fresh-world window and completes Origen's
  lowland-hills pair. Its natural 256x256 signature is pure Tundra Tracks at all 1,024 samples and spans physical
  surface Y=59..82. The fixture pins thirteen streams: cold evergreen trees, granite boulder patches, snow layers,
  tree snow, powder-snow deposits, ice coastline, and all seven BASE passes. Instrumented Terra agrees on all
  108,301 distributor coordinates. Ten complete streams are exact, including seagrass; the general snow-layer pass
  retains an exact authored block stream despite 147 scheduling-sensitive locator cells. The
  controlled four-chunk interior matches all 98,304 comparable non-ore voxels exactly. The structure-disabled
  64-chunk volume retains only 147 of 1,572,864 non-ore cells after excluding 393 EFR ore cells, exactly matching the
  `TREE_SNOW` worker-order fringe of 52 Biomic-only and 95 Paper-only one-layer snow cells. The clean-world gate now
  regenerates all twenty-seven reviewed windows from seed 42.
  Oasis at `(3152, 11712)` is the twenty-eighth reviewed fresh-world window and covers Origen's compact warm-basin
  special. Its natural 256x256 signature spans physical surface Y=58..80 and preserves the authored small scale:
  107 of the 1,024 samples form two Oasis lobes embedded predominantly in desert instead of being inflated into a
  province-sized biome. The fixture pins twelve visible streams: oasis palms, jungle bushes, grass, sugar cane,
  small boulders, kelp, seagrass, and all five BASE preprocessors. Instrumented Terra agrees on all 59,687
  distributor coordinates. Eleven complete locator/block streams are exact, including floating-water containment
  and seagrass; the only measured platform delta is 22 palm-tree block attempts, while every palm anchor remains
  exact. Four Oasis-interior chunks match all 98,304 comparable non-ore voxels,
  and the complete structure-disabled 64-chunk mixed window matches all 1,572,864 non-ore voxels after excluding
  341 EFR-owned ore cells. The clean-world gate now regenerates all twenty-eight reviewed windows from seed 42.
  Pillow Plains at `(-2880, 2368)` is the twenty-ninth reviewed fresh-world window and covers Origen's complete
  nested plains family. Its natural 256x256 signature spans physical surface Y=46..132 and contains 631 inner, 273
  middle, and 20 outer Pillow Plains samples. The fixture pins seventeen observed streams: low evergreen trees,
  both grass densities, four flower patches, boulders, small boulders, kelp, seagrass, and all six active BASE/cave
  preprocessors. Instrumented Terra agrees on all 54,492 distributor coordinates. Fifteen complete locator/block
  streams are exact; the only measured platform deltas are two glow-lichen cells and three low-grass cells. Four
  inner-core chunks match all 98,304 comparable non-ore voxels, and the complete structure-disabled 64-chunk mixed
  window matches all 1,572,864 non-ore voxels after excluding 72 EFR-owned ore cells. The clean-world gate now
  regenerates all twenty-nine reviewed windows from seed 42.
  Secluded Valley at `(14720, -25472)` is the thirtieth reviewed fresh-world window and covers both nested valley
  profiles. Its natural 256x256 signature contains 970 inner and 54 outer samples across physical surface Y=33..89.
  The fixture pins seventeen active streams and 67,711 distributor decisions: giant sakura trees and glow berries,
  petals, sunflowers, carrots, moss carpet, dripleaf, cave grass/lichen/vines/spore blossoms, slants, lava floors and
  columns, and floating-water containment. Generator v68 makes the separate giant glow-berry stream exactly empty
  like Paper, bringing ten complete locator/block streams to exact parity; the remaining bounded
  platform deltas are two leaf-vine cells, four moss-carpet cells, one sunflower anchor/two writes, one petal
  anchor/write, three slant writes, nine carrot writes, and sakura-tree writes with exact tree anchors. Paper's trace
  attributes the giant's nested glow-berry writes to its caller rather than the child feature. Across the complete
  structure-disabled 64-chunk window, only two moss-carpet cells differ among 1,572,864 comparable non-ore voxels
  after excluding 7,611 EFR-owned ore cells. The clean-world gate now regenerates all thirty reviewed windows.
  Stonegate Seas at `(-5216, -3872)` is the thirty-first reviewed fresh-world window and covers Origen's crossing
  ocean arches. Its natural 256x256 signature contains 956 Stonegate Seas, 49 Temperate Sea Arches, and 19 river
  samples across physical surface Y=51..127; the inner 128x128 audit is pure Stonegate. The fixture pins thirteen
  active streams and 79,640 distributor decisions: both ceiling-vine passes, crossing and leaf vines, small and big
  dripleaf, coral plants and fans, cave lichen, slants, lava floors and columns, and floating-water containment. Ten
  complete locator/block streams are exact. In an expanded parallel Paper run, the only bounded deltas are one
  leaf/vine overwrite and one nine-block mutable TOP-locator slant. The controlled four-chunk core matches all
  98,304 comparable non-ore voxels exactly; the full 64-chunk window differs in only those ten of 1,572,864 cells
  after excluding 913 EFR-owned ore cells. The clean-world gate now regenerates all thirty-one reviewed windows.
  Abyssal Alleys at `(-2048, 1600)` is the thirty-second reviewed fresh-world window and the first reference window
  centered almost entirely inside the compressed lower band. Its natural 256x256 signature contains 774 Abyssal,
  102 Fossilized Fenlands, 87 beach, 58 ocean, and three river samples across physical surface Y=4..76. The inner
  128x128 audit is pure Abyssal and spans Y=3..61. The fixture pins fourteen active streams and 129,194
  distributor decisions: both reef grammars, coral plants and fans, fossil structures, kelp, seagrass, cave lichen,
  both floating-water containment passes, lava floor and columns, magma deposits, and textured slants. Twelve streams
  are exact against instrumented Paper after compatibility-state normalization; the only bounded deltas are one
  mutable glow-lichen cell and twenty magma-deposit block cells. A structure- and population-disabled modern reference
  matches all 1,048,576 compared pre-population voxels across the full 64-chunk lower window exactly. The clean-world
  gate now regenerates all thirty-two reviewed windows from seed 42.
  Sandy Splits at `(-6160, -4352)` is the thirty-third reviewed fresh-world window and exercises Origen's canyon
  palette and complete cactus, giant granite-boulder, canyon leaf-vine, cave-flora, containment, lava, aquatic-flora,
  magma, and textured-slant stack. The natural 256x256 fixture contains 493 Sandy Splits samples and reaches physical
  surface Y=40..140; the inner 128x128 audit is effectively a controlled Sandy interior. An expanded instrumented
  Paper run matches all twelve profile-owned distributor, accepted-anchor, and authored-block streams exactly after
  normalizing modern cactus's explicit default age property. Four central chunks match all 98,304 comparable non-ore
  voxels exactly. Across the full 64-chunk window, 457 of 1,572,864 cells differ, all belonging to one adjacent
  Overgrown Cliffs jungle tree rather than Sandy terrain or decoration. The clean-world gate now regenerates all
  thirty-three reviewed windows from seed 42.
  Icy Incisions at `(1280, -768)` is the thirty-fourth reviewed fresh-world window and exercises the frozen canyon
  palette plus snow layers, ceiling icicles, granite boulders, sparse canyon evergreens, tree snow, frozen hanging
  leaves, cave flora, containment, lava, magma, slants, kelp, and seagrass. The natural 256x256 fixture contains 140
  Icy Incisions samples inside the surrounding Foliage Fortress/cold-biome boundary and spans physical surface
  Y=56..115. Instrumented Paper matches all 133,328 distributor decisions. Ten complete locator/block streams are
  exact; the five explicit bounded deltas are mutable cave-vine, lichen, frozen-leaf-vine, snow-layer, and magma
  locators under Paper's parallel population schedule. Four central canyon chunks match all 98,304 comparable
  non-ore voxels exactly. The complete mixed 64-chunk window differs in 162 neighboring-tree cells among 1,572,864
  voxels after excluding 11,674 EFR-owned ore cells. The clean-world gate now regenerates all thirty-four reviewed
  windows from seed 42.
  Lush Loops at `(-3072, 2848)` is the thirty-fifth reviewed fresh-world window and exercises Origen's complete
  analytic-arch flora stack: jungle trees and bushes, flowers, small and big dripleaf, crossing and ceiling vines,
  leaf curtains, glow lichen, kelp, seagrass, and underground magma. Its natural 256x256 fixture contains 1,023
  Lush Loops samples and one Coral Ocean sample across physical surface Y=65..181. Instrumented Paper matches all
  51,794 distributor decisions across eighteen feature streams exactly; seven complete locator/block streams are
  exact and the remaining bounded deltas are mutable-height or parallel tree/vine scheduling effects. The central
  four chunks differ only in 50 ordinary-vine cells among 98,304 comparable non-ore voxels after excluding 139 EFR
  ore cells. The complete 64-chunk window differs in 5,993 of 1,572,864 cells after excluding 2,904 EFR ore cells,
  dominated by one upstream arch lobe that Biomic intentionally rejects because it crosses the Lush Loops boundary,
  plus foliage/vine scheduling. This is the deliberate, visually reviewed no-half-arch policy rather than a porting
  omission. A second focused capture at the reported `(-2993, 2893)` boundary pins the three analytic lattice
  sources that formerly escaped admission because the helper transposed Origen's `archMask(z, 0)` arguments. All
  three are now rejected with their dependent decoration, while the complete interior showcase loop remains. The
  clean-world gate now regenerates all thirty-five reviewed windows from seed 42.
- Rocky Refuge at `(5552, -7424)` is the thirty-sixth reviewed fresh-world window. Its mixed natural signature
  contains 606 Rocky Refuge, 149 Ocean, 102 Icy Incisions, 56 Deep Ocean, 43 River, 35 Mangrove Swamp, and 33 Frosty
  Fingers samples across physical surface Y=44..132. The inner 128x128 fixture pins twenty-four temperate-overhang,
  giant-pillar, boulder, hanging-leaf, frozen-canyon, snow, aquatic, cave, containment, and lava streams. Instrumented
  Paper matches all 92,483 distributor decisions exactly. Fifteen complete locator/block streams are exact; the nine
  explicit bounded deltas are mutable cave/surface locators or parallel foliage scheduling effects. The fixture also
  locks the natural terrain signature, so Rocky's authored shape and complete visible feature graph are now guarded
  together. The clean-world gate now covers all thirty-six reviewed windows from seed 42.
- Vine Vault at `(1216, -768)` is the thirty-seventh reviewed window and the first independently locked 3D cave
  profile. Paper's saved quart-biome sections provide a 32,768-cell authored Y=-64..60 oracle containing exactly
  13,822 Vine Vault cells beneath the mixed Foliage Fortress/cold-canyon surface; Biomic matches every identity and
  the ordered SHA-256 signature. A separate fresh-server fixture pins all twelve `cave_jungle.yml` decoration
  streams and all 24,162 distributor decisions, with every distributor coordinate exact against instrumented Paper.
  Five complete locator/block streams are exact and seven measured mutable-surface/parallel-order deltas remain
  bounded. The dedicated server pass is intentional: its natural window overlaps Foliage Fortress, and isolating it
  gives both sentinels 169 genuinely fresh base chunks with zero recomputation instead of weakening either gate.
- Inferno Isles at `(-192, -3808)` is the thirty-eighth reviewed window and the second independently locked 3D cave
  profile. Paper's saved quart-biome sections provide an exact 32,768-cell authored Y=-64..60 oracle containing
  16,371 Inferno Isles, 13,203 Pillow Plains Inner, 3,102 Pillow Plains Middle, 67 Pillow Plains Outer, and 25 Salt
  Flats cells; Biomic matches every identity and the ordered SHA-256 signature. A separate fresh-server fixture pins
  all five `inferno_isle.yml` streams and all 18,467 distributor decisions, with every distributor coordinate exact
  against instrumented Paper. Lava-floor and lava-column streams are exact end to end. Chains have exact anchors and
  a 15-cell block delta; glow lichen and glow berries retain bounded mutable-neighbor deltas. The fixture captures
  169 genuinely fresh base chunks with zero recomputation.
- Terracotta Tombs at `(0, 0)` is the thirty-ninth and final reviewed window and the third independently locked 3D
  cave profile. Paper's saved quart-biome sections provide an exact 32,768-cell authored Y=-64..60 oracle containing
  16,280 Terracotta Tombs, 15,067 Fossilized Fenlands, and 1,421 River cells; Biomic matches every identity and the
  ordered SHA-256 signature. A separate fresh-server fixture pins both `terracotta_tombs.yml` decoration streams and
  all 2,087 distributor decisions, with every distributor coordinate exact against instrumented Paper. Glow lichen's
  27-block final stream is exact despite a ten-cell candidate-anchor delta. Glow berries retain bounded 56-cell
  anchor and 190-cell block deltas caused by mutable neighboring surfaces and parallel population order. The fixture
  captures 169 genuinely fresh base chunks with zero recomputation and completes the thirty-nine-concept review set.
- Rocky Refuge decoration retains the v27 giant supports and persistent-leaf web implementation around
  the exact v28 density field. Support locator ranges, taper depths, and vertical reach now use the same
  piecewise authored-Y to 1.7.10-Y transform as the density field. The earlier v27 terrain approximation used
  `rocky_refuge.yml`'s terraced base, three vertically bent cellular lobes, exact 0.95 cellular cutoff
  and fifth-power cap, red slant strata, and `giant_pillar.tesf` supports. `LARGE_LEAF_VINES`
  runs its authored 0.03 below-threshold distributor plus warped `Distance2Div` 3D locator and places
  persistent jungle-leaf webs against opaque terrain. The generic feature runtime mirrors Terra's Bukkit
  `Material.isOccluding()` contract, so non-occluding leaves cannot recursively become anchors. It is not
  substituted with EFR cave vines. A short
  terrain-only Rocky coast ecotone removes the hard Stonegate-Seas/refuge candidate-pool wall without
  blurring stored biome identity; that approximation is no longer the production ground-density path.

## Original production-variant mapping

| Upstream file | Upstream/Biomic identity | Executed family/stage |
| --- | --- | --- |
| `abyssal_ocean.yml` | `ABYSSAL_ALLEYS` | Abyssal ocean |
| `arch_oceans.yml` | `STONEGATE_SEAS` | Ocean arches |
| `arid_arboretum.yml` | `ARID_ARBORETUM` | Arid pillars |
| `badlands_balconies.yml` | `BADLANDS_BALCONIES` | Terrace mountains |
| `bamboo_basin.yml` | `BAMBOO_BASIN` | Basin mountain |
| `barren_tilted.yml` | `BARE_BOULDERFIELDS` | Tilted plateau |
| `black_forest.yml` | `BLACK_FOREST` | Mountain clearings |
| `canyon_arid.yml` | `SANDY_SPLITS` | Cross-province canyon |
| `canyon_frozen.yml` | `ICY_INCISIONS` | Cross-province canyon |
| `carving_creaks.yml` | `CARVING_CREAKS` | Canyon |
| `cave_jungle.yml` | `VINE_VAULT` | Stored 3D cave; EFR moss/vines |
| `dinosaurs.yml` | `FOSSILIZED_FENLANDS` | Segmented lowland |
| `frosty_fingers.yml` | `FROSTY_FINGERS` | Frozen dunes/fingers |
| `frozen_arch_ocean.yml` | `ARCTIC_ARCHES` | Frozen ocean arches |
| `frozen_fungi.yml` | `FROZEN_FUNGI` | Tilted plateau |
| `frozen_vistas.yml` | `MOUNTAIN_MIRRORS` | Terrace mountains |
| `gloomy_gorge.yml` | `GLOOMY_GORGE` | Eroded valley mountains |
| `high_mountains.yml` | `ALPINE_ASCENDANCY` | Alpine |
| `inferno_isle.yml` | `INFERNO_ISLES` | Stored 3D cave islands; basalt/tuff/EFR magma, no netherrack |
| `jungle_vistas.yml` | `CANOPY_CASCADES` | Terrace mountains |
| `lush_loops.yml` | `LUSH_LOOPS` | Volumetric loop mountains |
| `marine_monoliths.yml` | `MARINE_MONOLITHS` | Coast monoliths |
| `mesa_monuments.yml` | `MESA_MONUMENTS` | Arid pillars |
| `murky_marshlands.yml` | `MURKY_MARSHLANDS` | Marsh terraces |
| `oasis.yml` | `OASIS` | Oasis basin |
| `pillow_plains_inner.yml` | `PILLOW_PLAINS_INNER` | Nested pillow inner |
| `pillow_plains_middle.yml` | `PILLOW_PLAINS_MIDDLE` | Nested pillow middle |
| `pillow_plains_outer.yml` | `PILLOW_PLAINS_OUTER` | Nested pillow outer |
| `plateao_inner.yml` | `FOLIAGE_FORTRESS_INNER` | Nested fortress inner |
| `plateao_middle.yml` | `FOLIAGE_FORTRESS_MIDDLE` | Nested fortress middle |
| `plateao_outer.yml` | `FOLIAGE_FORTRESS_OUTER` | Nested fortress outer |
| `redwood_forests.yml` | `TALL_TIMBERLAND` | Mountain clearings |
| `rocky_refuge.yml` | `ROCKY_REFUGE` | Rocky pillars |
| `sakura_streams.yml` | `SAKURA_STREAMS` | Summit streams |
| `scarlet_sanctuary.yml` | `SCARLET_SANCTUARY` | Tilted plateau |
| `secluded_valley.yml` | `SECLUDED_VALLEY` | Nested secluded inner |
| `secluded_valley_outer.yml` | `SECLUDED_VALLEY_OUTER` | Nested secluded outer |
| `sinkhole_forest.yml` | `SINKHOLE_FOREST` | Dormant upstream profile; active preset omits sinkhole stage |
| `sinkhole_frozen.yml` | `SINKHOLE_FROZEN` | Dormant upstream profile; active preset omits sinkhole stage |
| `sinkhole_jungle.yml` | `SINKHOLE_JUNGLE` | Dormant upstream profile; active preset omits sinkhole stage |
| `sinkhole_outer.yml` | `SINKHOLE_OUTER` | Dormant upstream profile; active preset omits sinkhole stage |
| `snowy_spires.yml` | `SNOWY_SPIRES` | Snow pillars |
| `stone_savanna.yml` | `WATERY_WILDS` | Lowland hills |
| `terracotta_tombs.yml` | `TERRACOTTA_TOMBS` | Stored 3D cave; terracotta surfaces |
| `tundra_tracks.yml` | `TUNDRA_TRACKS` | Lowland hills |
| `verdant_valleys.yml` | `VERDANT_VALLEYS` | Verdant valley |
| `vertical_vistas.yml` | `VERTICAL_VISTAS` | Terrace mountains |
| `white_wallows.yml` | `WHITE_WALLOWS` | Eroded valley mountains |

Tests enforce all 85 resolved profiles, exact native signatures, exact pipeline/extrusion output,
scaled surface parity, surface/vertical routing, volumetric density, and fresh Paper-semantic decoration
fixtures for the complete thirty-nine-concept review set through Terracotta Tombs. `OrigenParityReportTest`
writes the Biomic half of the deterministic comparison under `build/reports/origen-parity/`.
`TerrainPreviewTest` continues to write
visual review atlases. Vanilla structures, EFR-owned ores, deliberate 1.7.10 world-height compression, and the
explicitly measured mutable-neighbor/parallel-order deltas remain compatibility boundaries rather than byte-identical
upstream output; the thirty-nine-profile Origen terrain and decoration review itself is complete.
