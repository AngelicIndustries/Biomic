# Biomic and Adventura Arcana Core: the split

Decided 2026-10-25. Biomic becomes a worldgen mod that people can install on its own. Everything that is a choice about
other mods' content for the Adventura Arcana pack moves to Adventura Arcana Core (AAC, `../AdventuraArcanaCore`).
AAC depends on none of the mods it patches, Biomic included (decided later that day): its Biomic code is in
`compat/biomic`, content that needs no other mod is on by default, and every feature and patch has a config switch.
Fixes that are purely about world height move to Hohen Angst (`../Hohenangst`).

The user's decisions (2026-10-25): BOP's native habitats stay in Biomic; Village Names gets bare-minimum compat in
Biomic and the rest goes to AAC, which also does any village farming itself (no Biomic farming hook); height-only
fixes go to Hohen Angst; Madake, its shoots and every other bamboo change go to AAC. Later: AAC is modular and
releasable on its own; Village Names' style choice stays in Biomic.

## The two mods

**Biomic** requires AngelLib and Et Futurum Requiem, plus its libraries (EndlessIDs, ChunkAPI, GTNHLib, UniMixins).
Hohen Angst (512-high worlds) and Angelica (rendering) stay optional. With nothing else installed it must look
finished.

**AAC** requires UniMixins and Biomic. It holds the pack's taste: which mod's plant grows where, which of two
duplicate items wins, which generators of other mods stand down, the village crops, the pack's gameplay tweaks.

The rule for any piece of work:

- Biomic's own terrain, biomes, caves, structures, Nether, blocks, sounds and their EFR integration: **Biomic**.
- A mechanism other content plugs into (palettes, layers, registries, hooks): **Biomic API**, used by Biomic itself
  first.
- Compat that keeps Biomic correct when another mod is installed (that mod would otherwise break Biomic's terrain,
  determinism or rendering): **Biomic**, optional and guarded.
- A choice about another mod's content, or a link between two other mods: **AAC**. The test: would a player who
  installs Biomic and that mod, without the pack, expect it? If not, it is AAC's.
- A fix that exists only because the world is taller (saplings, structures assuming y < 256): **Hohen Angst**. A fix
  that exists because Biomic moved the sea or the ground stays in Biomic.

## Where things stand (measured 2026-10-25)

- Outside its mixins, Biomic imports no optional mod's classes; it reaches them by registry names, reflection and
  `@Pseudo` mixins. It already starts without them.
- Standalone run (Biomic, EFR and libraries only; no Hohen Angst, so 256 high), the four determinism windows: 2,500
  chunks, no error. Per populated chunk, against the pack at 512:

  | Per chunk | Pack | Standalone |
  |---|---|---|
  | Biomic's own blocks | 432 | 431 |
  | Tall grass / double plants / brown mushrooms | 17.9 / 6.0 / 4.2 | 16.5 / 6.9 / 2.4 |
  | BOP / PMP / SaltyMod / HarvestCraft blocks | 9.5 / 6.1 / 2.6 / 0.4 | none |

  The standalone world is not bare; its gaps are particular habitats and decorations (see phase 4).
