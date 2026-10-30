# Gardens of the Dead R7 intrinsic slice

Source: `gardens-of-the-dead` commit `a2fc815aa5cf075bfbaa945e25a2b8413064ac17`.

License: MIT; the verbatim upstream license is packaged at
`META-INF/licenses/gardens-of-the-dead-MIT.txt`.

This backport includes the nine blocks intrinsic to the two Nether biomes and their source textures and
whistle sounds. The much larger decorative construction families (planks, stairs, slabs, doors, signs,
fences, mosaics and related recipes) are deliberately deferred; they are not world-generation dependencies.

Biome keys are Biomic-owned (`biomic:soulblight_forest` and `biomic:whistling_woods`) so the backport does
not impersonate the upstream mod namespace. Profiles preserve the authored source feature order and the
0..127 source-height domain. Upstream mob additions and spawn tables are deliberately not ported.

The Soulblight floor rule uses vanilla 1.21.1's `minecraft:patch` normal-noise parameters (first octave
`-5`, amplitudes `[1]`) with the pinned Nether legacy random source. That field is owned by the Gardens
surface adapter because the separately hashed RU surface graph does not reference or carry it.

The two biomes' music tracks (`sounds/gardens/soulblight_forest.ogg`, `sounds/gardens/whistling_woods.ogg`, upstream
`sounds/music/`, added as "custom music by Firch") ship under the same MIT licence, re-encoded with libvorbis at
quality 2 (about 64 kbit/s). Biomic's `music.gardens.*` events weigh them against C418's Nether tracks as upstream's
`music.*` events do (7 to 1 each, the Gardens track at half volume).
