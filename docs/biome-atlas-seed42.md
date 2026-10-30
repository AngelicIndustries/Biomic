# Biome atlas: seed 42, biomic, 512 high

Where every biome lies in one world, so you can teleport to each and look at it.

- **World:** seed `42`, level type `biomic`, 512 blocks high (Hohen Angst loaded), the world the review client (`tools/run_debloat_review.py`) and the bench (`tools/bench/profile_bench.py`) create.
- **Mod set:** the pack (`--profile pack`: Biomes O' Plenty, Plant Mega Pack, SaltyMod, Natura, Netherlicious, Adventura Arcana Core, Village Names, HarvestCraft and the template's other mods). Biomic revision `fe6564a` (branch `debloat`). The notes say where the standalone set (Biomic, Et Futurum Requiem and libraries) differs.
- **How the spots were found:** a temporary debug probe (not part of the repository) inside a pack server asked Biomic's own samplers (no chunk generation) for the surface biome on a 16-block grid over x and z from -32768 to 32768, and for the Nether's biome from -16384 to 16384; it walked every sky-island cluster within 20000 blocks; and, for caves, islands and sea-floor biomes, it generated the terrain of every fourth chunk (a 64-block lattice) out to 6144 blocks (31,050 chunks). For each biome it took the nearest spot (inside the biome, not on its edge) and up to three more at least 2000 blocks apart (1000 for caves). The server then generated each spot's chunk and its neighbours (so trees and plants are in place) and chose a standing spot: two passable blocks over a solid one, where the biome on the F3 line (Biomic's position-aware lookup) is the one listed.
- **How they were checked:** the probe's surface map matched the stored surface biome of every sampled column in the 17 bench windows of `run/baselines/3127ac4-pack-512-grid` (11,250 of 11,250 columns) and of the standalone baseline (11,481 of 11,481). Its terrain survey matched that baseline's saved biome volume and surface biome where they overlap (176 of 176 spots). Every spot below was saved by the server and read back from its region files: surface biome, biome-volume key and the blocks under and at the spot (1,473 of 1,473 pack spots outside the Nether; the Nether section has its own counts). Spots that failed the read-back were dropped.
- **Reading the rows:** `y` is where your feet go: the first open block above the ground, the sea floor for underwater biomes, the cave floor for caves, the island top for sky islands. "m" is the horizontal distance from 0, 0. Teleport commands use the review bot's name; replace it with yours. Nether rows need you in the Nether first, since vanilla `/tp` does not change dimension.

## Overworld surface

### Biomic's own

None of Biomic's own biomes is on the overworld surface in this world. Soulblight Forest and Whistling Woods are Nether biomes (see Nether: Gardens of the Dead). Sky Donut and Aeropelago are island shapes whose islands show the biome they host (see Sky). Temperate Lowlands, Temperate Archipelago, Windswept Highlands, Lush Caves and Estuary are registered but not generated (see the last section).

### Regions Unexplored (ported by Biomic)