- Coupling, by area, with where each part goes:

  | Area | Size | Mods | Goes to |
  |---|---|---|---|
  | Terrain, biomes, Nether, structures, own blocks; the review's removals (starfish, Redstone Caves, cave mycelium) | most of 98k lines | EFR | Biomic |
  | EFR firewall and mixins | 300 lines | EFR | Biomic |
  | Angelica render mixins | 170 lines | Angelica | Biomic |
  | BOP native habitats, `compat/biome` (with the BOP determinism mixins) | 4,245 lines, threaded through 21 terrain classes | BOP | Biomic, optional (decision 1) |
  | SaltyMod salt marsh, hosted like BOP's habitats | `SaltMarsh*`, `HostedMarshTerrain` | SaltyMod | Biomic, optional (the rule: Biomic + SaltyMod expects it) |
  | Nether neighbours (BOP and Netherlicious biomes in Tartarus), their dressing, Netherlicious ambience, music and portal mixins | ~500 lines | BOP, Netherlicious | Biomic, optional (the rule: Biomic + Netherlicious expects its biomes, without two ambiences) |
  | Village Names: ground and paths, road cap and spacing, monuments and elder guardians at Biomic's raised sea | 5 of 8 VN mixins + `world/structure` | Village Names | Biomic, bare minimum (decision 2) |
  | Village Names style choice (Biomic picks a village's type from its site) | `MixinVillageNamesStyle`, `VillageStyle` | Village Names | Biomic (decided 2026-10-25: villages styled after their site are what a Biomic + VN player expects, and Biomic's vanilla villages use the same choice) |
  | Village farming (crop matrix) | new | Village Names | AAC, its own VN mixins (no Biomic hook) |
  | NativeFeatureCapture (capture frames for foreign generators) | 180 lines | none | Biomic (mechanism) |
  | Sapling and tree growth above y 256: Natura (8 growth-height mixins, redwood envelope), HarvestCraft apples (2), their helpers | ~350 lines | Natura, HarvestCraft | Hohen Angst (decision 3) |
  | Food: wild bushes, fruit pods, fruit vines, FoodLayer's HarvestCraft parts, food links, bee nests, orchards, Nether food | ~1,700 lines | HarvestCraft, Harvest the Nether, PMP, EFR | AAC (the painting utility stays in Biomic's API) |
  | Bamboo: PMP species for Origen's bamboo, Madake (name, both Bamboo Forest groups), the ten shoots | ~400 lines + data | PMP, HarvestCraft | AAC (decision 4); standalone Biomic draws Origen's and RU's bamboo with EFR's again, the RU tree under RU's name |
  | Generators told to stand down (PMP, HarvestCraft, Natura crops and berries, SaltyMod flowers, lakes and bee nests, Growthcraft bamboo, Harvest the Nether) | ~300 lines of mixins | those mods | AAC, through one Biomic suppression list; any that guards Biomic's terrain (cascading lakes) stays, checked file by file |
  | Block substitutions (vine to PMP ivy, HarvestCraft shoot crop to Moso shoot) | 90 lines | PMP, HarvestCraft | AAC, through a Biomic substitution registry |
  | Pack tweaks and other mods' bugs: Natura clouds off; SaltyMod's dedicated-server fixes (bee nest icons, NEI, render type) and its reed render pass | ~250 lines of mixins | Natura, SaltyMod | AAC |
  | Flora palettes: PMP plants per habitat, epiphytes, vines, immersed and floating plants, tree mushrooms; Origen leaf decor (BOP ivy and PMP plants in curtains and clumps) | 463 PMP ids in `ecology/palettes.json`, `OrigenLeafDecor` | PMP, BOP, SaltyMod | AAC as overlays; Biomic keeps a vanilla, EFR and own-block baseline |
  | Biomic's own decorations borrowing BOP blocks (ivy, toadstools, stone formations in caves) | scattered | BOP | Biomic switches to own or EFR blocks; AAC may swap BOP's back in by overlay |

## APIs Biomic needs

Each one is first used by Biomic for the behaviour it has today, so the pack's worlds stay identical while the code
moves. All live in `industries.angelic.biomic.api`, with an API version constant; changes are additive.

1. **Ecology overlays.** Palettes, food lists, forms, links, Origen leaf decor and the bamboo species per feature load
   from Biomic's baseline plus overlays that other mods register (`assets/<modid>/biomic/ecology/*.json`, registered
   at preInit). Merge per habitat and per section (extend or replace). Unknown block ids are logged once and skipped,
   as today.
2. **Plant layer.** FoodLayer generalised: drift patches, groves, trunk vines, nests and shoots take block states and
   placement kinds, not HarvestCraft names. AAC registers the content (HarvestCraft crops, shoot blocks, Pam trees).
