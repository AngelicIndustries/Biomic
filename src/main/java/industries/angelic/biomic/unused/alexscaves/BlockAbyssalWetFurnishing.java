package industries.angelic.biomic.unused.alexscaves;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import industries.angelic.biomic.content.alexscaves.AbyssalClientRendering;

/**
 * Hidden, tile-free carriers for waterlogged vanilla shapes used only by the pinned rusty ruins.
 * Shape authorities from the pinned Minecraft 1.20.1 client jar:
 * e03c0c32e8f9753d4877d85e21114b9f5a1b78843f117415e26d7480b6106a98 chain.json
 * bd869ebe3ba380d46349e5c6e988b9b1ccf1ab25212ab1de66e2fdcc067edc1d slab.json
 * c02e81cd0b59698040db7a682d32d08ddeb0de64756e309d62ecfbda4af804f9 slab_top.json
 * 962dc154fd3337d6b7e165e2b734e171c4b3595c2b838a5da1e01f5bdcdcae3b stairs.json
 * fcb56ce59da95e5c1a77e49149caa3c72c18195141554a2c063d24b7962648d8 inner_stairs.json
 */
public final class BlockAbyssalWetFurnishing extends Block {

    public enum Kind {
        CHAIN,
        SLAB,
        STAIR_STRAIGHT,
        STAIR_INNER
    }

    public enum DryTarget {
        CHAIN,
        WEATHERED_SLAB,
        OXIDIZED_SLAB,
        WEATHERED_STAIR,
        WAXED_WEATHERED_STAIR,
        WAXED_OXIDIZED_STAIR
    }

    public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {}

    private final Kind kind;
    private final DryTarget dryTarget;

    BlockAbyssalWetFurnishing(String name, Kind kind, DryTarget dryTarget, String texture) {
        super(Material.iron);
        this.kind = kind;
        this.dryTarget = dryTarget;
        setBlockName("biomic.alexs_abyssal_" + name);
        setBlockTextureName("biomic:alexscaves/" + texture);
        setHardness(kind == Kind.CHAIN ? 5F : 3F);
        setResistance(6F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setLightOpacity(0);
    }

    public Kind kind() {
        return kind;
    }

    public boolean waterlogged() {
        return true;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return AbyssalClientRendering.renderId();
    }

    @Override
    public int getRenderBlockPass() {
        return 0;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    public Item getItemDropped(int metadata, java.util.Random random, int fortune) {
        Block dry = dryBlock();
        return Item.getItemFromBlock(dry == null ? this : dry);
    }

    @Override
    public int damageDropped(int metadata) {
        return dryTarget == DryTarget.WEATHERED_SLAB ? 2 : dryTarget == DryTarget.OXIDIZED_SLAB ? 3 : 0;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return getItemDropped(world.getBlockMetadata(x, y, z), world.rand, 0);
    }

    @Override
    public void setBlockBoundsBasedOnState(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        Box b = envelope(cuboids(world.getBlockMetadata(x, y, z)));
        setBlockBounds((float) b.x0, (float) b.y0, (float) b.z0, (float) b.x1, (float) b.y1, (float) b.z1);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {
        for (Box b : cuboids(world.getBlockMetadata(x, y, z))) {
            AxisAlignedBB box = AxisAlignedBB
                .getBoundingBox(x + b.x0, y + b.y0, z + b.z0, x + b.x1, y + b.y1, z + b.z1);
            if (mask.intersectsWith(box)) boxes.add(box);
        }
    }

    /** Exact 1.20.1 collision/model cuboids; stair metadata is east/west/south/north + top + inner-right. */
    public List<Box> cuboids(int metadata) {
        return switch (kind) {
            case CHAIN -> List.of(box(6.5, 0, 6.5, 9.5, 16, 9.5));
            case SLAB -> List.of((metadata & 1) == 0 ? box(0, 0, 0, 16, 8, 16) : box(0, 8, 0, 16, 16, 16));
            case STAIR_STRAIGHT -> stairs(metadata, false);
            case STAIR_INNER -> stairs(metadata, true);
        };
    }

    private static List<Box> stairs(int metadata, boolean inner) {
        boolean top = (metadata & 4) != 0, right = (metadata & 8) != 0;
        int facing = metadata & 3;
        double base0 = top ? 8 : 0, base1 = top ? 16 : 8, step0 = top ? 0 : 8, step1 = top ? 8 : 16;
        ArrayList<Box> out = new ArrayList<>();
        out.add(box(0, base0, 0, 16, base1, 16));
        out.add(switch (facing) {
            case 0 -> box(8, step0, 0, 16, step1, 16);
            case 1 -> box(0, step0, 0, 8, step1, 16);
            case 2 -> box(0, step0, 8, 16, step1, 16);
            default -> box(0, step0, 0, 16, step1, 8);
        });
        if (inner) out.add(switch (facing) {
            case 0 -> right ? box(0, step0, 8, 8, step1, 16) : box(0, step0, 0, 8, step1, 8);
            case 1 -> right ? box(8, step0, 0, 16, step1, 8) : box(8, step0, 8, 16, step1, 16);
            case 2 -> right ? box(0, step0, 0, 8, step1, 8) : box(8, step0, 0, 16, step1, 8);
            default -> right ? box(8, step0, 8, 16, step1, 16) : box(0, step0, 8, 8, step1, 16);
        });
        return List.copyOf(out);
    }

    private static Box box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new Box(x0 / 16D, y0 / 16D, z0 / 16D, x1 / 16D, y1 / 16D, z1 / 16D);
    }

    private static Box envelope(List<Box> boxes) {
        double x0 = 1, y0 = 1, z0 = 1, x1 = 0, y1 = 0, z1 = 0;
        for (Box b : boxes) {
            x0 = Math.min(x0, b.x0);
            y0 = Math.min(y0, b.y0);
            z0 = Math.min(z0, b.z0);
            x1 = Math.max(x1, b.x1);
            y1 = Math.max(y1, b.y1);
            z1 = Math.max(z1, b.z1);
        }
        return new Box(x0, y0, z0, x1, y1, z1);
    }

    private Block dryBlock() {
        return switch (dryTarget) {
            case CHAIN -> ganymedes01.etfuturum.ModBlocks.CHAIN.get();
            case WEATHERED_SLAB, OXIDIZED_SLAB -> ganymedes01.etfuturum.ModBlocks.CUT_COPPER_SLAB.get();
            case WEATHERED_STAIR -> ganymedes01.etfuturum.ModBlocks.WEATHERED_CUT_COPPER_STAIRS.get();
            case WAXED_WEATHERED_STAIR -> ganymedes01.etfuturum.ModBlocks.WAXED_WEATHERED_CUT_COPPER_STAIRS.get();
            case WAXED_OXIDIZED_STAIR -> ganymedes01.etfuturum.ModBlocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS.get();
        };
    }
}