| Biome (F3) | Key | ID | Nearest x y z | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Clover Plains | `biomic:clover_plains` | 6004 | 480 197 -1280 (1367 m) | `/tp ReviewBot 480 197 -1280` | -592 201 3296; -3392 163 -944; 3584 199 -48 |  |
| Wisteria Grove | `biomic:wisteria_grove` | 6005 | -2960 214 -1056 (3143 m) | `/tp ReviewBot -2960 214 -1056` | 352 257 3520; 1072 216 -3408; 3546 234 -976 | standalone shows Forest here |
| Chalk Cliffs | `biomic:chalk_cliffs` | 6006 | 6144 237 1344 (6289 m) | `/tp ReviewBot 6144 237 1344` | 6432 253 -1968; 7120 227 -4016; 7280 251 6672 |  |
| Towering Cliffs | `biomic:towering_cliffs` | 6007 | 448 261 3168 (3200 m) | `/tp ReviewBot 448 261 3168` | -3243 191 -1433; 3728 269 -896; -4672 220 -32 |  |
| Redwoods | `biomic:redwoods` | 6008 | -32 242 -192 (195 m) | `/tp ReviewBot -32 242 -192` | 528 256 -2960; -4688 244 208; 2736 198 3857 |  |
| Bayou | `biomic:bayou` | 6009 | -576 198 -2784 (2843 m) | `/tp ReviewBot -576 198 -2784` | -3920 189 -2752; 5904 191 -153; 2128 201 6416 |  |
| Old Growth Bayou | `biomic:old_growth_bayou` | 6010 | 320 196 2016 (2041 m) | `/tp ReviewBot 320 196 2016` | 3632 194 -752; 1808 191 -3908; -3888 193 3648 |  |
| Joshua Desert | `biomic:joshua_desert` | 6011 | -1104 195 2608 (2832 m) | `/tp ReviewBot -1104 195 2608` | -848 248 -3216; -4560 250 2224; -2192 194 -4960 |  |
| Pine Slopes | `biomic:pine_slopes` | 6014 | 464 208 16 (464 m) | `/tp ReviewBot 464 208 16` | 352 193 2288; 912 202 -3504; -3475 191 -2240 |  |
| Grassy Beach | `biomic:grassy_beach` | 6015 | -480 193 2416 (2463 m) | `/tp ReviewBot -480 193 2416` | -3024 193 2768; -2464 193 -5712; 6464 193 -1792 |  |
| Eucalyptus Forest | `biomic:eucalyptus_forest` | 6016 | -3183 200 -4784 (5746 m) | `/tp ReviewBot -3183 200 -4784` | -1437 90 -5919; -1600 198 -8464; -5792 92 6560 |  |
| Alpha Grove | `biomic:alpha_grove` | 6017 | 7104 237 8513 (11088 m) | `/tp ReviewBot 7104 237 8513` | 21584 211 4658; -16048 245 22464; -25232 239 17536 |  |
| Ashen Woodland | `biomic:ashen_woodland` | 6018 | -1712 196 704 (1851 m) | `/tp ReviewBot -1712 196 704` | 1696 254 -7408; -9264 258 7296; -12352 196 7056 |  |
| Autumnal Maple Forest | `biomic:autumnal_maple_forest` | 6019 | -4928 213 1488 (5148 m) | `/tp ReviewBot -4928 213 1488` | 2480 257 -6864; -6528 227 -6272; -8928 209 -6336 | standalone shows Alpha Grove here |
| Bamboo Forest | `biomic:bamboo_forest` | 6020 | -1616 227 1568 (2252 m) | `/tp ReviewBot -1616 227 1568` | -2608 223 -176; -4432 228 1120; 4736 90 -1344 |  |
| Baobab Savanna | `biomic:baobab_savanna` | 6021 | -2336 201 881 (2497 m) | `/tp ReviewBot -2336 201 881` | 464 229 -4544; -1456 196 -6160; 5040 194 6272 |  |
| Blackwood Taiga | `biomic:blackwood_taiga` | 6022 | -480 242 -816 (947 m) | `/tp ReviewBot -480 242 -816` | 3952 251 -304; -3408 248 -3008; 2160 240 -4064 |  |
| Boreal Taiga | `biomic:boreal_taiga` | 6023 | 736 230 -2672 (2772 m) | `/tp ReviewBot 736 230 -2672` | 2912 239 3728; 1376 235 5024; -5215 233 -160 |  |
| Cold Boreal Taiga | `biomic:cold_boreal_taiga` | 6024 | -7280 232 -7024 (10116 m) | `/tp ReviewBot -7280 232 -7024` | -15728 224 8608; -10976 236 -15919; -23312 244 12032 |  |
| Cold River | `biomic:cold_river` | 6025 | -560 191 5008 (5039 m) | `/tp ReviewBot -560 191 5008` | -7824 191 3920; -4720 191 -8192; 7344 191 -8096 |  |
| Dry Bushland | `biomic:dry_bushland` | 6026 | 3408 195 6576 (7407 m) | `/tp ReviewBot 3408 195 6576` | -928 196 10528; -17760 197 272; -1856 197 -18400 |  |
| Fen | `biomic:fen` | 6027 | 32 190 -64 (72 m) | `/tp ReviewBot 32 190 -64` | -3216 195 -399; 272 206 -3536; 2608 195 3264 | on the sea floor, 1 blocks of water (surface y 191) |
| Flower Fields | `biomic:flower_fields` | 6028 | 1376 198 -11264 (11348 m) | `/tp ReviewBot 1376 198 -11264` | -8816 164 7584; -5408 194 10496; -11824 199 -4848 |  |
| Frozen Pine Taiga | `biomic:frozen_pine_taiga` | 6029 | 2624 221 -1088 (2841 m) | `/tp ReviewBot 2624 221 -1088` | 560 224 28464; -24848 242 -18256; -32304 216 1328 |  |
| Fungal Fen | `biomic:fungal_fen` | 6030 | 608 198 -64 (611 m) | `/tp ReviewBot 608 198 -64` | 1280 203 2176; -480 198 3136; 3424 192 -2608 |  |
| Grassland | `biomic:grassland` | 6031 | -1984 210 -1104 (2270 m) | `/tp ReviewBot -1984 210 -1104` | -3680 200 6256; 7776 250 -960; 6272 200 -5552 |  |
| Gravel Beach | `biomic:gravel_beach` | 6032 | 0 193 240 (240 m) | `/tp ReviewBot 0 193 240` | -3280 185 -336; 3191 191 -1200; -4064 197 -2176 |  |
| Highland Fields | `biomic:highland_fields` | 6033 | 1232 219 48 (1233 m) | `/tp ReviewBot 1232 219 48` | 1440 211 2048; 3281 214 -1008; -576 256 4288 |  |
| Hyacinth Deeps | `biomic:hyacinth_deeps` | 6034 | -1209 153 -56 (1210 m) | `/tp ReviewBot -1209 153 -56` | -184 167 -4280; -2040 170 -5048 | on the sea floor, 38 blocks of water (surface y 191); a sea-floor identity: the surface map above it shows the ocean |
| Icy Heights | `biomic:icy_heights` | 6035 | -4576 232 -1280 (4752 m) | `/tp ReviewBot -4576 232 -1280` | -2512 225 5904; -5648 191 3280; -6528 213 -1728 |  |
| Magnolia Woodland | `biomic:magnolia_woodland` | 6037 | -720 226 -608 (942 m) | `/tp ReviewBot -720 226 -608` | 400 251 1216; 48 201 -3200; -3616 197 -1808 |  |
| Maple Forest | `biomic:maple_forest` | 6038 | 480 224 2272 (2322 m) | `/tp ReviewBot 480 224 2272` | 176 214 -3280; -4016 199 0; 4192 236 -896 |  |
| Marsh | `biomic:marsh` | 6039 | 896 191 1440 (1696 m) | `/tp ReviewBot 896 191 1440` | -3808 192 -3966; 1616 191 -7108; -8048 205 -7280 |  |
| Muddy River | `biomic:muddy_river` | 6040 | -1888 178 -576 (1974 m) | `/tp ReviewBot -1888 178 -576` | -624 178 1936; 2896 184 624; -4176 176 -208 | on the sea floor, 13 blocks of water (surface y 191) |
| Old Growth Boreal Taiga | `biomic:old_growth_boreal_taiga` | 6041 | -2624 238 5424 (6025 m) | `/tp ReviewBot -2624 238 5424` | 5840 254 2368; 8080 209 2848; -5984 231 -10703 |  |
| Old Growth Forest | `biomic:old_growth_forest` | 6042 | 432 207 -320 (538 m) | `/tp ReviewBot 432 207 -320` | -2272 211 -528; 4368 208 -1040; -2496 207 3968 |  |
| Old Growth Golden Boreal Taiga | `biomic:old_growth_golden_boreal_taiga` | 6043 | 2656 134 -1408 (3006 m) | `/tp ReviewBot 2656 134 -1408` | 8944 215 -47; 5104 216 -7888; 8816 85 4496 | on a sinkhole floor, far below the land around it |
| Orchard | `biomic:orchard` | 6044 | -3904 218 -7072 (8078 m) | `/tp ReviewBot -3904 218 -7072` | 3472 237 8128; -3056 194 8352; -6656 223 6352 |  |
| Outback | `biomic:outback` | 6045 | -1328 252 2400 (2743 m) | `/tp ReviewBot -1328 252 2400` | -1184 199 -3264; 641 194 -6847; -7472 234 7312 |  |
| Pine Taiga | `biomic:pine_taiga` | 6046 | 2289 222 -1261 (2613 m) | `/tp ReviewBot 2289 222 -1261` | -144 88 -2768; 3376 229 2576; 4928 89 -768 |  |
| Poppy Fields | `biomic:poppy_fields` | 6047 | -5056 161 128 (5058 m) | `/tp ReviewBot -5056 161 128` | 5712 211 1344; 8592 226 3568; 2224 199 -9440 |  |
| Prairie | `biomic:prairie` | 6048 | -16 199 4416 (4416 m) | `/tp ReviewBot -16 199 4416` | 11152 222 3632; 7312 201 -10928; -12560 186 -4960 |  |
| Rainforest | `biomic:rainforest` | 6049 | -4190 234 1280 (4381 m) | `/tp ReviewBot -4190 234 1280` | -2731 192 -4672; 1104 276 -7136; -3616 206 -6608 |  |
| Rocky Reef | `biomic:rocky_reef` | 6050 | -1904 186 2304 (2989 m) | `/tp ReviewBot -1904 186 2304` | -3392 199 944; 128 162 -4256; 4549 191 6326 | on the sea floor, 5 blocks of water (surface y 191) |
| Saguaro Desert | `biomic:saguaro_desert` | 6051 | -128 215 1280 (1286 m) | `/tp ReviewBot -128 215 1280` | -2512 198 1200; -560 250 -3168; 4560 255 -1632 |  |
| Shrubland | `biomic:shrubland` | 6052 | 608 233 -1504 (1622 m) | `/tp ReviewBot 608 233 -1504` | -1040 163 6688; 12960 197 -3776; 14672 160 5392 |  |
| Silver Birch Forest | `biomic:silver_birch_forest` | 6053 | 1360 248 704 (1531 m) | `/tp ReviewBot 1360 248 704` | 256 234 -1856; 112 86 2272; 2752 206 3472 |  |
| Sparse Rainforest | `biomic:sparse_rainforest` | 6054 | 5360 197 -1648 (5608 m) | `/tp ReviewBot 5360 197 -1648` | 2208 168 8272; -5552 207 6800; -2944 92 -8416 |  |
| Sparse Redwoods | `biomic:sparse_redwoods` | 6055 | -1904 200 3888 (4329 m) | `/tp ReviewBot -1904 200 3888` | 2352 200 -4736; -3808 219 -4800; -4224 261 6768 |  |
| Spires | `biomic:spires` | 6056 | 848 207 -176 (866 m) | `/tp ReviewBot 848 207 -176` | 3120 195 288; 1728 208 -2720; 2720 200 2256 |  |
| Tropical River | `biomic:tropical_river` | 6057 | -2544 184 -8528 (8899 m) | `/tp ReviewBot -2544 184 -8528` | -3040 242 8928; 9456 213 -6256; -15152 182 4400 | on the sea floor, 7 blocks of water (surface y 191) |
| Tropics | `biomic:tropics` | 6058 | -1920 191 -80 (1922 m) | `/tp ReviewBot -1920 191 -80` | -992 194 2432; 5760 194 -768; -1680 191 -5840 |  |
| Tundra | `biomic:tundra` | 6059 | -10160 259 23264 (25386 m) | `/tp ReviewBot -10160 259 23264` | -1901 240 -28350; -31760 215 -15872; 19872 243 31056 |  |
| Willow Forest | `biomic:willow_forest` | 6060 | 1184 198 -3584 (3775 m) | `/tp ReviewBot 1184 198 -3584` | -4160 221 512; 2272 226 4368; -3696 245 -4672 |  |
| Windswept Maple Forest | `biomic:windswept_maple_forest` | 6061 | 672 246 1520 (1662 m) | `/tp ReviewBot 672 246 1520` | -1680 228 -2032; -3744 199 -704; 4640 213 576 |  |

### Origen biome profiles

