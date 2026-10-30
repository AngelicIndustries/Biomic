# Terra terrain port provenance

Biomic generator version 12 replaced its rejected scalar terrain router with a terrain graph adapted
from Terra's default Overworld pack. Generator version 13 retains that sampler/lattice foundation
inside `OrigenTerrainGraph` and layers Origen's profile catalogue over it. This document records the
v12 inputs, transformations, and license boundaries; Origen changes are recorded separately.

## Pinned sources

- Terra: <https://github.com/PolyhedralDev/Terra>, revision
  `25d510156de02cb77f8fb6ffde276417eb19aea4`.
- Terra Overworld Config: <https://github.com/PolyhedralDev/TerraOverworldConfig>, revision
  `7d5a5c8d1eea9dee83077e3746efda6c74e56888`, CC BY 4.0.
- Seismic: <https://github.com/PolyhedralDev/Seismic>, tag `v2.5.7`, revision
  `17e98bdd5d18e3e469fc49faddc53265ae451d2e`, LGPL-3.0.
- Seismic `2.5.7` binary SHA-256:
  `a3a371d98f9cbc4d9d8538281240ded7ea5f2f8592daa0ef6d5d186a0a33807a`.

Terra is not governed by one license across every module. Its API and core addons are MIT, while its
platform-agnostic implementation and platform implementations are GPLv3. Biomic does not copy or
embed those GPL implementation/platform modules.

## Adapted Overworld Config material

The generator-v12 graph, now incorporated into `OrigenTerrainGraph`, translates the following CC BY
4.0 configuration material:

| Upstream file | Adapted behavior |
| --- | --- |
| `math/samplers/elevation.yml` | Squared FBM hills, mountain mask, pseudo-erosion mountains, flatness mask, detailed elevation, and the authored terrain-height scale. |
| `biomes/abstract/terrain/land/eq_land.yml` | Separate additive 2D elevation and `-y + base` 3D density composition. |
| `biomes/abstract/terrain/land/eq_rocky_overhangs.yml` | Three-octave OpenSimplex2 FBM sampled as `(x, y*3, z)` and multiplied by a mountain mask. |
| `biomes/abstract/terrain/land/eq_eroded_coast.yml` | Bounded vertical coastal erosion band and authored variation/erosion frequencies. |
| `biomes/abstract/terrain/aquatic/eq_global_ocean.yml` | Independently detailed ocean-floor elevation and shelf transition. |
| `biomes/abstract/terrain/land/eq_global_river.yml` and `math/samplers/rivers.yml` | Wide continuous drainage approach, bank, and channel masks integrated with Biomic's existing river stage. |
| `customization.yml` | Base Y semantics and the authored `terrain-height: 230`. |

This is an adaptation, not a byte-for-byte copy of the YAML. Terra's biome pipeline, palettes,
features, structures, and modern block registry are not imported.

## Biomic changes

- Terra biome-pipeline masks are replaced with Biomic continentalness, archipelago, highland,
  island-interior, coast, and river fields so RU/EFR biome ownership remains intact.
- Density is sampled on Biomic's 4x8 lattice and trilinearly interpolated into 1.7.10 chunks.
- The final surface estimate is clamped to Y 20..210, with an additional upper density fade, for the
  0..255 world-height envelope.
- Ocean and island base levels use Biomic sea level 63 and its explicit archipelago shelf/shore
  masks.
- EFR surfaces and ores, Biomic cave carving, stored 3D biomes, structures, and decoration remain
  downstream stages.

## Seismic embedding and relinking

Biomic depends on `com.dfsek:seismic:2.5.7` for the same `OpenSimplex2Sampler`,
`BrownianMotionSampler`, and `PseudoErosionSampler` implementations used by current Terra. GTNHGradle
minimizes and relocates those classes into the Biomic jar, then JVM Downgrader transforms Java 21
bytecode to Java 8-compatible bytecode. GTNHLib supplies JVM Downgrader runtime stubs and is already a
Biomic hard dependency.

Seismic's SLF4J API `2.0.17` runtime dependency is embedded and relocated with it; its MIT license is
retained at `META-INF/LICENSE.txt` and reproduced in `THIRD_PARTY_NOTICES.md`. Seismic's compile-time-
only JetBrains annotations dependency is excluded from the shaded runtime graph.

The distributed jar retains Seismic's `LICENSE_seismic`. The upstream source tag, Biomic application
source, dependency coordinates, repository, and all transformation settings are recorded here and in
the checked-in Gradle files so recipients can rebuild or relink a modified Seismic version.
