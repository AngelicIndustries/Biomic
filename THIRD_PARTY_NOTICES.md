# Third-party notices

## Cave and Nether source ports

- Tartarus: [Jason-Ding19/Tartarus](https://github.com/Jason-Ding19/Tartarus), revision
  `0f4d435c8eec4287a7176c895ba14667053d1cf5`, GPL-3.0-only. Imported terrain and
  surface/feature/structure rules, original credit and full source license text are retained in
  `assets/biomic/tartarus/terrain-density.json`. The source credits the Terra
  default pack and its contributors.
- UltimateCavesMod: [kotmatross28729/UltimateCavesMod](https://github.com/kotmatross28729/UltimateCavesMod),
  revision `d687b27a9f0a950d72a97aed691470fe8bd08684`, MIT; copyright 2021 Johann
  Bernhardt. Selected noise kernels are relocated into `internal.ultimatecaves`;
  source hashes and the license are retained in `assets/biomic/ultimatecaves`.
  FastNoise retains its MIT copyright notice for Jordan Peck. The Biomic adapter
  uses bounded pre-decoration terrain inputs instead of the upstream mod's
  global replacement hooks, and corrects Worley grid-edge smoothing.
- YUNG's Better Caves2.0.4: revision
  `e71f869808fc139773b75125919a65925a983a77`, LGPL-3.0. Chamber and
  cubic/simplex passage kernels retain upstream source-order semantics; the
  regional composition and immutable HA512 host adapters are Biomic changes.
  The source water/lava/null-region selector retains its exact two-dimensional
  simplex noise, float CDF and chunk RNG order. Publishing contained liquid
  basins over immutable cave masks, with rock barriers at unsafe openings and
  opposite-fluid boundaries, is a Biomic adaptation rather than whole-carver parity.
  Relocated FastNoise slices retain Jordan Peck's MIT notice, and the exact
  long-seeded OpenSimplex2S wrapper retains K.jpg's attribution. Applicable
  notices and the complete LGPL text ship under `META-INF/licenses`.

## Ecology ports: TerraFirmaCraft, Tropicraft, Mariculture

Textures of the plants and shells Biomic ports for its sea and cave mouths, under `assets/biomic/textures/blocks/ports`
(`tfc_*`, `trop_*`, `mari_*`). They are unmodified except that animation strips are cut to their first frame and
indexed images are stored as RGBA. Each keeps its own licence; the full texts ship under `META-INF/licenses`.

- TerraFirmaCraft: [TerraFirmaCraft/TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft), revision
  `502726a0af751dc6bbd9482d67d764e22d33ea12`, EUPL-1.2 (`EUPL-1.2-TerraFirmaCraft.txt`): clam, mollusk, mussel,
  sea urchin (and its spikes), driftwood, seaweed and guano, with their groundcover models (element boxes, under
  `assets/biomic/models/ports/tfc_*.json`, unchanged except for texture names); turtle grass and manatee grass;
  sargassum, red algae, gutweed and sea palm; barnacles; cobblestone, elegant sunburst and reindeer lichen.
- Tropicraft: [Tropicraft/Tropicraft](https://github.com/Tropicraft/Tropicraft), revision
  `5deb0e2b8fc44325a9baf08546f6a42f29d456d0`, MPL-2.0 (`MPL-2.0-Tropicraft.txt`): frox conch, rube nautilus, pab and
  solonox shells, turtle shell; fern and sickle seagrass; matted eel grass, fern and sickle seagrass (top and side);
  coral sand; mud with pianguas.
- Mariculture: [joshiejack/Mariculture](https://github.com/joshiejack/Mariculture), branch `1.7.10-1.2.4`, revision
  `363de5c39b068e65b21f5bc4b62fc8a115b0df6a`, MIT, copyright 2014, 2015 joshiejack (`MIT-Mariculture.txt`): pink,
  orange, blue, purple, yellow, red and white corals; the oyster (drawn with Biomic's own small model).

## Minecraft artwork

Biomic ships none of Mojang's textures or sounds. Some of its blocks and particles use Minecraft's own textures (big and
small dripleaf, spore blossom, powder snow, rooted dirt, hanging roots, mushroom stem, the living and dead corals, kelp,
seagrass, sea pickle, ochre froglight, the Abyssal ruins' chain, copper, lightning rod, iron trapdoor and stone, the
grass top and pink petals stem some Regions Unexplored blocks reuse, large bamboo leaves, and the big_smoke particle
frames). On the client, MCLib's Asset Director, which Et Futurum Requiem includes, downloads Minecraft 1.20.4's client
jar from Mojang at the first launch, and `client.VanillaTextures` serves those textures from it under Biomic's names.
Biomic's renderers for these blocks are its own code; their geometry follows the corresponding vanilla block models.
Hanging mangrove propagules, sculk sensors, can-summon shriekers and sculk veins use the textures Et Futurum Requiem
provides.

## GTNewHorizons ExampleMod1.7.10

Biomic's initial Gradle scaffold is adapted from [GTNewHorizons/ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10), inspected at commit `33f0cf760939442696eb0350790f815688b686f6`.

MIT License

Copyright (c) 2021 Johann Bernhardt

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Regions Unexplored

Biomic adapts biome data and assets from
[ApolloUnknowndev/RegionsUnexplored](https://github.com/ApolloUnknowndev/RegionsUnexplored), pinned at
commit `f5dfe4eea916cb7ccb4990faebc952459dad5f8b`. Every imported or adapted file is recorded in
`docs/provenance/regions-unexplored-imports.md`.

MIT License

Copyright (c) 2026 Apollo

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Gardens of the Dead

Biomic backports the two Nether biomes of
[ochotonida/gardens-of-the-dead](https://github.com/ochotonida/gardens-of-the-dead) (Soulblight Forest, Whistling
Woods): their nine intrinsic blocks, textures, whistle sounds and generation rules, from commit
`a2fc815aa5cf075bfbaa945e25a2b8413064ac17`. MIT License, Copyright (c) 2022 Wouter Pauwels; the verbatim license
ships as `META-INF/licenses/gardens-of-the-dead-MIT.txt` and the provenance is in
`assets/biomic/gardens/PROVENANCE.md`. Its two music tracks (Soulblight Forest and Whistling Woods, added upstream
as "custom music by Firch") ship under the same licence, re-encoded at a lower bitrate.

## Seismic

Biomic embeds a minimized and package-relocated copy of
[PolyhedralDev/Seismic](https://github.com/PolyhedralDev/Seismic), version `2.5.7`, source revision
`17e98bdd5d18e3e469fc49faddc53265ae451d2e`. The upstream binary has SHA-256
`a3a371d98f9cbc4d9d8538281240ded7ea5f2f8592daa0ef6d5d186a0a33807a`.

Seismic is licensed under the GNU Lesser General Public License, version 3. The embedded classes are
modified mechanically by package relocation, unused-class minimization, and JVM Downgrader's Java
21-to-Java-8 bytecode transformation. The corresponding upstream source is available at
<https://github.com/PolyhedralDev/Seismic/tree/v2.5.7>; Biomic's source and build scripts provide the
application code and transformation instructions needed to rebuild and relink the combined work.
The upstream `LICENSE_seismic` is retained in the distributed jar.

## SLF4J API

Biomic embeds and package-relocates SLF4J API `2.0.17`, a runtime dependency of Seismic. The
upstream `META-INF/LICENSE.txt` is retained in the distributed jar.

Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Terra runtime subset and Paralithic

Biomic adapts Terra's v2 biome-provider array/stage algorithms and chunk-generator-noise-3d
interpolation/raster algorithms in `TerraV2BiomePipeline` and `TerraPackChunkRasterizer`.
The same pinned runtime's ore placement, schematic decoding and feature selector semantics are
adapted in `TerraPackOre`, `TerraPackSchematic` and `TerraPackFeatureSelectors`, with bounded
state-string transactions instead of game-platform writes. These common Terra algorithms are MIT-licensed;
their license below remains applicable. Host fitting and projection are Biomic-specific adapters.
Original algorithm oracles are relocated under
`src/test/java/industries/angelic/biomic/testterra/{pipeline,pipelinelegacy,raster}/` (test-only,
not part of the shipped mod), with its source hashes and MIT license. Package
and noise imports are adjusted; diagnostic logging is removed. The fixture uses
a minimal biome identity and separate config adapters, not copied game bindings.
All use revision `3745c3e947c8b1f955e9c934f12ea402ef29907f`.

Biomic includes a Java-8-compatible subset of
[PolyhedralDev/Terra](https://github.com/PolyhedralDev/Terra), revision
`3745c3e947c8b1f955e9c934f12ea402ef29907f`, corresponding to the Terra 6.4.2 runtime used to load
the pinned Origen pack. The subset contains the required Terra noise samplers, TerraScript parser,
and utility contracts. Changes consist of pruning unrelated platform/configuration code, package
relocation where needed, Java 8 compatibility changes, and adapters from Terra's platform contracts
to Biomic's Forge 1.7.10 worldgen runtime. Adapted source files retain their upstream copyright
headers.

Biomic also embeds and package-relocates [Paralithic](https://github.com/PolyhedralDev/Paralithic)
`0.7.1`, the expression compiler version used by Terra 6.4.2, together with its ASM runtime
dependency. The following MIT license applies to the Terra subset and Paralithic.

Copyright (c) 2020-2023 Polyhedral Development

Copyright (c) 2020-2021 dfsek

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

### ASM 9.5

Paralithic's embedded, package-relocated ASM `9.5` runtime is licensed under the BSD 3-Clause
License.

ASM: a very small and fast Java bytecode manipulation framework

Copyright (c) 2000-2011 INRIA, France Telecom

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted
provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions
   and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, this list of
   conditions and the following disclaimer in the documentation and/or other materials provided
   with the distribution.
3. Neither the name of the copyright holders nor the names of its contributors may be used to
   endorse or promote products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Terra Overworld Config

Biomic's terrain equations are adapted from
[PolyhedralDev/TerraOverworldConfig](https://github.com/PolyhedralDev/TerraOverworldConfig), revision
`7d5a5c8d1eea9dee83077e3746efda6c74e56888`, licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

Copyright (c) Polyhedral Development and TerraOverworldConfig contributors.

The adapted material remains in Biomic's non-Origen and RU terrain components and includes the
elevation, land, rocky-overhang, eroded-coast, ocean, and river sampler equations. Changes include
translation from Terra YAML expressions to Java, a Minecraft 1.7.10 height envelope, and integration
with Biomic's macro, 3D-biome, EFR surface, cave, river, and decoration stages. The exact Origen path
described below instead executes Origen's pinned Terra graphs directly. A per-file mapping and
detailed change notice is in `docs/provenance/terra-terrain-port.md`.

## Origen

Biomic adapts the complete 48-profile production catalogue and the distribution,
terrain-density, biome-pipeline, palette, feature, structure, ore, and cave-biome configuration from
[Rearth/Origen](https://github.com/Rearth/Origen), revision
`c01cd79717ed1c1172f615ebedd90db37119ff25`.

Origen 1.0.0 identifies its authors as Astrash, Sancires, Aureus, and Rearth and is licensed under
[Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).

Biomic changes the execution platform from Terra 6.4.2 to Forge 1.7.10, maps Y=-64..319 into the
0..255 world (or, with Hohen Angst's 512-high worlds, keeps Origen's full 384-block range), resolves modern block states through EFR and Biomic backports, and executes a generated
runtime-neutral manifest through its Forge adapters. Terra's sampler, expression, biome-pipeline,
palette, feature-distributor, locator, TerraScript structure, and ore semantics are retained where
the legacy platform permits; explicitly documented admission and boundary rules keep tall authored
features intact inside the compressed height and biome envelope. No endorsement by the Origen
authors is implied. The complete per-profile mapping, generated-manifest inventory, transformation
policy, and parity measurements are in `docs/provenance/origen-port.md`.

Biomic also modifies Origen's look (CC-BY-4.0 requires changes to be indicated):
`assets/biomic/origen/palette-overrides.json` (written by `tools/origen_overrides.py`) replaces the
palettes that picked blocks by white or gaussian noise, gives Stonegate Seas and Black Forest new surface
and slant palettes, makes cliff boulders stone only and keeps rock deposits off sky-exposed faces. The biome
pipeline places canyons and inner sinkholes by their cell's climate band, moves mangroves to warm coasts
and four profiles to the band their look belongs to, and filters hosted habitats by climate. Its biome
cells are 480 blocks (Origen 350) with patchwork splits at 1.4 times that frequency (Origen 2.0), its
temperature noise is scaled by 1.0 (Origen 0.4) with height cooling a cell by one band at most, and the
biome lookup takes a further slow ±40-block warp and a ±3-block wobble. Frozen Ocean, Wooded Badlands'
low ground, the rocky slant palettes and the deep dark sea floor have new palettes. Palette slant is
measured over ±2 blocks of the continuous density field (Terra ±0.55 within the owning chunk), so
slant palettes no longer follow the 4-block interpolation lattice and chunk lines, and a column's
palettes come from the profile up to four blocks away on a smooth noise (biome identity unchanged).
Biomic adds a temperate canyon profile, Fern Fissures, built from Origen's canyon landform and Sandy
Splits' features with its own palettes.

## Aeropelago

Biomic contains a Forge 1.7.10 adaptation of
[astrsh/Aeropelago](https://github.com/astrsh/Aeropelago), revision
`bc525814ba84d961310d1c359a724382c54fd5d4`. Aeropelago 0.1.0 identifies Astrash as its author and
is licensed under GNU GPL version 3.

The adaptation retains Aeropelago's complete density sampler graph, authored constants, grass/dirt/stone palette,
and grass feature stage. Biomic uniformly scales the authored coordinates into the 1.7.10 sky envelope, assigns a
dedicated position-aware biome identity, and adds a feathered open-ocean admission envelope so the source generator
can coexist with Origen terrain. The Aeropelago-derived implementation files are GPL-3.0-only, and the combined jar
containing them is distributed under GPL-3.0. The complete GPL text is included as `LICENSE`; detailed source mapping
and transformation notes are in `docs/provenance/aeropelago-port.md`.

## Alex's Caves

Biomic contains a Forge 1.7.10 adaptation of parts of
[AlexModGuy/AlexsCaves](https://github.com/AlexModGuy/AlexsCaves), revision
`4718f4287c65b810aecb211789b45f54d405a94d` (upstream version 2.0.2). Worlds get the Abyssal Chasm: its terrain,
abyssmarine, muck and boulders, its geothermal vents (behaviour, particles and seven bubbling sounds), tube worms,
anemones, mussels with their growth, and ping-pong sponges. The jar also carries, unused (no world generates them, and
they are not registered), the code and textures of the Chasm's ruin and bone blocks and of the Forlorn Hollows' and
Primordial Caves' blocks. Official project metadata licenses Alex's Caves under
`LGPL-3.0-only`. The copied textures and sounds remain Alex's Caves assets; the LGPL text is included as
`META-INF/licenses/biomic/LICENSE-LGPL-3.0-ALEXSCAVES-ASSETS`, the vent notice as
`META-INF/licenses/ALEXSCAVES-NATURAL-VENT-NOTICE.txt`, and exact per-file hashes are in
`assets/biomic/alexscaves/source-manifest.json`, `abyssal-source-manifest.json` and
`natural-vent-source-manifest.json`.

Changes include translating the raw-generation terrain and intrinsic feature shapes to pure, transactional planners,
adapting block models to the legacy renderer and mapping the negative-Y source envelope into Biomic's height range. No
mobs, spawn entries, Spelunkery Table, loot or progression systems, Candy Cavity, Magnetic Caves or Toxic Caves are
included. No endorsement by the Alex's Caves authors is implied.