| Biome (F3) | Key | ID | Nearest x y z | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Abyssal Alleys | `biomic:origen/abyssal_alleys` | 6100 | -240 85 304 (387 m) | `/tp ReviewBot -240 85 304` | -1072 132 -1520; 1744 125 912; -3216 159 704 | on the sea floor, 106 blocks of water (surface y 191) |
| Stonegate Seas | `biomic:origen/stonegate_seas` | 6101 | 704 179 -2032 (2150 m) | `/tp ReviewBot 704 179 -2032` | 1696 178 1808; -2272 176 -1984; -3584 178 16 | on the sea floor, 12 blocks of water (surface y 191) |
| Arctic Arches | `biomic:origen/arctic_arches` | 6102 | 2272 191 32 (2272 m) | `/tp ReviewBot 2272 191 32` | 3456 203 3136; 3792 191 -3168; -1442 191 5015 |  |
| Arid Arboretum | `biomic:origen/arid_arboretum` | 6103 | 2563 191 6980 (7436 m) | `/tp ReviewBot 2563 191 6980` | 7616 187 -4752; 11488 196 1376; 5808 191 -12864 |  |
| Badlands Balconies | `biomic:origen/badlands_balconies` | 6104 | -848 205 -336 (912 m) | `/tp ReviewBot -848 205 -336` | 112 210 1424; -2448 228 1120; -960 201 -2768 |  |
| Bamboo Basin | `biomic:origen/bamboo_basin` | 6105 | -2318 206 -5337 (5819 m) | `/tp ReviewBot -2318 206 -5337` | 5488 199 -2368; -688 213 9360; 2192 194 11264 |  |
| Bare Boulderfields | `biomic:origen/bare_boulderfields` | 6106 | 2576 214 -1232 (2855 m) | `/tp ReviewBot 2576 214 -1232` | 3504 206 1792; 2112 204 -4224; 5904 191 3296 |  |
| Black Forest | `biomic:origen/black_forest` | 6107 | 148 200 -176 (230 m) | `/tp ReviewBot 148 200 -176` | 320 213 1952; -1504 235 -2160; 640 226 -3088 |  |
| Sandy Splits | `biomic:origen/sandy_splits` | 6108 | -1696 201 2960 (3411 m) | `/tp ReviewBot -1696 201 2960` | -4544 201 1824; 4752 201 -1664; -1088 201 -4960 |  |
| Icy Incisions | `biomic:origen/icy_incisions` | 6109 | 928 201 112 (935 m) | `/tp ReviewBot 928 201 112` | 1472 201 -1856; -3952 201 -768; 3680 201 2960 |  |
| Carving Creaks | `biomic:origen/carving_creaks` | 6110 | -1280 185 1264 (1799 m) | `/tp ReviewBot -1280 185 1264` | -3536 238 304; -1296 214 -3632; -416 239 -5680 | on the sea floor, 6 blocks of water (surface y 191) |
| Fossilized Fenlands | `biomic:origen/fossilized_fenlands` | 6112 | 586 191 202 (620 m) | `/tp ReviewBot 586 191 202` | -1360 199 -2000; 544 191 -2640 | standalone shows Verdant Valleys here |
| Frosty Fingers | `biomic:origen/frosty_fingers` | 6113 | 736 191 32 (737 m) | `/tp ReviewBot 736 191 32` | 1472 195 -2288; 2928 209 -48; 3424 192 2176 |  |
| Frozen Fungi | `biomic:origen/frozen_fungi` | 6114 | 1760 212 0 (1760 m) | `/tp ReviewBot 1760 212 0` | 1872 262 -2896; -768 220 5216; 3184 231 -5072 |  |
| Mountain Mirrors | `biomic:origen/mountain_mirrors` | 6115 | 1600 282 -3392 (3750 m) | `/tp ReviewBot 1600 282 -3392` | 400 234 4496; 4688 206 2416; 4464 250 -4576 |  |
| Gloomy Gorge | `biomic:origen/gloomy_gorge` | 6116 | -240 229 16 (241 m) | `/tp ReviewBot -240 229 16` | 928 236 1728; -1888 228 -1872; 480 220 -2800 |  |
| Alpine Ascendancy | `biomic:origen/alpine_ascendancy` | 6117 | 1392 198 -352 (1436 m) | `/tp ReviewBot 1392 198 -352` | -224 223 -1536; -4080 269 -704; 4112 205 1920 |  |
| Canopy Cascades | `biomic:origen/canopy_cascades` | 6119 | -2032 202 1584 (2576 m) | `/tp ReviewBot -2032 202 1584` | -4112 198 1184; -2720 194 -6032; 912 201 -6896 |  |
| Lush Loops | `biomic:origen/lush_loops` | 6120 | -640 196 -7584 (7611 m) | `/tp ReviewBot -640 196 -7584` | 2352 208 8048; 832 200 -8944; -1184 201 10784 |  |
| Marine Monoliths | `biomic:origen/marine_monoliths` | 6121 | 2800 191 3250 (4290 m) | `/tp ReviewBot 2800 191 3250` | -1008 244 -4640; 3616 241 -3552; 5327 165 4127 |  |
| Mesa Monuments | `biomic:origen/mesa_monuments` | 6122 | -752 196 -64 (755 m) | `/tp ReviewBot -752 196 -64` | -1168 205 2032; -2720 199 304; -1408 200 -2800 |  |
| Murky Marshlands | `biomic:origen/murky_marshlands` | 6123 | 608 198 -864 (1056 m) | `/tp ReviewBot 608 198 -864` | 1344 212 1744; -1728 197 -1744; 704 232 -3040 |  |
| Oasis | `biomic:origen/oasis` | 6124 | -2096 192 576 (2174 m) | `/tp ReviewBot -2096 192 576` | -1136 192 2800; -2064 192 -3936; 656 192 -7168 |  |
| Pillow Plains Inner | `biomic:origen/pillow_plains_inner` | 6125 | -3152 195 -784 (3248 m) | `/tp ReviewBot -3152 195 -784` | -784 159 6512; -6208 159 5408; -7504 193 -4368 |  |
| Pillow Plains Middle | `biomic:origen/pillow_plains_middle` | 6126 | -2720 188 -784 (2831 m) | `/tp ReviewBot -2720 188 -784` | -800 197 6464; -6064 201 5408; -4608 199 -7120 | on the sea floor, 3 blocks of water (surface y 191) |
| Pillow Plains Outer | `biomic:origen/pillow_plains_outer` | 6127 | -2721 201 -752 (2823 m) | `/tp ReviewBot -2721 201 -752` | -768 207 6448; -5984 198 5408; -4576 193 -7120 |  |
| Foliage Fortress Inner | `biomic:origen/foliage_fortress_inner` | 6128 | -208 217 6128 (6132 m) | `/tp ReviewBot -208 217 6128` | 2272 248 6032; 3616 244 -5792; -9824 245 -4048 |  |
| Foliage Fortress Middle | `biomic:origen/foliage_fortress_middle` | 6129 | 32 231 6256 (6256 m) | `/tp ReviewBot 32 231 6256` | 2480 247 5952; 1104 252 -10896; 11872 199 -5344 |  |
| Foliage Fortress Outer | `biomic:origen/foliage_fortress_outer` | 6130 | 608 201 240 (654 m) | `/tp ReviewBot 608 201 240` | -192 195 -1984; 624 212 2400; -2192 200 -2128 |  |
| Tall Timberland | `biomic:origen/tall_timberland` | 6131 | 816 215 208 (842 m) | `/tp ReviewBot 816 215 208` | 2320 216 -1120; 1504 218 -3008; 4032 233 736 |  |
| Rocky Refuge | `biomic:origen/rocky_refuge` | 6132 | 272 200 1808 (1828 m) | `/tp ReviewBot 272 200 1808` | -2320 205 -1264; -2112 202 3600; 3456 200 3616 |  |
| Sakura Streams | `biomic:origen/sakura_streams` | 6133 | 816 198 336 (882 m) | `/tp ReviewBot 816 198 336` | -48 226 2640; -2912 217 -1440; -1744 194 4112 |  |
| Scarlet Sanctuary | `biomic:origen/scarlet_sanctuary` | 6134 | -512 212 1968 (2034 m) | `/tp ReviewBot -512 212 1968` | -3152 250 1184; -784 212 -4719; 1488 227 -6560 |  |
| Secluded Valley | `biomic:origen/secluded_valley` | 6135 | 48 210 -1120 (1121 m) | `/tp ReviewBot 48 210 -1120` | 1392 213 2544; 3712 211 -1600; 7168 216 -640 |  |
| Secluded Valley Outer | `biomic:origen/secluded_valley_outer` | 6136 | 144 200 -160 (215 m) | `/tp ReviewBot 144 200 -160` | 208 199 1952; -1552 208 -1664; 592 207 -3088 |  |
| Forest Sinkhole | `biomic:origen/sinkhole_forest` | 6137 | 224 92 -2032 (2044 m) | `/tp ReviewBot 224 92 -2032` | 4512 99 -1488; -5120 191 784; -3152 210 4704 | on a sinkhole floor, far below the land around it |
| Frozen Sinkhole | `biomic:origen/sinkhole_frozen` | 6138 | 2144 90 -2528 (3315 m) | `/tp ReviewBot 2144 90 -2528` | -3344 150 4608; 4272 175 4256; -5408 85 -4288 | on a sinkhole floor, far below the land around it |
| Jungle Sinkhole | `biomic:origen/sinkhole_jungle` | 6139 | -336 117 -3200 (3218 m) | `/tp ReviewBot -336 117 -3200` | 5040 166 -1264; -2368 85 -4640; -705 135 -8384 | on a sinkhole floor, far below the land around it |
| Sinkhole Rim | `biomic:origen/sinkhole_outer` | 6140 | 1792 198 -576 (1882 m) | `/tp ReviewBot 1792 198 -576` | 224 231 -1968; 384 192 3184; 2208 192 -2544 |  |
| Snowy Spires | `biomic:origen/snowy_spires` | 6141 | 848 200 -848 (1199 m) | `/tp ReviewBot 848 200 -848` | 2832 198 208; 1856 211 -2576; -3648 256 -2176 |  |
| Watery Wilds | `biomic:origen/watery_wilds` | 6142 | -640 209 -32 (641 m) | `/tp ReviewBot -640 209 -32` | -1328 191 1856; -2733 191 432; -928 195 -2752 |  |
| Tundra Tracks | `biomic:origen/tundra_tracks` | 6144 | 896 192 -16 (896 m) | `/tp ReviewBot 896 192 -16` | 2880 195 288; 1552 197 -2624; 3136 187 2592 |  |
| Verdant Valleys | `biomic:origen/verdant_valleys` | 6145 | 560 188 -480 (738 m) | `/tp ReviewBot 560 188 -480` | 816 216 1728; -1760 194 -1792; 464 218 -2688 | on the sea floor, 3 blocks of water (surface y 191) |
| Vertical Vistas | `biomic:origen/vertical_vistas` | 6146 | 576 227 -112 (587 m) | `/tp ReviewBot 576 227 -112` | 384 205 1952; -1968 193 -1984; 368 210 -2848 |  |
| White Wallows | `biomic:origen/white_wallows` | 6147 | 1056 236 -432 (1141 m) | `/tp ReviewBot 1056 236 -432` | 752 248 1712; -1920 216 -1824; 384 234 -3280 |  |
| Alpine Ascendancy River | `biomic:origen/alpine_ascendancy_river` | 6148 | 1376 337 -464 (1452 m) | `/tp ReviewBot 1376 337 -464` | -224 225 -1680; -4320 292 -1072; 4144 191 1792 |  |
| Badlands Balconies River | `biomic:origen/bad_balcoonies_river` | 6149 | -944 177 1936 (2154 m) | `/tp ReviewBot -944 177 1936` | -758 191 -3088; -3136 179 2224; 544 174 -4608 | on the sea floor, 14 blocks of water (surface y 191) |
| Beach | `biomic:origen/beach` | 6150 | 128 192 288 (315 m) | `/tp ReviewBot 128 192 288` | -1552 193 -816; -1552 193 1392; 1760 193 -1808 |  |
| Cold Steppe | `biomic:origen/cold_steppe` | 6151 | 1488 215 -800 (1689 m) | `/tp ReviewBot 1488 215 -800` | 3104 193 496; 2240 221 -2976; 3488 197 2496 |  |
| Coral Ocean | `biomic:origen/coral_ocean` | 6152 | -224 186 2304 (2315 m) | `/tp ReviewBot -224 186 2304` | -2000 173 -3168; -256 187 -4160; 1344 162 -5984 | on the sea floor, 5 blocks of water (surface y 191) |
| Deep Ocean | `biomic:origen/deep_ocean` | 6154 | 0 149 336 (336 m) | `/tp ReviewBot 0 149 336` | -1296 153 -1344; 1712 159 1376; 288 150 -2576 | on the sea floor, 42 blocks of water (surface y 191) |
| Desert | `biomic:origen/desert` | 6155 | -800 201 -352 (874 m) | `/tp ReviewBot -800 201 -352` | -928 195 1840; -2592 195 720; -944 193 -2736 |  |
| Golden Desert Spikes | `biomic:origen/desert_spikes_gold` | 6156 | -688 191 144 (703 m) | `/tp ReviewBot -688 191 144` | -800 209 2144; -2736 197 752; -688 221 -3248 |  |
| Flowering Forest | `biomic:origen/flowering_forest` | 6158 | 4432 199 -3616 (5720 m) | `/tp ReviewBot 4432 199 -3616` | 8112 229 -3120; -2928 194 8224; 9191 191 2342 |  |
| Frozen Fungi River | `biomic:origen/frozen_fungi_river` | 6159 | 1664 212 96 (1667 m) | `/tp ReviewBot 1664 212 96` | 1936 191 -2800; -832 212 5168; 2960 240 -5296 |  |
| Frozen Ocean | `biomic:origen/frozen_ocean` | 6160 | 1904 191 1424 (2378 m) | `/tp ReviewBot 1904 191 1424` | 1792 191 -1903; -1168 191 4560; 3648 191 -3920 |  |
| Frozen River | `biomic:origen/frozen_river` | 6161 | 992 181 -32 (993 m) | `/tp ReviewBot 992 181 -32` | 1424 191 -2224; 3040 191 -352; 3168 191 2000 | on the sea floor, 10 blocks of water (surface y 191) |
| Gloomy Gorge River | `biomic:origen/gloomy_gorge_river` | 6162 | -192 230 -48 (198 m) | `/tp ReviewBot -192 230 -48` | 0 243 2256; -2480 189 -1216; 1344 176 -3584 |  |
| Iceberg Ocean | `biomic:origen/iceberg_ocean` | 6163 | 864 191 -1936 (2120 m) | `/tp ReviewBot 864 191 -1936` | 2192 205 1232; 2736 191 -864; 3376 191 -2799 |  |
| Lush Loops River | `biomic:origen/loops_river` | 6164 | -800 185 -7600 (7642 m) | `/tp ReviewBot -800 185 -7600` | 2544 283 8144; -1120 186 10720; 19856 201 5579 | on the sea floor, 6 blocks of water (surface y 191) |
| Mangrove Swamp | `biomic:origen/mangrove_swamp` | 6166 | -1600 179 560 (1695 m) | `/tp ReviewBot -1600 179 560` | -1712 193 2560; -1680 201 -2696; -3600 200 496 | on the sea floor, 12 blocks of water (surface y 191) |
| Mountain Mirrors River | `biomic:origen/mountain_mirrors_river` | 6167 | 1696 191 -3232 (3650 m) | `/tp ReviewBot 1696 191 -3232` | 400 191 4352; -4608 191 -1584; 4480 268 2624 |  |
| Ocean | `biomic:origen/ocean` | 6168 | 544 187 368 (657 m) | `/tp ReviewBot 544 187 368` | -1168 180 -1008; 816 183 -2096; 2448 187 -640 | on the sea floor, 4 blocks of water (surface y 191) |
| Overgrown Cliffs | `biomic:origen/overgrown_cliffs` | 6169 | -672 201 112 (681 m) | `/tp ReviewBot -672 201 112` | -1296 200 2128; -2896 231 272; -1056 291 -2928 |  |
| Palm Beach | `biomic:origen/palm_beach` | 6170 | -176 194 240 (298 m) | `/tp ReviewBot -176 194 240` | 1440 191 1432; -1536 190 -1456; -1824 191 1504 |  |
| Foliage Fortress Inner River | `biomic:origen/plateao_river_inner` | 6171 | 2176 243 6096 (6473 m) | `/tp ReviewBot 2176 243 6096` | 3552 230 -5808; 1232 230 -11024; 11888 216 -5424 |  |
| Foliage Fortress Middle River | `biomic:origen/plateao_river_middle` | 6172 | 2119 239 6096 (6454 m) | `/tp ReviewBot 2119 239 6096` | 1280 229 -11008; 2208 183 -14176; -9872 230 -11248 |  |
| River | `biomic:origen/river` | 6173 | 0 184 0 (0 m) | `/tp ReviewBot 0 184 0` | -1840 180 -784; 1440 177 1392; 643 191 -1902 | on the sea floor, 7 blocks of water (surface y 191) |
| Salt Flats | `biomic:origen/salt_flats` | 6174 | -928 194 -144 (939 m) | `/tp ReviewBot -928 194 -144` | -1056 194 1856; -2944 194 640; -896 194 -2960 |  |
| Scarlet Sanctuary River | `biomic:origen/scarlet_sanctuary_river` | 6175 | -560 176 1904 (1985 m) | `/tp ReviewBot -560 176 1904` | -3152 243 1296; -1088 211 -4752; 1179 218 -6990 | on the sea floor, 15 blocks of water (surface y 191) |
| Shale Beach | `biomic:origen/shale_beach` | 6176 | 848 192 832 (1188 m) | `/tp ReviewBot 848 192 832` | -1152 193 960; -1504 190 -1552; 322 191 -2720 |  |
| Snowy Meadow | `biomic:origen/snowy_meadow` | 6177 | 1200 238 -192 (1215 m) | `/tp ReviewBot 1200 238 -192` | 1360 194 -2256; 3040 194 592; 3744 198 2624 |  |
| Snowy Mountains | `biomic:origen/snowy_mountains` | 6178 | 752 235 64 (755 m) | `/tp ReviewBot 752 235 64` | 1408 191 -2160; 2512 193 1024; -96 211 4704 |  |
| Temperate Sea Arches | `biomic:origen/temperate_sea_arches` | 6179 | 1936 240 1680 (2563 m) | `/tp ReviewBot 1936 240 1680` | -3004 191 -3180; -3584 190 3168; 3168 247 3600 |  |
| Tropical Deep Ocean | `biomic:origen/tropical_deep_ocean` | 6180 | -672 154 848 (1082 m) | `/tp ReviewBot -672 154 848` | -1824 160 -3328; -3584 168 2576; 288 152 -5968 | on the sea floor, 37 blocks of water (surface y 191) |
| Tropical Ocean | `biomic:origen/tropical_ocean` | 6181 | -560 186 304 (637 m) | `/tp ReviewBot -560 186 304` | -2848 187 2784; -1616 166 -4512; 1056 151 -5392 | on the sea floor, 5 blocks of water (surface y 191) |
| Verdant River | `biomic:origen/verdant_river` | 6182 | 608 182 192 (638 m) | `/tp ReviewBot 608 182 192` | 1520 179 2016; -1744 176 -1856; 576 181 -2688 | on the sea floor, 9 blocks of water (surface y 191) |
| Vertical Jungle River | `biomic:origen/vertical_jungle_river` | 6183 | -1952 261 1584 (2514 m) | `/tp ReviewBot -1952 261 1584` | -4448 257 1232; -2592 246 -4848; 1104 220 -6976 |  |
| Vertical Vistas River | `biomic:origen/vistas_river` | 6184 | 1408 215 224 (1426 m) | `/tp ReviewBot 1408 215 224` | 1120 189 2480; -1936 220 -2016; 208 193 -2912 |  |
| Fern Fissures | `biomic:origen/fern_fissures` | 6185 | 688 164 160 (706 m) | `/tp ReviewBot 688 164 160` | 464 201 -1840; 624 201 2304; -2352 201 -1200 |  |

