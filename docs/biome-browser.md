# Biome browser (plan, 2026-10-29)

A developer's screen for every biome Biomic generates or hosts: find it, go there, see it, keep notes on it. It
serves Biomic's development and the pack's (Adventura Arcana Core's overlays, BOP, Netherlicious and the other hosted
biomes show up with the rest). It replaces the old biome browser removed with the strip-down (3b14083), whose server
search no longer fits; the locating comes from the atlas probe that found `docs/biome-atlas-seed42.md`.

## Decisions (user, 2026-10-29)

- **Audience:** a dev tool only. Ops (or creative in single player) open it; players never see it.
- **Layout:** a searchable list with small thumbnails on the left, a large thumbnail and the details on the right.
- **Thumbnails:** a Capture button in the browser hides it and shows a single screenshot button over the clean world
  view; pressing it captures the view (the button and the HUD hidden) as the selected biome's thumbnail.
- **Review notes:** per-biome notes and an "issue here" marker that records the position, exported to a file Claude
  reads.

## The screen

```
+--------------------------------------------------------------+
| [search: forest____]  [Surface][Sky][Caves][Nether] [*] [img]|
+------------------------+-------------------------------------+
| * [thmb] Redwoods      |  +-------------------------------+  |
|   [thmb] Bayou         |  |                               |  |
| * [thmb] Infernal Holt |  |      large thumbnail 16:9     |  |
|   [    ] Pine Slopes   |  |                               |  |
|   [thmb] Alpha Grove   |  +-------------------------------+  |
|   ...                  |  Infernal Holt   RU, Nether         |
|                        |  hot, dry  - brimwood, brimsprout   |
| RU (12) Origen (80)    |  nearest 508 m   [Go] [Next] [*]    |
| BOP (11) Tartarus (12) |  [Capture] [Note] [Issue here]      |
+------------------------+-------------------------------------+
```

- **List:** every biome by display name, with its thumbnail (or an empty frame), a star for favourites and its source.
  Filters: where it lives (surface, sky islands, caves, Nether), favourites, with or without a thumbnail, with notes;
  source counts under the list filter by source. Search matches names and keys.
