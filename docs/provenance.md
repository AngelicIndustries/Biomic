# Source and asset provenance ledger

Biomic's imported Regions Unexplored assets and adapted biome data are recorded in the per-file
manifest. The build scaffold is adapted from the MIT-licensed GTNH ExampleMod. Both upstream notices
are preserved in `THIRD_PARTY_NOTICES.md`.

Biomic-original code is dual-licensed under MIT or GPL-3.0-only. The combined jar containing the GPL-3.0-only
Aeropelago port is distributed under GPL-3.0; bundled third-party material remains under the terms recorded below.

| Source | Pinned revision | License evidence | Intended use | Imported files/assets | Required action |
| --- | --- | --- | --- | --- | --- |
| [GTNewHorizons/ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10) | `33f0cf760939` | Root MIT license | Gradle/build scaffold. | Wrapper and build-convention scaffold, adapted metadata layout. | Preserve the MIT notice in `THIRD_PARTY_NOTICES.md`. |
| [ApolloUnknowndev/RegionsUnexplored](https://github.com/ApolloUnknowndev/RegionsUnexplored) | `f5dfe4eea916` | Root `LICENSE` says MIT; README still says ARR; project maintainer confirmed MIT in Discord on 2026-08-16. The clarification and screenshot hash are archived in `docs/provenance/regions-unexplored-license-clarification.md`. | Primary biome/content catalogue, adapted around EFR and the Biomic terrain model. | The eleven completed slices: Clover Plains, Wisteria Grove, Chalk Cliffs, Towering Cliffs, Redwoods, Bayou, Old Growth Bayou, Joshua Desert, Pine Slopes, Grassy Beach, and Eucalyptus Forest; their required block/model data and artwork, including palm and rainbow-eucalyptus content; see the per-file manifest. | Record every imported/adapted file in `docs/provenance/regions-unexplored-imports.md`, verify per-asset provenance, and retain the MIT notice. |
| [Hybrid Beta](https://github.com/Crystalis7/Hybrid-Beta) | `b3b550b00258` | CC BY-NC-ND 4.0 | Independent inspiration only. | None | Copy no code, data, or assets. |
| [WWOO](https://modrinth.com/mod/wwoo) | Not pinned | CC BY-NC-ND 4.0 | Independent inspiration only. | None | Pin the reviewed release before recording design references; copy nothing. |
| [Blooming Biosphere](https://modrinth.com/datapack/blooming-biosphere) | Not pinned | CC BY-NC-SA 4.0 | Visual/compositional reference only unless licensing is revisited. | None | Pin the reviewed release; use original implementation and art. |
| [Tectonic](https://modrinth.com/datapack/tectonic) | `34241bdb35ac` source checkout | Modrinth says MIT; inspected checkout had no obvious license artifact. | Algorithmic inspiration only until evidence is archived. | None | Resolve the exact source/license pairing before copying anything. |
| Modern Biomes O' Plenty | `b418c362b266` inspected; historical `BOP-1.7.10-2.1.x` also inspected | All rights reserved | Architectural reference only: climate/landform matrix, height-profile blending, and explicit terrain variants. | None | Independently implement general concepts; copy no code, tables, data, biome text, names, or assets. |
| [Terra](https://github.com/PolyhedralDev/Terra) / [Terra Overworld Config](https://github.com/PolyhedralDev/TerraOverworldConfig) | Terra `25d510156de0`; Overworld config `7d5a5c8d1eea` | Terra API/core addons MIT; implementations/platforms GPLv3; Overworld config CC BY 4.0 | Terrain sampler architecture and adapted Overworld equations. | Generator-v12 equation base, subsequently composed into `OrigenTerrainGraph`; see `docs/provenance/terra-terrain-port.md`. No GPL implementation/platform code copied. | Preserve CC BY attribution and change notice; audit the closest parent license before any later Terra engine import. |
| [Origen](https://github.com/Rearth/Origen) | `c01cd79717ed1c1172f615ebedd90db37119ff25` | Root CC BY 4.0 license; `pack.yml` credits Astrash, Sancires, Aureus, and Rearth. | Primary generator-v13 terrain/profile catalogue. | All 48 production `biomes/rearth/variants` identities, their equation families, cellular distribution, nested/special stages, palettes, and 3D cave identities are adapted; see `docs/provenance/origen-port.md`. | Retain attribution, source link, license link, and modification notice. Do not describe the port as endorsed. |
| [astrsh/Aeropelago](https://github.com/astrsh/Aeropelago) | `bc525814ba84d961310d1c359a724382c54fd5d4` | Root GNU GPL v3 license. | Faithful port of the complete compact Terra pack as a broad floating-archipelago zone, alongside rather than instead of the accepted sky donut. | Terrain equation, single Plains identity, grass/dirt/stone palette, and grass feature semantics; see `docs/provenance/aeropelago-port.md`. | Fulfilled: GPLv3 text, source/modification notices, corresponding source, GPL-only file markers, and GPL terms for the combined jar are present. |
| [PolyhedralDev/Seismic](https://github.com/PolyhedralDev/Seismic) | `v2.5.7` / `17e98bdd5d18` | LGPL-3.0; upstream `LICENSE` and artifact `LICENSE_seismic` | Exact sampler runtime used by Terra: OpenSimplex2, FBM, and pseudo-erosion. | Minimized Seismic classes are shaded, relocated, and bytecode-downgraded into Biomic. | Retain `LICENSE_seismic`, source/rebuild links, modification notice, and the ability to rebuild/relink the combined work. |

Runtime dependencies are linked rather than copied (minimum versions):

- AngelLib `1.0.0`
- Et Futurum Requiem `2.6.54-GTNH`
- EndlessIDs `1.7.4`
- ChunkAPI `0.8.4`
- GTNHLib `0.11.39`

Everything the jar ships from other projects, with its licence, is listed in `THIRD_PARTY_NOTICES.md`.