### Vanilla biomes (hosted)

| Biome (F3) | Key | ID | Nearest x y z | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Plains | `minecraft:plains` | 1 | 1664 198 2096 (2676 m) | `/tp ReviewBot 1664 198 2096` | -4016 200 -528; 4096 199 -672; -2624 165 4016 |  |
| Desert | `minecraft:desert` | 2 | -96 195 1456 (1459 m) | `/tp ReviewBot -96 195 1456` | -1888 195 544; -1808 195 -3856; 5856 199 -1056 |  |
| Forest | `minecraft:forest` | 4 | -1696 200 -1296 (2134 m) | `/tp ReviewBot -1696 200 -1296` | 1552 85 -4144; 2112 252 4096; 4480 224 -2608 |  |
| Taiga | `minecraft:taiga` | 5 | 1952 93 -464 (2006 m) | `/tp ReviewBot 1952 93 -464` | 2384 232 -3824; -4544 211 -3264; -6528 207 -12 | on a sinkhole floor, far below the land around it |

### Biomes O' Plenty (hosted, pack only)

| Biome (F3) | Key | ID | Nearest x y z | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Dead Swamp | `biomesoplenty:dead_swamp` | 5054 | -224 191 -2624 (2634 m) | `/tp ReviewBot -224 191 -2624` | 466 191 2832; 4032 191 1136; 4960 191 -3968 | pack only (standalone shows Fen here) |
| Garden | `biomesoplenty:garden` | 5062 | 448 199 -496 (668 m) | `/tp ReviewBot 448 199 -496` | -1584 253 -1840; -16 195 -3088; 3728 234 1392 | pack only (standalone shows Towering Cliffs here) |
| Lavender Fields | `biomesoplenty:lavender_fields` | 5069 | -112 157 -704 (713 m) | `/tp ReviewBot -112 157 -704` | -2816 163 -464; 256 245 2896; 1264 231 -2928 | pack only (standalone shows Plains here) |
| Mystic Grove | `biomesoplenty:mystic_grove` | 5077 | 368 232 -96 (380 m) | `/tp ReviewBot 368 232 -96` | 128 206 2128; 2496 235 3808; -3584 196 -2944 | pack only (standalone shows Silver Birch Forest here) |
| Ominous Woods | `biomesoplenty:ominous_woods` | 5078 | 2208 226 -976 (2414 m) | `/tp ReviewBot 2208 226 -976` | -2208 239 -1440; 1248 242 -2736; 2448 244 4240 | pack only (standalone shows Taiga here) |
| Sacred Springs | `biomesoplenty:sacred_springs` | 5085 | -2875 206 1860 (3424 m) | `/tp ReviewBot -2875 206 1860` | -3340 91 -5119; -384 85 -8175 | pack only (standalone shows Sparse Rainforest here) |
| Sludgepit | `biomesoplenty:sludgepit` | 5089 | 243 191 -125 (273 m) | `/tp ReviewBot 243 191 -125` | -3456 192 -560; 144 193 4896; -3696 191 -4191 | pack only (standalone shows Bayou here) |
| Wasteland | `biomesoplenty:wasteland` | 5095 | -2128 205 -176 (2135 m) | `/tp ReviewBot -2128 205 -176` | -2879 198 2339; -1024 196 -3981; 2832 231 5856 | pack only (standalone shows Outback here) |
| Kelp Forest | `biomesoplenty:kelp_forest` | 5100 | -568 149 840 (1014 m) | `/tp ReviewBot -568 149 840` | -1721 189 2696; -3770 170 1928; 4874 156 -58 | pack only (standalone shows Hyacinth Deeps here); on the sea floor, 42 blocks of water (surface y 191); a sea-floor identity: the surface map above it shows the ocean |
| Volcano | `biomesoplenty:volcano` | 5102 | -1200 153 -4112 (4284 m) | `/tp ReviewBot -1200 153 -4112` | -1072 162 -7056; 816 168 -8000; -6816 159 7088 | pack only (standalone shows Tropical Deep Ocean here); on the sea floor, 38 blocks of water (surface y 191) |
| Silkglades | `biomesoplenty:silkglades` | 5113 | -1439 191 -2255 (2675 m) | `/tp ReviewBot -1439 191 -2255` | 2499 191 5184; -3289 191 6112; 6144 192 -3472 | pack only (standalone shows Fossilized Fenlands here) |