3. **Generator ownership.** One Biomic mixin on `GameRegistry.generateWorld` skips the IWorldGenerators listed through
   the API in Biomic's chunks. It replaces the per-mod cancel mixins.
4. **Substitutions.** `WorldSubstitutes` as a registry: from block, to block (by column context), scope always or
   generation only.
5. **Late decoration.** A per-chunk decoration callback with the capture-safe physical writer and a seeded random,
   only when AAC needs features of its own, so they keep Biomic's invariant: the same world in any generation order.
6. **Painted sprites.** `FoodSprite` and the painting helpers as a client utility, so AAC paints from installed
   textures as Biomic does and ships no other mod's art.

No village farming hook: AAC mixes into Village Names' farm pieces itself, taking each crop from the world seed and the
farm's position, which keeps the invariant without Biomic.

## Migration order

Every step keeps the pack playable. The gates: unit tests in both repos; grid against shuffled generation identical
in the pack profile; a refactor or a move leaves the pack worlds identical to the baseline (moved blocks compared
under a rename map); the standalone run passes.

0. **Baselines** (done 2026-10-25). Commit the debloat work. Record the pack baseline (17 windows at 512, grid order) and the standalone
   baseline (same windows, Biomic, EFR and libraries, with and without Hohen Angst).
1. **Wiring** (done 2026-10-25). AAC depends on Biomic (`implementation("industries.angelic.biomic:Biomic:<ver>:dev")`,
   from mavenLocal while developing: `VERSION=0.0.1-debloat ./gradlew publishToMavenLocal` in Biomic; the Angelic Maven
   for releases). The bench (`--profile`) and the review launcher (`--mods`) have two mod sets: `pack` (everything plus
   AAC's newest dev jar, the default) and `standalone`.
2. **APIs, behaviour unchanged** (done 2026-10-25 but the late decoration hook, which waits until AAC needs one;
   `api.md`). Build the APIs above inside Biomic and route Biomic's current pack behaviour through
   them. Zero-diff gate after each.
3. **Moves**, one area per pair of commits (removal in Biomic, addition in AAC or Hohen Angst), lowest risk first:
   1. height fixes to Hohen Angst (Natura and HarvestCraft growth heights; done 2026-10-25, Hohen Angst 1b89957:
      its `api.PlantedGrowth` keeps the limits inside Biomic's generation frame; Natura's thorn-branch mixin was a
      no-op and went. A growth probe of every Natura and HarvestCraft sapling at y 300 and 420 grew the same before
      and after, and the pack's 17 windows are identical; `run/debloat-template` runs the new jar);
   2. pack tweaks and other mods' bugs (done 2026-10-25: Natura's clouds and SaltyMod's three dedicated-server fixes;
      the pack's 17 windows identical). SaltyMod's reed render pass stays in Biomic: it is how Biomic's own salt
      marsh, with its reeds in water, renders. Each generator stand-down moves with the plants it makes room for
      (HarvestCraft and Harvest the Nether with food, Growthcraft with bamboo, PMP and Natura with the palettes);
      SaltyMod's salt lakes stay stood down in Biomic, which they would cascade and mislight;
   3. substitutions (done 2026-10-25 for the vine: AAC registers PMP's ivy behind its `pmpIvyForVines` switch; the
      shoot crop's swap moves with the shoots). AAC keeps Biomic optional: its Biomic code sits in `compat/biomic`
      and compiles against Biomic's API from the local Maven until the API is published;
   4. food content and the food sections of the palettes (done 2026-10-25 for the overworld food: AAC registers the
      bushes, pods and vines with HarvestCraft, Biomic or not, and with Biomic their plant-layer names, food lists,
      HarvestCraft's stand-downs and EFR's nests; the wild-crop, PMP berry and SaltyMod nest mixins went too. FML
      sends a missing name only to its own mod, so Biomic remaps its old names to AAC's blocks (`MovedBlocks`); an old
      world keeps its 173 bushes and 32 vines. A block no codec names is written as it stands, so a plant-layer
      resolver may name any mod's block. The pack's 17 windows are identical under the rename. Harvest the Nether in
      Tartarus followed: the plant layer gained Nether kinds (patches with a ground test, trees), Biomic keeps the
      placement and AAC the lists, resolvers and stand-down; four Nether windows identical, no late-decoration hook
      needed);
   5. bamboo: PMP species per feature, Madake, shoots; Biomic back to EFR's bamboo and RU's names (done 2026-10-25:
      the shoots, the HarvestCraft shoot swap, Growthcraft's stand-down and the PMP species live in AAC behind three
      switches; the Bamboo Forest's second group is EFR's bamboo again in Biomic's graph, and AAC's overlay of the new
      `modern-features.json` puts Madake back, its placement and random unchanged. The pack's 17 windows are identical
      under the rename; standalone lost only the pack's 84 extra bee nests of move 4, its trees keep theirs);
   6. PMP palettes, forms, leaf decor and tree mushrooms, with the PMP determinism mixin (done 2026-10-25 but the
      mixin: other mods' plants in Biomic's habitats, forms, flower patches, the sky's plants and the BOP and PMP
      leaf decor are AAC overlays behind two switches, with PMP's and Natura's stand-downs. A mod's own plants in its
      own habitat stay in Biomic (SaltyMod's in the salt marsh). The PMP mixin's random redirects stay, as they serve
      Biomic's own captures; its 512 ceiling parts went to Hohen Angst (3b1d70e). The pack's 17 windows are
      identical; standalone now draws vanilla vines and its own flower patches where the pack's named absent plants);
   7. Village Names style choice (decided 2026-10-25: it stays in Biomic, see the table). Biomic's Regions Unexplored
      and Origen biomes gained climate types in the Biome Dictionary (snowy, cold, hot, dry, wet from their own
      temperature and rainfall); the pack's 17 windows are identical with them.
4. **Standalone polish.** Survey the standalone world per habitat (17 windows) for what went missing; give Biomic's
   own decorations own or EFR blocks where they borrowed BOP's; a standalone visual review; a dead-code sweep with
   `tools/deadcode`.
   - The survey (2026-10-25): per surface biome, block counts per column of the pack's and the standalone baseline's
     17 windows, over the 2.46 million columns whose biome is the same in both (so the terrain cancels). No habitat
     goes bare. The pack's hosted BOP habitats are other Biomic habitats standalone (wisteria grove, gloomy gorge).
     On land each habitat keeps its own decoration at the pack's density; where the pack adds PMP flowers and
     HarvestCraft crops, standalone has fewer kinds (orchard, forests, cold steppe, verdant valleys: 8 to 13 in the
     pack, 2 or 3 alone). That is for the visual review to judge. The seas were the gap: PMP's kelp and BOP's corals
     left standalone sea floors at 6 to 17 % of the pack's plants, and rivers without any.
   - Decor slots (done 2026-10-25): Biomic's cave and sea decoration takes its blocks from `decor.json` by slot.
     Biomic's own: vine on mossy and moss-skin walls, RU's blue bioshroom and cave hyssop for BOP's glowshroom and
     miner's delight, its ported algae and seagrass on the sea floor and in fresh water, its own corals on the reefs;
     no giant kelp, stalactites or bones. A slot names a ported plant's variant (`biomic:port_algae:2`). The animated
     kelp and seagrass stay creative-only. AAC's overlay (switch `decorInBiomic`) puts BOP's and PMP's back: the
     pack's 17 windows are identical to 3127ac4. Standalone gains 92k wall vines, 6k bioshrooms, 1k hyssop, 4k algae,
     3k seagrass and 6k coral plants and fans; grid and shuffled are identical. BOP's glowshroom almost never takes
     in the pack (13 in the 17 windows), RU's bioshroom on any stone: standalone's fungal caves glow where the
     pack's did not.
   - Dead-code sweep (done 2026-10-25): `tools/deadcode` (ProGuard's usage report, `DeadCodeCutter`,
     `DeadCodeSmells`) after the moves. They left only the stand-down check for generators that see just the world,
     its frame flag and marker interface; nothing else is unreachable, no config key is unread, and no resource is
     unnamed. The pack's 17 windows are identical.
   - Standalone visual review (2026-10-25, Biomic fe6564a with EFR and ArchaicFix at 512): the user found it
     acceptable as it is, so standalone needs no palettes of its own for now.
5. **Pack decisions.** The Flora Ledger choices (duplicates, tree mushroom matrix, village crops) are built in AAC on
   the APIs.

## Baselines (after the moves, 2026-10-25, revision 3127ac4)

Recorded with `tools/bench/profile_bench.py` in `run/baselines/3127ac4-<profile>-<height>-<order>` (git-ignored; each
has a manifest with the revision, the jar's hash and the classpath). 17 windows of 25x25 chunks, seed 42, 10,625 chunks
each; the pack's AAC is `run/baselines/adventuraarcanacore-7d2b799-dev.jar` (pass it with `--aac`), its Hohen Angst the
review template's (with the planted-growth and PMP height fixes). Compared with `tools/bench/worlddiff.py`:

| Profile | Grid against shuffled | ms per chunk, grid (server / workers) | shuffled |
|---|---|---|---|
| `pack`, 512 | identical (10,368 populated chunks, 0 cells) | 10.4 / 22.8 | 21.9 / 17.8 |
| `standalone`, 512 (Biomic's default config) | identical (10,413 chunks) | 23.0 / 19.6 | 25.4 / 17.3 |
| `standalone`, 256 (no Hohen Angst) | identical (10,368 chunks) | 8.7 / 25.3 | 29.9 / 20.8 |

The pack grid run under another JVM identity-hash sequence (`-XX:+UnlockExperimentalVMOptions -XX:hashCode=3`) is
identical too. The pack's world is the phase 0 baseline's (20195a9, in `superseded-20195a9`) with the moved blocks under
their AAC names; standalone changed on purpose in moves 4 to 6 (see the migration order). Getting to order independence
took six fixes; the rule they follow is in [order-independent-population.md](order-independent-population.md).

Standalone at 512 is slower on the server thread (23 against 10 ms per chunk) because it has no ArchaicFix: vanilla
relights every new chunk's tall columns. With ArchaicFix it takes 9.0 ms (`profile_bench.py --archaicfix`); Biomic's
README recommends it.

## How we work from here

- **Which repo.** By the rule above. When AAC needs something Biomic does not offer, add the smallest general hook to
  Biomic's API first, used by Biomic or covered by a test, then the AAC feature.
- **Testing.** Both profiles on every worldgen change: determinism in both, zero-diff for refactors in `pack`, the
  per-habitat survey in `standalone`. The review server runs `pack` unless asked.
- **Docs.** Biomic's docs describe Biomic and its API. The pack's decisions and their log live in AAC's `docs/`.
- **Commits.** Biomic: as now. AAC: per its AGENTS.md, no co-author trailers, and you approve each message first.
  Hohen Angst: by its own repository's rules, read before the first change there.
- **Licences.** Biomic is GPL-3.0 with GPL-3.0-only ports in it; AAC is LGPL-3.0. Your own code can move to AAC or
  Hohen Angst under their licences; anything derived from GPL-3.0-only sources (Tartarus, Aeropelago) stays in Biomic.
  Everything on the move list is Biomic-original as far as THIRD_PARTY_NOTICES shows; AAC's Madake reuses Regions
  Unexplored's tree, which stays in Biomic, by overlay, not by copy.

## Settled questions

1. BOP native habitats stay in Biomic (2026-10-25); so do the salt marsh and the Nether neighbours, by the same rule.
2. Village Names: bare-minimum compat in Biomic, everything else in AAC; no Biomic farming hook.
3. Height-only fixes go to Hohen Angst.
4. Madake and all bamboo changes go to AAC.
