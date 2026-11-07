# Block palette

Every block Biomic generates is named by an id from its vocabulary, the modern Minecraft ids its terrain and features
use (`minecraft:deepslate`, `minecraft:crimson_nylium`, `regions_unexplored:icicle`). The block palette decides which
installed block each id places. Biomic's defaults (`assets/biomic/palette/default.json`) use Et Futurum Requiem's
blocks; where one is not installed, the id takes its 1.7.10 block from the vanilla palette (below).
`config/biomic-palette.json` overrides any id.

```json
{
  "blocks": {
    "minecraft:deepslate": "minecraft:stone",
    "minecraft:tuff": ["etfuturum:tuff", "minecraft:gravel"],
    "minecraft:crimson_nylium": "minecraft:netherrack",
    "minecraft:mangrove_log": {"block": "minecraft:log", "meta": "auto"}
  }
}
```

- A value is `"mod:block"` (metadata 0), `"mod:block:meta"`, a list tried in order (the first installed block is used)
  or `{"block": "mod:block", "meta": 3}`.
- Metadata `"auto"` keeps what Biomic computes from the block state (a log's axis, a stair's facing, a crop's age):
  only for a block laid out like the default one. A number is fixed.
- `minecraft:air` places nothing.
- Any id Biomic places can be overridden, vanilla ones (`minecraft:stone`) included, in the terrain and in features.
- Changes apply to chunks generated afterwards.

At every start Biomic writes `config/biomic-palette-reference.json`: every id with an entry, its default, the
override if any and the block in use. It also writes `config/biomic-palette-vanilla.json`, the palette that maps every
default id to a 1.7.10 block (`assets/biomic/palette/examples/vanilla-1.7.10.json`).

## Without Et Futurum Requiem

Et Futurum Requiem is optional. A default whose block is not installed, because the mod is missing or has that block
turned off in its config (`enablePrismarine=false` in `config/etfuturum/blocksitems.cfg`, say), takes its block from
the vanilla palette, exactly as if `config/biomic-palette.json` mapped it there. Biomic logs those ids at startup.
Only an id your own file sets, with none of its blocks installed, stops the game at startup, naming it.

Some of Biomic's own blocks are built on Et Futurum Requiem's (clover, icicles, the Skylands' waterlogged glow lichen
and mangrove roots). They exist only with it installed; their ids (`regions_unexplored:clover`,
`regions_unexplored:icicle`) are in the palette like the others. Floating islands' amethyst geodes come from Et
Futurum Requiem's own generator and are left out without it.

The modern textures and sounds Biomic's own blocks and the Nether's ambience use come through the Asset Director
Biomic ships (MCLib's), with or without Et Futurum Requiem.

## How it works

`world.palette.BlockPalette` loads the defaults and the overrides in pre-init and picks each id's block in post-init,
once every mod registered its blocks. The resolver (`OrigenBlockStateResolver`) places each id through the palette and
reads placed blocks back to ids for Origen's rules: a block the user's palette maps an id onto reads as that id (slime
mapped from deepslate gets deepslate's ores), unless it is a vanilla block Biomic already knows. The terrain's material
tables (`ChunkProviderBiomic`), Alex's Caves' codecs, Regions Unexplored's decorations, the flora palettes and the
cave decor take their blocks from it too. Biomic names Et Futurum Requiem's blocks only by registry name
(`compat.efr.EfrBlock`), and its tile entities, bees, sounds and geode generator only from classes loaded when the mod
is installed (`compat.efr`, `content.efr`).

With the default palette the world is the same as before the palette, cell for cell (the standalone bench's four
windows, at 512 and 256 high, 2026-10-31).