### Other hosted mods (pack only)

| Biome (F3) | Key | ID | Nearest x y z | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Salt Marsh | `saltymod:salt_marsh` | 40 | -432 191 3216 (3245 m) | `/tp ReviewBot -432 191 3216` | -3840 192 -4352; 3600 191 5616; -3168 191 -6768 | pack only (standalone shows Murky Marshlands here) |

## Sky

Sky islands float between about y 260 and y 390, at most one cluster in each 704-block square region. They come in two shapes. In a 512-high world an island's biome is the habitat it hosts, which follows the ground below it, so F3 shows that habitat (for example Redwoods or Desert) and never "Sky Donut" or "Aeropelago".

### Island shapes

| Shape | What it is | Clusters within 20000 blocks | Nearest cluster centre (x z) | Spot on it (x y z) | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Sky Donut | one ring-shaped island (the donut), up to about 350 blocks across | 326 | -1146 1084 | -1178 326 1060 (1585 m) | `/tp ReviewBot -1178 326 1060` | 1663 366 441; -889 269 -2290; 2620 297 2623 | F3 there: Desert |
| Aeropelago | many small islands spread over about 600 blocks | 878 | 916 457 | 892 344 449 (999 m) | `/tp ReviewBot 892 344 449` | -1086 316 -463; 1087 313 -1761; -1700 343 1730 | F3 there: Taiga |

### Biomes on sky islands

