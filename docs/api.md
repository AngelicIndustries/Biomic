# Biomic's API for other mods

Everything in `industries.angelic.biomic.api`. `BiomicApi.VERSION` rises with each hook added; changes are additive.
The hooks exist so a mod (Adventura Arcana Core for the pack, see `biomic-aac-split.md`) can add to Biomic's worlds
without patching Biomic, and every one keeps Biomic's invariant: the same world in any generation order.

The biome API (`BiomicBiomeApi`, `BiomeCatalogue`, `BiomeTags`, `BiomeKeys`) is described in `biome-api.md`; version 2
added the catalogue, the one-call lookup and the column spans there.

## Foreign generators (version 1)

`ForeignGenerators.standDown(className, scope)` makes another mod's `IWorldGenerator` stand down in Biomic's chunks:
Forge's world-generator pass (`GameRegistry.generateWorld`) skips it there and runs it everywhere else. Name the class
the mod registers with `GameRegistry.registerWorldGenerator`, by its binary name; a name whose mod is not installed
never matches. Register at preInit or init. The scope is `OVERWORLD` (Biomic's overworld, 256 or 512 high) or
`EVERY_DIMENSION` (the Tartarus Nether too); a class registered twice keeps the wider one.

Use it when your mod places that generator's blocks itself, through Biomic, so each plant has one provider. Each
generator has its own seeded random in Forge's pass, so skipping one changes nothing else.

Biomic registers its own list in `compat/flora/ReplacedGenerators`.

## Block substitutes (version 1)

`BlockSubstitutes.register(block, scope, substitute)` puts another block in place of `block` in Biomic's worlds,
metadata kept, whoever writes it: Biomic's population, structures, other mods' generators and, with scope `ALWAYS`,
players. With scope `GENERATION` only writes made while a chunk generates are replaced, so players can still place
the original. The substitute is asked per column (`(chunk, localX, localZ) -> Block`, null keeps the block), so it can
follow the biome. Register at init or postInit, once both blocks exist; one substitute per block.

Adventura Arcana Core registers vanilla's vine to PMP's ivy and, in generation, HarvestCraft's generic bamboo shoot
crop to its Moso shoot.

## Ecology overlays (version 1)

Biomic's ecology data lives in `assets/biomic/ecology/`: `palettes.json` (plants per habitat and tier, flower patches,
plant forms, food, the sky's plants), `leaf-decor.json` (what replaces Origen's leaf-block decor per feature and
profile, and the species of Origen's bamboo), `modern-features.json` (`"placed"`: per placed feature of the
modern habitats, the configured feature it places instead; its placement and random stay) and `decor.json` (the blocks
of Biomic's own cave and sea decoration by slot: the moss on cave walls, the glowshroom, the sea floor's algae and
seagrass, the fresh-water plants, the reef's corals; an empty slot places nothing, and where a slot is placed stays
Biomic's). `EcologyOverlays.register(file, resource)` adds a JSON file of the same
shape from another mod (by convention `assets/<modid>/biomic/ecology/`), at preInit; overlays apply in registration
order, key by key:

- `"key": {...}` merges into the object there (a habitat, a section, a feature's profiles);
- `"key": [...]` adds the entries the list does not have yet, at its end;
- `"key": value` sets a number, string or boolean;
- `"=key": anything` replaces what is there (a list whose order matters, a whole spec);
- `"-key": [...]` takes entries out of the list there; `"-key": true` removes the key.

Biomic's own files name vanilla's, Et Futurum's and Biomic's blocks, and another mod's plants only in that mod's own
habitats (SaltyMod's in the salt marsh); a plant whose mod is missing is skipped. Adventura Arcana Core's overlays add
the pack's: other mods' plants in Biomic's habitats, plant forms, flower patches, leaf decor, cave and sea decor, food,
bamboo and Harvest the Nether in Tartarus. Biomic registers none of the pack's itself.

## Painted sprites (version 1, client)

`api.client.PaintedSprite.register(iconRegister, name, painter)` registers a block-atlas sprite that is painted when
the atlas is stitched, from textures other mods install: the painter reads them through a `SpritePixels.Source`
(16x16 ARGB, null when absent) and returns the sprite's pixels. A mod can then show another mod's plant in another
colour without shipping that mod's art. `SpritePixels` has the colour arithmetic Biomic paints its food with
(luminance, scale, multiply, mix, HSB) and touches no client classes.

## Plant layer (version 1)

The ecology palettes' `food` section names plants per habitat by kind: `crops`, `bushes` and `shoots` (drifting
patches on 40, 48 and 24-block grids), `trees` (groves of one tree, 14 blocks apart in orchards, 32 elsewhere),
`vines` (hanging from trunks) and `nests` (one on a trunk side under the canopy). Its `nether` lists name, per
Tartarus biome, Nether plants (patches over every cave floor on nether ground; a `Patch` may refuse a ground) and
Nether trees (about one per six chunks of their biomes). Biomic decides where, from hashes of
the world seed and the position; `PlantLayer` resolvers registered by the mod that owns the plants decide what a name
grows as: a `Patch` (block, metadata per stage roll, sand, fairy rings), a `Tree` (a sapling-style generator, sand, how
its fruit ripens), a vine block, or the one `Nest` (state and tile entity data). Resolvers are asked in registration
order and a name nobody answers is skipped. Register at init or postInit.

A block no Biomic codec names is written as it stands (registry name and metadata), so a resolver may name any mod's
block. Adventura Arcana Core registers HarvestCraft's crops and fruit trees, its food blocks and bamboo shoots, and
EFR's bee nests.
