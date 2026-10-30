# Aeropelago port provenance and transformation policy

Source: [astrsh/Aeropelago](https://github.com/astrsh/Aeropelago) at revision
`bc525814ba84d961310d1c359a724382c54fd5d4` (2022-07-25).

The upstream root `LICENSE` is GNU GPL version 3. Aeropelago identifies Astrash as the pack author. The faithful
Biomic port is therefore recorded as an adapted work, not anonymous inspiration. Its implementation files are
GPL-3.0-only, while original Biomic code is available under MIT or GPL-3.0. The combined jar containing the port is
distributed under GPL-3.0 and ships the license text, source and modification notices, and corresponding source.

## Complete upstream inventory

Aeropelago 0.1.0 is a compact Terra configuration pack:

- `pack.yml`: a `NOISE_3D` generator, one `PLAINS` biome, one `flora` feature stage, an OpenSimplex2 humidity sampler,
  and the pack's piecewise linear interpolation helper;
- `customization.yml`: island scale, upper and lower height envelopes, undercarriage translation, flatness, taper, and
  density threshold;
- `biomes/terrain.yml`: the complete five-octave probability-normalized FBM upper-mass field and three-octave
  probability-normalized ridged undercarriage mask;
- `biomes/plains.yml` and `palettes/grass.yml`: one Plains identity with one grass, two dirt, then stone layers;
- `features/grass.yml`, `structures/tall_grass.tesf`, and `structures/blank.tesf`: the complete flora pass.

There are no additional upstream biome families, trees, ores, structures, blocks, or assets to omit. In particular,
the village visible in a modern reference world is vanilla structure placement over Aeropelago terrain; it is not an
Aeropelago-authored feature. Later SkyLands content is a separate project and a separate licensing decision.

## Biomic transformation

Biomic generator v72 retains the accepted generator-v71 `biomic:sky_donut` unchanged. Aeropelago becomes a second dedicated 3D
biome and density source rather than replacing the Overworld or the donut.

The port preserves Aeropelago's sampler graph, constants, Terra 6.4.2 seed behavior, density threshold, palette, and
flora semantics. Its authored vertical envelope is uniformly scaled into Minecraft 1.7.10's available sky band, and
the same scale is applied horizontally so island proportions do not become unnaturally flat. A broad, smoothly feathered remote-ocean zone bounds the
otherwise world-wide source generator so it can coexist with Origen. The unmodified core of each accepted zone must
match an executed modern reference at equivalent source coordinates, and the feather may only close terrain near the
zone perimeter—never cut it with a hard plane.

Every deviation needed for coexistence is tested and documented: vertical projection, remote-ocean admission,
perimeter feather, dedicated ChunkAPI identity, and legacy block-state mapping. The original sky donut keeps an
independent locator and regression fixture so adding Aeropelago cannot move or mutate it.

Generator v74 keeps that GPL-derived density, palette, and grass algorithm isolated, then composes original
Biomic work above it. A broad deterministic habitat mosaic assigns real Origen `LUSH_LOOPS`, `SAKURA_STREAMS`,
`FOLIAGE_FORTRESS_INNER`, and `TALL_TIMBERLAND` identities to parts of a landmass while retaining open Aeropelago
meadows between them. Composite stable keys such as `biomic:aeropelago/origen/sakura_streams` preserve both the
floating host and its Origen ecosystem for surface rules, future JAS spawning, and debugging. A separate decorator
adds matching jungle, cherry, canopy, and mega-pine trees without changing the upstream grass random field. The
same grass algorithm runs after those trees across both meadow and hosted composite keys, avoiding bare habitat
halves without suppressing tree placement.

The v74 fresh-world gate keeps the exact 23,153-column source footprint, zero bare terrain, zero palette mismatches,
and zero exposed ground while independently pinning 2,468 logs, 7,696 leaves, 1,898 short grass, 1,691 double-grass
pairs, 2,627 hosted-habitat grass plants, and no canopy leaves lacking a nearby trunk. Regions Unexplored habitat
patches remain deferred until that port is re-enabled and visually audited.

Ordinary ground villages are part of Biomic's full structure pass, not this licensed port. Generator v74 previews
each deterministic vanilla piece graph and admits it only if the complete expanded footprint fits dry, locally
smooth Origen land. Generator v75 additionally reserves that footprint and a 16-block margin from later surface
decoration, while retaining underground content, and seals snowy blacksmith lava pools locally after decoration. A
future sky-village policy must separately admit only a sufficiently broad, connected floating plateau; simply
allowing the ground predicate at sky height would produce cut roads and unsupported houses.