| Biome (F3) | Stored key | ID | Nearest x y z | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Taiga | `biomic:hosted/minecraft/taiga` | 5 | 892 344 449 (999 m) | `/tp ReviewBot 892 344 449` | -1086 316 -463; 4509 323 3919; -4452 302 4407 | on Aeropelago islands |
| Birch Forest | `biomic:hosted/minecraft/cherry_grove` | 27 | 924 340 497 (1049 m) | `/tp ReviewBot 924 340 497` | 4551 352 1802; -8906 356 -3296; -289 358 -11026 | on Aeropelago islands; not on the ground anywhere: sky only |
| Plains | `biomic:hosted/minecraft/plains` | 1 | -1021 310 -303 (1065 m) | `/tp ReviewBot -1021 310 -303` | 1120 335 3242; -2344 317 3043; -2608 309 -3187 | on Aeropelago islands |
| Autumnal Maple Forest | `biomic:hosted/regions_unexplored/autumnal_maple_forest` | 6019 | -1077 307 -375 (1140 m) | `/tp ReviewBot -1077 307 -375` | -259 295 1626; -2598 297 -2249; -416 297 4013 | on Aeropelago, Sky Donut islands |
| Rainforest | `biomic:hosted/regions_unexplored/rainforest` | 6049 | -1101 310 -311 (1144 m) | `/tp ReviewBot -1101 310 -311` | -4516 308 4511; 6545 314 -964; -1573 338 -6659 | on Aeropelago islands |
| Maple Forest | `biomic:hosted/regions_unexplored/maple_forest` | 6038 | 1084 327 369 (1145 m) | `/tp ReviewBot 1084 327 369` | -1069 310 -511; 457 351 -2346; -1708 350 1762 | on Aeropelago islands |
| Old Growth Forest | `biomic:hosted/regions_unexplored/old_growth_forest` | 6042 | 303 330 -1156 (1195 m) | `/tp ReviewBot 303 330 -1156` | -2580 353 -953; 1685 355 -4809; -5372 351 -4518 | on Aeropelago islands |
| Desert | `biomic:hosted/minecraft/desert` | 2 | -1178 326 1060 (1585 m) | `/tp ReviewBot -1178 326 1060` | -1889 320 -893; -554 347 -3791; -2408 312 3235 | on Aeropelago, Sky Donut islands |
| Pine Taiga | `biomic:hosted/regions_unexplored/pine_taiga` | 6046 | -1205 310 -1033 (1587 m) | `/tp ReviewBot -1205 310 -1033` | -520 293 4005; 1089 319 -4397; -5184 300 -588 | on Aeropelago, Sky Donut islands |
| Ice Mountains | `biomic:hosted/minecraft/snowy_slopes` | 13 | 1663 366 441 (1720 m) | `/tp ReviewBot 1663 366 441` | 2418 328 -2499; -3867 324 -2323; -5945 324 -2371 | on Aeropelago, Sky Donut islands; not on the ground anywhere: sky only |
| Willow Forest | `biomic:hosted/regions_unexplored/willow_forest` | 6060 | 975 317 1663 (1928 m) | `/tp ReviewBot 975 317 1663` | 998 349 -3296; 381 342 3676; -3281 341 -2630 | on Aeropelago islands |
| Marsh | `biomic:hosted/regions_unexplored/marsh` | 6039 | 1095 326 1631 (1964 m) | `/tp ReviewBot 1095 326 1631` | -8164 313 -7427; 13205 351 1352; 2663 324 -17211 | on Aeropelago islands |
| Icy Heights | `biomic:hosted/regions_unexplored/icy_heights` | 6035 | 1087 313 -1761 (2069 m) | `/tp ReviewBot 1087 313 -1761` | 3976 331 3281; -2485 361 6111; -5881 390 2996 | on Aeropelago, Sky Donut islands |
| Ominous Woods | `biomic:hosted/biomesoplenty/ominous_woods` | 5078 | 1007 310 -1905 (2155 m) | `/tp ReviewBot 1007 310 -1905` | -1692 331 1642; -3817 354 4434; 4589 322 3911 | pack only (standalone shows Autumnal Maple Forest here); on Aeropelago islands |
| Silver Birch Forest | `biomic:hosted/regions_unexplored/silver_birch_forest` | 6053 | 401 356 -2202 (2238 m) | `/tp ReviewBot 401 356 -2202` | -2494 303 -2385; 509 353 3724; 3780 336 -526 | on Aeropelago islands |
| Bayou | `biomic:hosted/regions_unexplored/bayou` | 6009 | 489 353 -2234 (2287 m) | `/tp ReviewBot 489 353 -2234` | -2594 318 5286; -5932 311 361; 795 307 -5890 | on Aeropelago islands |
| Forest | `biomic:hosted/minecraft/forest` | 4 | -1715 361 -1677 (2399 m) | `/tp ReviewBot -1715 361 -1677` | 3173 285 1169; -3807 350 -892; 4673 352 -506 | on Aeropelago, Sky Donut islands |
| Bamboo Forest | `biomic:hosted/regions_unexplored/bamboo_forest` | 6020 | -1700 343 1730 (2425 m) | `/tp ReviewBot -1700 343 1730` | -2916 313 -300; -2379 315 -4616; 264 339 -5356 | on Aeropelago islands |
| Ice Plains | `biomic:hosted/minecraft/snowy_plains` | 12 | 1895 338 -1559 (2454 m) | `/tp ReviewBot 1895 338 -1559` | -1774 309 3605; 3943 331 3056; 3803 303 -3917 | on Aeropelago islands; not on the ground anywhere: sky only |
| Redwoods | `biomic:hosted/regions_unexplored/redwoods` | 6008 | -889 269 -2290 (2457 m) | `/tp ReviewBot -889 269 -2290` | 2620 297 2623; -5315 300 2505; -7581 331 -5920 | on Sky Donut islands |
| Cold Taiga | `biomic:hosted/minecraft/snowy_taiga` | 30 | 2441 328 -306 (2460 m) | `/tp ReviewBot 2441 328 -306` | 2708 353 -3974; -6034 357 3812; 6496 369 5228 | on Aeropelago islands; not on the ground anywhere: sky only |
| Wisteria Grove | `biomic:hosted/regions_unexplored/wisteria_grove` | 6005 | 1839 339 -1759 (2545 m) | `/tp ReviewBot 1839 339 -1759` | 473 318 -3998; 4705 349 -434; -5080 294 -620 | on Aeropelago islands |
| Extreme Hills+ | `biomic:hosted/minecraft/windswept_forest` | 34 | -1087 343 2358 (2596 m) | `/tp ReviewBot -1087 343 2358` | -4035 353 395; -1706 354 -3956; -4349 350 2564 | on Aeropelago islands; not on the ground anywhere: sky only |
| Clover Plains | `biomic:hosted/regions_unexplored/clover_plains` | 6004 | 2601 328 -178 (2607 m) | `/tp ReviewBot 2601 328 -178` | 3231 359 -2414; -1095 336 3914; 1541 348 -4665 | on Aeropelago islands |
| Tropics | `biomic:hosted/regions_unexplored/tropics` | 6058 | -1015 347 2406 (2611 m) | `/tp ReviewBot -1015 347 2406` | 1678 346 6599; -1056 309 -7421; 8149 309 -6105 | on Aeropelago islands |
| Garden | `biomic:hosted/biomesoplenty/garden` | 5062 | 1983 334 -1719 (2624 m) | `/tp ReviewBot 1983 334 -1719` | 1168 323 3322; 4565 355 -377; -2974 359 4123 | pack only (standalone shows Silver Birch Forest here); on Aeropelago islands |
| Grassland | `biomic:hosted/regions_unexplored/grassland` | 6031 | -2500 350 -1025 (2702 m) | `/tp ReviewBot -2500 350 -1025` | 6601 320 -5397; 10333 355 -6583; 14457 349 4392 | on Aeropelago, Sky Donut islands |
| Lavender Fields | `biomic:hosted/biomesoplenty/lavender_fields` | 5069 | -2956 317 -380 (2980 m) | `/tp ReviewBot -2956 317 -380` | 501 343 3852; -5979 315 417; 4699 333 5427 | pack only (standalone shows Highland Fields here); on Aeropelago islands |
| Fen | `biomic:hosted/regions_unexplored/fen` | 6027 | -3164 317 -380 (3187 m) | `/tp ReviewBot -3164 317 -380` | 425 318 -3838; -2714 315 -3889; -3771 355 6545 | on Aeropelago islands |
| Boreal Taiga | `biomic:hosted/regions_unexplored/boreal_taiga` | 6023 | 1014 355 -3152 (3311 m) | `/tp ReviewBot 1014 355 -3152` | -7475 375 9478; -1034 358 12146; 12268 351 4503 | on Aeropelago islands |
| Old Growth Golden Boreal Taiga | `biomic:hosted/regions_unexplored/old_growth_golden_boreal_taiga` | 6043 | 2386 322 -2419 (3398 m) | `/tp ReviewBot 2386 322 -2419` | 6102 322 10773; 8765 348 -9619; -13658 318 -668 | on Aeropelago islands |
| Wasteland | `biomic:hosted/biomesoplenty/wasteland` | 5095 | 1248 336 3218 (3452 m) | `/tp ReviewBot 1248 336 3218` | -743 302 -6481; 6161 316 -2596; 2287 314 7644 | pack only (standalone shows Wisteria Grove here); on Aeropelago islands |
| Old Growth Bayou | `biomic:hosted/regions_unexplored/old_growth_bayou` | 6010 | -1143 334 3706 (3878 m) | `/tp ReviewBot -1143 334 3706` | 1663 343 -3864; -5851 308 305; 5856 316 -3317 | on Aeropelago islands |
| Mystic Grove | `biomic:hosted/biomesoplenty/mystic_grove` | 5077 | -1071 332 3730 (3881 m) | `/tp ReviewBot -1071 332 3730` | -5240 299 -388; 3865 328 -4628; -1647 313 7215 | pack only (standalone shows Maple Forest here); on Aeropelago islands |
| Pine Slopes | `biomic:hosted/regions_unexplored/pine_slopes` | 6014 | -3807 352 -996 (3935 m) | `/tp ReviewBot -3807 352 -996` | -5819 309 361; 3985 349 4467; 6689 312 -836 | on Aeropelago islands |
| Saguaro Desert | `biomic:hosted/regions_unexplored/saguaro_desert` | 6051 | -3185 343 -2357 (3962 m) | `/tp ReviewBot -3185 343 -2357` | 4587 365 -1609; 2963 357 -5249; -6027 313 313 | on Aeropelago, Sky Donut islands |
| Blackwood Taiga | `biomic:hosted/regions_unexplored/blackwood_taiga` | 6022 | 3964 340 -342 (3979 m) | `/tp ReviewBot 3964 340 -342` | -8929 333 5198; -14404 365 1217; -11510 369 8857 | on Aeropelago, Sky Donut islands |
| Sludgepit | `biomic:hosted/biomesoplenty/sludgepit` | 5089 | -3289 352 -2398 (4070 m) | `/tp ReviewBot -3289 352 -2398` | 7347 337 -2985; -8309 324 -5361; 951 335 -12907 | pack only (standalone shows Fungal Fen here); on Aeropelago islands |
| Sparse Redwoods | `biomic:hosted/regions_unexplored/sparse_redwoods` | 6055 | -1790 301 3813 (4212 m) | `/tp ReviewBot -1790 301 3813` | 2362 338 -4673; -3750 313 -8777; -9439 351 9306 | on Aeropelago islands |
| Sacred Springs | `biomic:hosted/biomesoplenty/sacred_springs` | 5085 | -2552 296 -3403 (4254 m) | `/tp ReviewBot -2552 296 -3403` | 883 306 -6018; -4556 294 4215; -2289 303 -6232 | pack only (standalone shows Maple Forest here); on Aeropelago islands |
| Fungal Fen | `biomic:hosted/regions_unexplored/fungal_fen` | 6030 | -3369 343 -2638 (4279 m) | `/tp ReviewBot -3369 343 -2638` | 4996 349 329; 7471 328 1501; -10260 319 -3100 | on Aeropelago, Sky Donut islands |
| Outback | `biomic:hosted/regions_unexplored/outback` | 6045 | -497 345 -4533 (4560 m) | `/tp ReviewBot -497 345 -4533` | 16567 364 -1231; 5984 358 16346; 8107 366 -15926 | on Aeropelago, Sky Donut islands |
| Mesa Plateau F | `biomic:hosted/minecraft/wooded_badlands` | 38 | -2658 316 -3953 (4764 m) | `/tp ReviewBot -2658 316 -3953` | 5141 363 -1878; -1652 341 -6795; 320 324 7582 | on Aeropelago, Sky Donut islands; not on the ground anywhere: sky only |
| Spires | `biomic:hosted/regions_unexplored/spires` | 6056 | 2516 342 -4070 (4785 m) | `/tp ReviewBot 2516 342 -4070` | 4567 346 2355; -6801 322 2291; 5646 374 -4701 | on Aeropelago islands |
| Cold Taiga | `biomic:hosted/minecraft/grove` | 30 | 4471 360 1930 (4870 m) | `/tp ReviewBot 4471 360 1930` | 6696 373 5323; 3140 378 -8853; -12913 374 1711 | on Aeropelago islands |
| Flower Fields | `biomic:hosted/regions_unexplored/flower_fields` | 6028 | 4392 349 -2383 (4997 m) | `/tp ReviewBot 4392 349 -2383` | -4668 319 3876; -3924 336 -6903; 7627 331 -2953 | on Aeropelago islands |
| Tundra | `biomic:hosted/regions_unexplored/tundra` | 6059 | 1869 348 -4641 (5003 m) | `/tp ReviewBot 1869 348 -4641` | 3895 328 3248; -5357 337 -1872; 4047 345 -5343 | on Aeropelago, Sky Donut islands |
| Baobab Savanna | `biomic:hosted/regions_unexplored/baobab_savanna` | 6021 | -4461 349 2508 (5118 m) | `/tp ReviewBot -4461 349 2508` | -3239 326 -6591; -251 326 8226; -223 344 10881 | on Aeropelago, Sky Donut islands |
| Extreme Hills | `biomic:hosted/minecraft/windswept_gravelly_hills` | 3 | 2378 336 -4545 (5130 m) | `/tp ReviewBot 2378 336 -4545` | 9455 348 503; -9676 368 562; 8778 333 8125 | on Aeropelago islands; not on the ground anywhere: sky only |
| Windswept Maple Forest | `biomic:hosted/regions_unexplored/windswept_maple_forest` | 6061 | 5388 363 1085 (5496 m) | `/tp ReviewBot 5388 363 1085` | 5226 335 5419; -4712 304 -6048; 8202 354 3141 | on Aeropelago, Sky Donut islands |
| Towering Cliffs | `biomic:hosted/regions_unexplored/towering_cliffs` | 6007 | -3068 356 4687 (5602 m) | `/tp ReviewBot -3068 356 4687` | -8573 362 -5681; 15689 364 1621; 17775 355 1122 | on Aeropelago, Sky Donut islands |
| Sparse Rainforest | `biomic:hosted/regions_unexplored/sparse_rainforest` | 6054 | 5315 344 -2399 (5831 m) | `/tp ReviewBot 5315 344 -2399` | 15968 338 -1884; -4658 324 -18075; -18732 370 -2592 | on Aeropelago islands |
| Old Growth Boreal Taiga | `biomic:hosted/regions_unexplored/old_growth_boreal_taiga` | 6041 | -2617 315 5566 (6151 m) | `/tp ReviewBot -2617 315 5566` | 8122 356 3005; -7313 332 -7582; 9359 344 9511 | on Aeropelago islands |
| Magnolia Woodland | `biomic:hosted/regions_unexplored/magnolia_woodland` | 6037 | 5993 317 -2564 (6518 m) | `/tp ReviewBot 5993 317 -2564` | -2413 365 6575; -2462 339 10970; -12904 358 3839 | on Aeropelago islands |
| Silkglades | `biomic:hosted/biomesoplenty/silkglades` | 5113 | -3205 344 6125 (6913 m) | `/tp ReviewBot -3205 344 6125` | 6769 340 12317; -12789 320 -19569 | pack only (standalone shows Fungal Fen here); on Aeropelago islands |
| Dead Swamp | `biomic:hosted/biomesoplenty/dead_swamp` | 5054 | 5830 320 -3833 (6977 m) | `/tp ReviewBot 5830 320 -3833` | -1670 316 13076; 14425 313 -1037; 3866 309 14563 | pack only (standalone shows Fungal Fen here); on Aeropelago, Sky Donut islands |
| Highland Fields | `biomic:hosted/regions_unexplored/highland_fields` | 6033 | 5400 370 4703 (7161 m) | `/tp ReviewBot 5400 370 4703` | -3941 358 6210; -7489 321 -5220; -1695 350 -10813 | on Aeropelago islands |
| Ashen Woodland | `biomic:hosted/regions_unexplored/ashen_woodland` | 6018 | 2527 317 7460 (7876 m) | `/tp ReviewBot 2527 317 7460` | 5885 329 -13018; -13073 332 -8230; 14999 307 -3954 | on Aeropelago, Sky Donut islands |
| Orchard | `biomic:hosted/regions_unexplored/orchard` | 6044 | -3739 322 -7550 (8425 m) | `/tp ReviewBot -3739 322 -7550` | -5152 349 15626; 13655 348 -10194; -13860 354 10052 | on Aeropelago, Sky Donut islands |
| Prairie | `biomic:hosted/regions_unexplored/prairie` | 6048 | 11601 340 3031 (11990 m) | `/tp ReviewBot 11601 340 3031` | 7356 331 -11021; -12801 314 -4714; 16407 332 498 | on Aeropelago, Sky Donut islands |
| Joshua Desert | `biomic:hosted/regions_unexplored/joshua_desert` | 6011 | -11754 322 6782 (13570 m) | `/tp ReviewBot -11754 322 6782` | 8635 367 -12907; 15241 316 -3076; 18761 373 3789 | on Aeropelago islands |
| Poppy Fields | `biomic:hosted/regions_unexplored/poppy_fields` | 6047 | 9432 334 -13601 (16551 m) | `/tp ReviewBot 9432 334 -13601` | -13087 342 -13553; -17965 325 -17190; 17994 344 17890 | on Aeropelago, Sky Donut islands |
| Eucalyptus Forest | `biomic:hosted/regions_unexplored/eucalyptus_forest` | 6016 | -15330 318 -8756 (17654 m) | `/tp ReviewBot -15330 318 -8756` | -15095 318 -11024; 19914 341 5755; -17265 337 14727 | on Aeropelago islands |

