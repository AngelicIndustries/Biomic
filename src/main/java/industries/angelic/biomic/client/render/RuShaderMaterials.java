package industries.angelic.biomic.client.render;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.content.block.BlockRuBranch;
import industries.angelic.biomic.content.block.BlockRuCaveModel;
import industries.angelic.biomic.content.block.BlockRuConeflower;
import industries.angelic.biomic.content.block.BlockRuDoublePlant;
import industries.angelic.biomic.content.block.BlockRuFlatPlant;
import industries.angelic.biomic.content.block.BlockRuGroundCover;
import industries.angelic.biomic.content.block.BlockRuPlant;
import industries.angelic.biomic.content.block.BlockRuVariantPlant;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/** Pack-relative fallback material IDs. No shaderpack numbers, lighting changes or render-layer overrides. */
public final class RuShaderMaterials {

    private RuShaderMaterials() {}

    public static int apply(Map<Block, Int2IntMap> materials, Iterable<Block> blocks) {
        if (materials == null) return 0;
        int assigned = 0;
        for (Block block : blocks) {
            for (int metadata = 0; metadata < 16; metadata++) {
                Material material = fallback(block, metadata);
                if (material == null) continue;
                Int2IntMap source = materials.get(material.block());
                // An absent entry is not material zero. Leave unknown classifications alone.
                if (source == null || !source.containsKey(material.metadata())) continue;
                Int2IntMap target = materials.get(block);
                // Explicit Biomic mappings (including the extended-metadata wildcard) always win.
                if (target != null && (target.containsKey(metadata) || target.containsKey(1 << 30))) continue;
                if (target == null) {
                    target = new Int2IntOpenHashMap();
                    target.defaultReturnValue(-1);
                    materials.put(block, target);
                }
                copyFallback(target, metadata, source, material.metadata());
                assigned++;
            }
        }
        return assigned;
    }

    static void copyFallback(Int2IntMap target, int metadata, Int2IntMap source, int sourceMetadata) {
        if (source == null || !source.containsKey(sourceMetadata)
            || target.containsKey(metadata)
            || target.containsKey(1 << 30)) return;
        target.put(metadata, source.get(sourceMetadata));
    }

    static Material fallback(Block block, int metadata) {
        String registry = Block.blockRegistry.getNameForObject(block);
        // BOP's single-kelp meta is commonly classified with vines. Give its generated
        // bottom/middle/top segments the same continuous-column movement without pack IDs.
        if (isBopKelp(registry, metadata)) return new Material(Blocks.vine, 0);
        int reedHalf = saltyReedHalf(registry);
        if (reedHalf >= 0) return new Material(Blocks.double_plant, reedHalf == 0 ? 2 : 10);
        if (!block.getUnlocalizedName()
            .startsWith("tile.biomic.")) return null;
        if (block instanceof net.minecraft.block.BlockLeaves) return new Material(Blocks.leaves, 0);
        // Ground litter/petals must not inherit full-height grass wind. Keep emission and woody branches intact.
        if (block.getLightValue() > 0 || block instanceof BlockRuGroundCover
            || block instanceof BlockRuFlatPlant
            || block instanceof BlockRuBranch) return null;
        String gardens = gardensVegetationKind(registry);
        if ("vine".equals(gardens)) return new Material(Blocks.vine, 0);
        if ("mushroom".equals(gardens)) return new Material(Blocks.red_mushroom, 0);
        if ("grass".equals(gardens)) return new Material(Blocks.tallgrass, 1);
        String forlorn = forlornVegetationKind(registry);
        if ("grass".equals(forlorn)) return new Material(Blocks.tallgrass, 1);
        if (block instanceof BlockRuDoublePlant) return new Material(Blocks.double_plant, (metadata & 8) | 2);
        String name = block.getUnlocalizedName();
        if (block instanceof BlockRuCaveModel cave && cave.modelName()
            .equals("prismoss_sprout")) {
            return new Material(Blocks.tallgrass, 1);
        }
        if (block instanceof BlockRuCaveModel cave
            && industries.angelic.biomic.content.RuCaveModelData.hangingFamily(cave.modelName()) != null) {
            return new Material(Blocks.vine, 0);
        }
        if (block instanceof BlockRuPlant
            && (name.contains("vine") || name.contains("moss") || name.contains("beard"))) {
            return new Material(Blocks.vine, 0);
        }
        if (block instanceof BlockRuVariantPlant || block instanceof BlockRuPlant
            || block instanceof BlockRuConeflower) {
            if (name.contains("shroom")) return new Material(Blocks.red_mushroom, 0);
            return new Material(Blocks.tallgrass, 1);
        }
        return null;
    }

    static boolean isBopKelp(String registry, int metadata) {
        return registry != null && registry.equalsIgnoreCase("BiomesOPlenty:coral1") && metadata >= 8 && metadata <= 11;
    }

    static int saltyReedHalf(String registry) {
        if ("saltymod:marsh_reeds_b".equals(registry)) return 0;
        if ("saltymod:marsh_reeds_t".equals(registry)) return 1;
        return -1;
    }

    /** Only non-emissive, non-woody intrinsic Gardens plants inherit pack-relative movement. */
    static String gardensVegetationKind(String registry) {
        if (registry == null) return null;
        return switch (registry.toLowerCase(java.util.Locale.ROOT)) {
            case "biomic:gardens_soul_spore" -> "vine";
            case "biomic:gardens_soulblight_fungus" -> "mushroom";
            case "biomic:gardens_soulblight_sprouts" -> "grass";
            default -> null;
        };
    }

    /** Only the soft, non-emissive Underweed plant moves; thornwood and cave materials remain rigid. */
    static String forlornVegetationKind(String registry) {
        return registry != null && registry.equalsIgnoreCase("biomic:alexs_forlorn_underweed") ? "grass" : null;
    }

    record Material(Block block, int metadata) {}
}