- **Detail:** large thumbnail, name, key and id, source and pack-only flag, climate (temperature, rainfall, the Biome
  Dictionary's types), the signature plants of its palette, the nearest spot and its distance.
- **Actions:** Go (nearest spot, safe floor, across dimensions), Next (the next spot further out), favourite, Capture,
  Note, Issue here, copy the teleport command.
- **Keys:** a keybinding opens it (unbound by default); in the screen, type to search, arrows move, Enter goes
  (Shift+Enter: Next), Ctrl+F favourites, Esc closes.

## Parts

1. **Catalogue (client).** Built from the biome registry and Biomic's own knowledge: its biome definitions (Regions
   Unexplored, Origen, Tartarus, Gardens of the Dead), the Nether neighbours, the cave biomes of the 3D volume, the
   hosted habitats (BOP, SaltyMod, vanilla) and the sky islands' habitats. Climate from the biome, plants from the
   composed palettes (so AAC's overlays show).
2. **Locator (server).** The atlas probe's search: Biomic's samplers on a grid, no chunk generation, outward from the
   player, for the surface, sky, caves and Nether; on a worker with a time budget, nearest first, then the next ones.
   Go generates the spot's chunk, finds a floor with room to stand, and moves the player there, changing dimension when
   needed. Ops only.
   As built (`BiomeLocator`): one low-priority search thread with its own samplers. The surface and the Nether by column
   on square rings (16 blocks apart, 32 beyond 4096, 64 beyond 16384; out to 32768, the Nether 12000). A surface
   column first answers with its Origen profile (one noise sample; the full lookup takes a dozen), and only a profile
   that can lead to the biome gets the full lookup; a hosted habitat is asked at its 32-block province cells' centres,
   which the rings pass. The sky by island cluster (columns 8 apart, the island's habitat must be the biome's; Sky Donut
   and Aeropelago name island shapes, so any island of that shape), caves by the raw 3D biome volume of every other
   chunk (out to 6144; Regions Unexplored's caves through the Origen caves they replace). The few nearest finds are
   checked on the server's tick in their generated and populated chunk: a column where the biome really is (its surface
   biome or the 3D identity of its floor or seabed, the 3D key for caves, the island's habitat or shape for the sky),
   feet and head free, a floor below (ground below any island; water's surface counts). Next starts from where Go
   started and skips everything within 1024 blocks of the spots already visited. A biome living in several places
   (surface and sky) has its place badges as a switch for where Go looks. A registered biome the world never generates
   (BOP outside the save's selection, the salt marsh without SaltyMod, Nether biomes Tartarus leaves out) is listed
   greyed, with no places; so are Biomic's five registered biomes no generator places (the atlas's "Not found or not
   generated"), which the probe still looks for and would report as found. `-Dbiomic.browserProbe=<file>` (and `-Dbiomic.browserProbeMatch=<regex>`) runs Go for
   every biome and place from 0,0 at server start and writes the table.
3. **Thumbnails (client).** Capture mode as decided above; a 16:9 crop scaled to 384x216, saved as PNG under
   `config/biomic/browser/thumbnails/<namespace>/<biome>.png`, loaded lazily as textures. A pack may ship defaults at
   `assets/<modid>/biomic/thumbnails/<namespace>/<biome>.png`; the user's own capture wins.
   As built (`GuiCapture`, `BrowserThumbnails`): Capture opens a screen over the world with the HUD and the hand hidden,
   the part outside the 16:9 crop dimmed, a Capture and a Cancel button. The movement keys still move the player and the
   right mouse button held turns the view, so the shot can be framed; the button or Enter takes it on a frame that
   draws nothing over the world (read as vanilla's screenshot reads the framebuffer), Esc goes back. A key
   `namespace:path` names `<namespace>/<path>.png`. The list shows a 64x36 copy of each thumbnail, the detail panel the
   full one (the last six kept as textures); right-clicking your own capture there removes it, and the "No thumbnail"
   chip lists only the biomes still without one. `-Dbiomic.browserCaptureTest=<key>` (a client JVM argument) captures
   that biome ten seconds into a world and quits, for checking the capture without hands on the game.
4. **Favourites and notes (client).** `config/biomic/browser/state.json`: favourites, notes, issue markers (biome,
   dimension, position, text, time). Export writes `config/biomic/browser/review.md`, grouped by biome, with ready
   teleport commands, for Claude to read.
   As built: Note writes a line on the selected biome; Issue here writes one at the player's feet, filed under the
   biome there (its 3D identity, so a cave or an island counts as itself; the prompt names it), with the server or
   save, the dimension and the block. Enter saves, Esc drops. The detail panel lists the biome's issues and notes
   (five lines, the rest counted): clicking an issue goes back to it (any dimension, same server), right-clicking either
   deletes it. Every change rewrites review.md at once (no export step): the annotated biomes by name, each with its
   source, favourite flag and thumbnail, its notes and issues in time order with `/tp` lines, then the favourites
   without notes. A "Notes" chip lists the annotated biomes. The screen fits small GUI sizes (480x270 at 1080p, auto
   scale): the filter chips and the buttons wrap, the thumbnail takes about half the panel's height and the details
   below it show as far as they fit. `-Dbiomic.browserScreenTest=<png>` (client) writes a note and an issue as the
   buttons do, saves the screen, and walks away and back to the issue, for checking the screen unattended.
5. **Network.** One request and one answer per action (catalogue extras the client cannot know, locate, go), checked
   for op rights on the server. Off with a config switch for servers that do not want it.

## Order of work

1. Catalogue and screen with search, filters, favourites and the detail panel (no locating yet).
2. Locator and Go/Next, with the dimension change.
3. Capture mode and thumbnails.
4. Notes, issue markers and the export.
5. Polish: keyboard flow, sorting (name, distance, source), remembered filters and selection.
   As built: a "Sort" chip (Ctrl+S) cycles name, source and distance. Distance asks the server for a survey: every
   biome and place searched from where the player stands (the Nether from the point under them), one a task on
   the search thread so a Go waits for one search at most, nothing generated; results stream in, the chip shows the
   progress, the list shows the distances in place of the sources and the detail line "N blocks away". A new survey
   starts only 256 blocks from the last one. Filters, search, selection, sort and each biome's Go place are kept in
   state.json between games (distance comes back as name, since its numbers belong to a spot). Keys: Enter Go,
   Shift+Enter Next, Ctrl+F favourite, Ctrl+P capture, Ctrl+N note, Ctrl+I issue here, Ctrl+K copy key, Ctrl+S sort,
   Home/End; each button's hover shows its key. Seed 42 pack: the survey took 119 s for 204 biome places, and a Go
   during it landed in 2.4 s.

Each step is checked in a review world; worldgen is untouched, so the bench gates only confirm nothing else moved.