## Caves

Cave biomes live in Biomic's 3D biome volume (chunk NBT `biomic.biome_volume`), so they show on F3 only inside the cave.

| Biome (F3) | Volume key | ID | Nearest x y z | Teleport | Further spots | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Prismachasm | `biomic:prismachasm` | 6064 | 0 100 0 (0 m) | `/tp ReviewBot 0 100 0` | 960 105 -576; -64 135 -1139; -768 107 960 | Regions Unexplored cave |
| Bioshroom Caves | `biomic:bioshroom_caves` | 6063 | 132 125 0 (132 m) | `/tp ReviewBot 132 125 0` | 1152 120 -185; -572 158 -1137; -960 121 960 | Regions Unexplored cave |
| Ancient Delta | `biomic:ancient_delta` | 6062 | 4 149 257 (257 m) | `/tp ReviewBot 4 149 257` | -956 169 -123; 1088 169 14; 261 122 1226 | Regions Unexplored cave |
| Deep Dark | `biomic:origen/deep_dark` | 6153 | -60 90 260 (267 m) | `/tp ReviewBot -60 90 260` | -768 78 -448; 1164 75 -190; 844 116 833 | Origen cave |
| Abyssal Chasm (Alex's Caves port) | `alexscaves:abyssal_chasm` | none | -245 77 271 (365 m) | `/tp ReviewBot -245 77 271` | 846 73 1088; -1408 79 -576; -448 73 1481 | a flooded ocean trench under the sea floor; it has no 1.7 biome id, so F3 shows the ocean biome above it; under water |
| Inferno | `biomic:inferno` | 6036 | 200 147 -320 (377 m) | `/tp ReviewBot 200 147 -320` | 961 162 384; 320 135 1157; -384 159 -1152 | Regions Unexplored cave (lava) |
| Dripstone Caves | `biomic:origen/dripstone_caves` | 6157 | -767 173 143 (780 m) | `/tp ReviewBot -767 173 143` | 64 151 1028; -1536 153 -755; -1717 139 456 | Origen cave |
| Vine Vault | `biomic:origen/vine_vault` | 6111 | 1164 166 588 (1304 m) | `/tp ReviewBot 1164 166 588` | 1740 136 -760; -2044 162 -1980; 1804 128 -2680 | Origen large cave |
| Inferno Isles | `biomic:origen/inferno_isles` | 6118 | -1340 122 -632 (1482 m) | `/tp ReviewBot -1340 122 -632` | -2172 166 -1969 | Origen large cave (lava lake with islands) |
| Terracotta Tombs | `biomic:origen/terracotta_tombs` | 6143 | -1408 100 780 (1610 m) | `/tp ReviewBot -1408 100 780` | -947 136 -1470; -2620 100 -820; 2816 99 -768 | Origen large cave |
| Lush Caves | `biomic:origen/lush_caves` | 6165 | 905 164 -1527 (1775 m) | `/tp ReviewBot 905 164 -1527` | -2624 172 1038 | Origen cave, rare |

## Nether (Tartarus)

These spots were found with Biomic `3af90e7` (branch `debloat`), the revision that added Regions Unexplored's four Nether biomes to a new world's Nether, which changed the whole Nether layout. A world keeps the Nether it was created with, so a world made with an earlier revision has a different Nether.

Biomic generates the Nether itself (`ChunkProviderBiomicNether`, 256 high). In the pack it hosts BOP's and Netherlicious's Nether biomes besides its own, so the standalone Nether has a different layout: its spots are in the last column.

Found and checked the same way as the rest of the atlas: the probe read the Nether's biome on a 16-block grid from -16384 to 16384 in x and z, then the server generated each picked spot's chunk and its neighbours and the spot was read back from the saved region files (stored biome, a solid floor that is not lava, two passable blocks to stand in): 104 of 104 pack spots and 36 of 36 standalone spots passed. As a cross-check, the four Regions Unexplored standing spots that a separate pack bench world from the same revision found (Glistering Meadow -6504 90 -19384, Infernal Holt -6584 76 -19352, Mycotoxic Undergrowth -19080 56 -6584, Blackstone Basin -19384 70 6216, all beyond this map's edge) were generated again here: each is in the same biome, with open air at feet and head.

### Tartarus (Biomic's Nether)

| Biome (F3) | Key | ID | Nearest x y z | Teleport (in the Nether) | Further spots | Standalone nearest |
| --- | --- | --- | --- | --- | --- | --- |
| Basaltic Deltas | `biomic:basaltic_deltas` | 6310 | 32 102 -288 (290 m) | `/tp ReviewBot 32 102 -288` | 656 93 1616; -1776 190 1056; -1776 207 -1152 | `/tp ReviewBot 32 102 -288` |
| Bloodless Undergrowth | `biomic:bloodless_undergrowth` | 6311 | 656 42 -31 (657 m) | `/tp ReviewBot 656 42 -31` | 1456 113 -1872; 530 40 2322; -2032 117 1376 | `/tp ReviewBot 656 42 -31` |
| Bloody Remnants | `biomic:bloody_remnants` | 6312 | -816 201 0 (816 m) | `/tp ReviewBot -816 201 0` | 1040 61 -1263; 624 80 2128; -1376 155 1920 | `/tp ReviewBot 96 93 241` |
| Corrupted Wastes | `biomic:corrupted_wastes` | 6313 | -16 52 0 (16 m) | `/tp ReviewBot -16 52 0` | 1776 141 896; -2400 90 368 | `/tp ReviewBot -16 52 0` |
| Crimson Forest | `biomic:crimson_forest` | 6314 | 176 63 720 (741 m) | `/tp ReviewBot 176 63 720` | -560 206 -1280; -1824 91 272; 2016 149 -64 | `/tp ReviewBot 176 63 720` |
| Crystalline Veldts | `biomic:crystalline_veldts` | 6315 | -512 68 1376 (1468 m) | `/tp ReviewBot -512 68 1376` | 1377 99 -848; -2032 90 -3552; 288 52 -4256 | `/tp ReviewBot -1504 64 1648` |
| Flaming Hellscapes | `biomic:flaming_hellscapes` | 6316 | 16 138 800 (800 m) | `/tp ReviewBot 16 138 800` | -496 98 -1520; 1712 48 -1360; 2016 96 880 | `/tp ReviewBot 112 190 80` |
| Lifeless Spires | `biomic:lifeless_spires` | 6317 | -1065 41 272 (1099 m) | `/tp ReviewBot -1065 41 272` | 1600 84 144; -2512 65 -1632; 1008 68 -2944 | `/tp ReviewBot -297 42 -624` |
| Serrated Mountains | `biomic:serrated_mountains` | 6318 | 112 143 80 (138 m) | `/tp ReviewBot 112 143 80` | -1888 164 64; 1504 188 -1360; 2400 168 816 | `/tp ReviewBot -480 135 -112` |
| Soul Valley | `biomic:soul_valley` | 6319 | -240 103 592 (639 m) | `/tp ReviewBot -240 103 592` | 1696 128 -528; -1632 106 -848; 1808 91 1712 | `/tp ReviewBot -448 56 112` |
| Torrid Crags | `biomic:torrid_crags` | 6320 | -304 147 -624 (694 m) | `/tp ReviewBot -304 147 -624` | 1712 178 -240; -1200 135 1568; -2352 190 -1424 | `/tp ReviewBot -112 128 240` |
| Warped Forest | `biomic:warped_forest` | 6321 | -480 51 -112 (493 m) | `/tp ReviewBot -480 51 -112` | 1072 156 1408; 656 90 -1760; 2416 168 -80 | `/tp ReviewBot 496 82 96` |

### Gardens of the Dead (Biomic, hosted in the Nether)

| Biome (F3) | Key | ID | Nearest x y z | Teleport (in the Nether) | Further spots | Standalone nearest |
| --- | --- | --- | --- | --- | --- | --- |
| Soulblight Forest | `biomic:soulblight_forest` | 6300 | -576 50 960 (1120 m) | `/tp ReviewBot -576 50 960` | -1200 124 -960; 2160 141 -1328; -2704 92 624 | `/tp ReviewBot 240 157 -144` |
| Whistling Woods | `biomic:whistling_woods` | 6301 | -1264 65 112 (1269 m) | `/tp ReviewBot -1264 65 112` | 1904 102 336; -704 51 -1824; -688 133 2048 | `/tp ReviewBot -736 90 432` |

### Regions Unexplored (ported by Biomic, hosted in the Nether)

| Biome (F3) | Key | ID | Nearest x y z | Teleport (in the Nether) | Further spots | Standalone nearest |
| --- | --- | --- | --- | --- | --- | --- |
| Blackstone Basin | `biomic:blackstone_basin` | 6066 | 436 227 -1388 (1455 m) | `/tp ReviewBot 436 227 -1388` | 0 137 2288; -1952 145 -1840; -1984 40 2017 | `/tp ReviewBot -1264 50 112` |
| Glistering Meadow | `biomic:glistering_meadow` | 6067 | -288 84 -1952 (1973 m) | `/tp ReviewBot -288 84 -1952` | 192 52 2016; 2352 66 -496; 2128 51 1504 | `/tp ReviewBot 432 52 -1392` |
| Infernal Holt | `biomic:infernal_holt` | 6068 | 464 56 -208 (508 m) | `/tp ReviewBot 464 56 -208` | -1536 142 -112; 760 184 2032; -992 86 -2048 | `/tp ReviewBot 496 134 -110` |
| Mycotoxic Undergrowth | `biomic:mycotoxic_undergrowth` | 6069 | -128 146 -367 (389 m) | `/tp ReviewBot -128 146 -367` | 672 71 -2208; 2000 86 1152; -1328 43 -2175 | `/tp ReviewBot -128 146 -367` |

### Biomes O' Plenty (hosted, pack only)

| Biome (F3) | Key | ID | Nearest x y z | Teleport (in the Nether) | Further spots | Standalone nearest |
| --- | --- | --- | --- | --- | --- | --- |
| Boneyard | `biomesoplenty:boneyard` | 5117 | 11 55 560 (560 m) | `/tp ReviewBot 11 55 560` | -496 50 -2240; -2112 178 1056; -48 134 2656 | pack only |
| Undergarden | `biomesoplenty:undergarden` | 5119 | -112 167 240 (265 m) | `/tp ReviewBot -112 167 240` | -976 175 -1568; 1680 65 1136; -2064 200 688 | pack only |
| Polar Chasm | `biomesoplenty:polar_chasm` | 5120 | 1472 142 752 (1653 m) | `/tp ReviewBot 1472 142 752` | -1424 169 -992; 1856 219 -1216; 112 179 2960 | pack only |

### Netherlicious (hosted, pack only)

| Biome (F3) | Key | ID | Nearest x y z | Teleport (in the Nether) | Further spots | Standalone nearest |
| --- | --- | --- | --- | --- | --- | --- |
| Burning Desert | `netherlicious:burning_desert` | 3170 | 688 124 -1104 (1301 m) | `/tp ReviewBot 688 124 -1104` | -1552 188 -496; -1069 234 1457; 1856 190 624 | pack only |
| Tainted Shroom Cave | `netherlicious:tainted_shroom_cave` | 3171 | 240 157 -144 (280 m) | `/tp ReviewBot 240 157 -144` | -1648 181 816; 1504 53 -1696; 2288 40 1008 | pack only |
| Foxfire Swamp | `netherlicious:foxfire_swamp` | 3179 | 496 51 96 (505 m) | `/tp ReviewBot 496 51 96` | -496 56 -1712; -2128 155 -544; -304 87 2288 | pack only |
| Abyssal Shadowland | `netherlicious:abyssal_shadowland` | 3181 | 2160 139 720 (2277 m) | `/tp ReviewBot 2160 139 720` | -1040 64 2144; -3600 134 272; -1808 100 -3408 | pack only |
| Ruptured Chasm | `netherlicious:ruptured_chasm` | 3182 | -400 197 -1008 (1084 m) | `/tp ReviewBot -400 197 -1008` | -1072 116 1232; -2544 230 -815; 1104 158 -2464 | pack only |

## The End

Biomic does not touch the End (no End generator or End biome of its own), so it is not mapped here.

## Not found or not generated

Searched: the overworld surface map out to 32768 blocks in x and z, the terrain survey (caves, islands, sea floor) out to 6144 blocks, sky clusters out to 20000 blocks and the Nether out to 16384 blocks.

- Biomic Temperate Lowlands (`biomic:temperate_lowlands`, 6000), Biomic Temperate Archipelago (`biomic:temperate_archipelago`, 6001) and Biomic Windswept Highlands (`biomic:windswept_highlands`, 6002): not on the surface map. `SurfaceBiomeSampler` returns them only as a fallback when Origen's profile for a column is not a surface profile, which this world type never produces.
- Biomic Lush Caves (`biomic:lush_caves`, 6003): in no surveyed chunk's biome volume. It is registered, but no generator writes this key; the lush caves of this world are Origen's Lush Caves (6165, under Caves).
- Sky Donut (`biomic:sky_donut`, 6012) and Aeropelago (`biomic:aeropelago`, 6013): never stored in a 512-high world. Islands of those shapes store the habitat they host (`biomic:hosted/...`); the shapes themselves are under Sky: Island shapes.
- Estuary (`biomic:estuary`, 6302): not on the surface map or in the survey. It is registered, but nothing selects it in this revision.
- Alex's Caves Forlorn Hollows (`alexscaves:forlorn_hollows`) and Primordial Caves (`alexscaves:primordial_caves`): retired for new worlds. This world's saved Alex's Caves settings have both off, so only the Abyssal Chasm generates.
- Netherlicious Glow Jungle: one of the Nether biomes Tartarus can host, but it is not registered in this pack, so this Nether has none. Netherlicious's own Basalt Deltas, Crimson Forest, Soul Sand Valley, Warped Forest and Crystalline Crag are left out on purpose (near copies of Tartarus' own).
- Biomes O' Plenty: only its 11 curated overworld biomes (`bopFullCatalogue=false`) and three Nether biomes (Undergarden, Polar Chasm, Boneyard) are hosted; its other biomes do not generate in this world. Vanilla biomes appear only where a habitat hosts them: Plains, Desert, Forest and Taiga on the ground, a few more on sky islands.

