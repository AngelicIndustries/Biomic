package industries.angelic.biomic.content.alexscaves;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.world.LiveChunkLayout;

/** Explicit registry boundary for the non-progression Primordial environment palette. */
public final class PrimordialContentBlocks {

    public static final Block LIMESTONE = new Solid("limestone", Material.rock, 1.2F, 0);
    public static final Block FLOOD_BASALT = new PillarRock("flood_basalt");
    public static final Block PRIMAL_MAGMA = new PrimalMagma();
    /** Deliberately inert: visual/environment parity only, with no summoning block entity. */
    public static final Block VOLCANIC_CORE = new Solid("volcanic_core", Material.rock, 55F, 8);
    public static final Block AMBER = new Amber();
    public static final Block AMBERSOL = new Ambersol();
    public static final Block AMBERSOL_LIGHT = new Light();
    public static final Block PEWEN_LOG = new Log("pewen_log", "pewen_log", "pewen_log_top");
    public static final Block PEWEN_WOOD = new Log("pewen_wood", "pewen_log", "pewen_log");
    public static final Block PEWEN_BRANCH = new Plant("pewen_branch", "pewen_branch", 0, 1F, Support.BRANCH);
    public static final Block PEWEN_PINES = new Plant("pewen_pines", "pewen_pines_0", 0, 1F, Support.FLOOR);
    public static final Block CYCAD = new Plant("cycad", "cycad", 0, 1F, Support.STACK);
    public static final Block CURLY_FERN = new Plant("curly_fern", "curly_fern_bottom", 0, 1F, Support.DOUBLE);
    public static final Block FLYTRAP = new Plant("flytrap", "flytrap_mouth_open", 0, 1F, Support.FLOOR);
    public static final Block ARCHAIC_VINE = new Plant("archaic_vine", "archaic_vine", 0, 1F, Support.HANGING);
    public static final Block ARCHAIC_VINE_PLANT = new Plant(
        "archaic_vine_plant",
        "archaic_vine_plant",
        0,
        1F,
        Support.HANGING);
    public static final Block ANCIENT_LEAVES = new Foliage("ancient_leaves");
    public static final Block TREE_STAR = new Plant("tree_star", "tree_star", 5, .75F, Support.FACE);
    private static final String[] MODEL_TEXTURES = { "pewen_branch", "pewen_branch_top", "no_pines", "pewen_pines_0",
        "pewen_pines_1", "pewen_pines_2", "pewen_pines_3", "pewen_pines_4", "pewen_pines_5", "pewen_pines_6",
        "pewen_pines_7", "pewen_tip", "cycad", "cycad_leaf", "cycad_trunk", "curly_fern_bottom", "curly_fern_frawn",
        "curly_fern_top", "flytrap_mouth_closed_0", "flytrap_mouth_closed_1", "flytrap_mouth_open", "flytrap_stem",
        "tree_star", "tree_star_stem" };

    private PrimordialContentBlocks() {}

    private static class Solid extends Block {

        Solid(String name, Material material, float hardness, int light) {
            super(material);
            setBlockName("biomic.alexs_primordial_" + name);
            setBlockTextureName("biomic:alexscaves/" + name);
            setHardness(hardness);
            setResistance(name.equals("limestone") ? 4.5F : 1F);
            if (light > 0) setLightLevel(light / 15F);
            setStepSound(material == Material.rock ? soundTypeStone : soundTypeGlass);
        }
    }

    private static final class PillarRock extends Block {

        private final String texture;
        private IIcon side, top;

        PillarRock(String texture) {
            super(Material.rock);
            this.texture = texture;
            setBlockName("biomic.alexs_primordial_" + texture);
            setHardness(3);
            setResistance(100);
            setStepSound(soundTypeStone);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister r) {
            side = r.registerIcon("biomic:alexscaves/" + texture);
            top = r.registerIcon("biomic:alexscaves/" + texture + "_top");
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getIcon(int face, int meta) {
            return face < 2 ? top : side;
        }
    }

    private static final class PrimalMagma extends Block {

        private IIcon side, top, active;

        PrimalMagma() {
            super(Material.rock);
            setBlockName("biomic.alexs_primordial_primal_magma");
            setHardness(3);
            setResistance(100);
            setLightLevel(5 / 15F);
            setStepSound(soundTypeStone);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister r) {
            side = r.registerIcon("biomic:alexscaves/primal_magma");
            top = r.registerIcon("biomic:alexscaves/primal_magma_top");
            active = r.registerIcon("biomic:alexscaves/primal_magma_active");
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getIcon(int face, int meta) {
            return (meta & 1) != 0 ? active : face < 2 ? top : side;
        }

        @Override
        public int damageDropped(int meta) {
            return 0;
        }
    }

    private static final class Amber extends Block {

        Amber() {
            super(Material.glass);
            setBlockName("biomic.alexs_primordial_amber");
            setBlockTextureName("biomic:alexscaves/amber");
            setHardness(.3F);
            setResistance(2);
            setStepSound(soundTypeGlass);
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public int getRenderBlockPass() {
            return 1;
        }

        @Override
        protected boolean canSilkHarvest() {
            return true;
        }
    }

    private static final class Ambersol extends Solid {

        Ambersol() {
            super("ambersol", Material.glass, 3F, 15);
            setResistance(10);
            setTickRandomly(true);
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            fillLights(world, x, y, z);
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
            fillLights(world, x, y, z);
        }

        private static void fillLights(World world, int x, int y, int z) {
            for (int py = y - 1; py > 0; py--) {
                Block current = world.getBlock(x, py, z);
                if (current == AMBERSOL_LIGHT) continue;
                if (!world.isAirBlock(x, py, z)) break;
                world.setBlock(x, py, z, AMBERSOL_LIGHT, 0, 3);
            }
        }
    }

    private static final class Log extends BlockLog {

        private final String side, top;
        private IIcon sideIcon, topIcon;

        Log(String name, String side, String top) {
            this.side = side;
            this.top = top;
            setBlockName("biomic.alexs_primordial_" + name);
            setHardness(2);
            setStepSound(soundTypeWood);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister r) {
            sideIcon = r.registerIcon("biomic:alexscaves/" + side);
            topIcon = r.registerIcon("biomic:alexscaves/" + top);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getIcon(int face, int meta) {
            int axis = meta & 12;
            boolean end = axis == 0 && (face < 2) || axis == 4 && (face == 4 || face == 5)
                || axis == 8 && (face == 2 || face == 3);
            return end ? topIcon : sideIcon;
        }

        @Override
        public int damageDropped(int metadata) {
            return 0;
        }
    }

    private enum Support {
        FLOOR,
        STACK,
        DOUBLE,
        HANGING,
        BRANCH,
        FACE
    }

    private static class Plant extends Block {

        private final String texture;
        private final Support support;
        private IIcon[] modelIcons;

        Plant(String name, String texture, int light, float height, Support support) {
            super(Material.plants);
            this.texture = texture;
            this.support = support;
            setBlockName("biomic.alexs_primordial_" + name);
            setHardness(support == Support.BRANCH ? 1 : 0);
            setLightOpacity(0);
            if (light > 0) setLightLevel(light / 15F);
            setStepSound(support == Support.BRANCH ? soundTypeWood : soundTypeGrass);
            setBlockBounds(.1F, 0, .1F, .9F, height, .9F);
            if ("flytrap".equals(name)) setTickRandomly(true);
        }

        @Override
        public boolean canBlockStay(World w, int x, int y, int z) {
            int physicalHeight = LiveChunkLayout.of(w)
                .height();
            if (y <= 0 || y >= physicalHeight) return false;
            int meta = w.getBlockMetadata(x, y, z);
            return switch (support) {
                case FLOOR -> solid(w, x, y - 1, z);
                case STACK -> solid(w, x, y - 1, z) || w.getBlock(x, y - 1, z) == this;
                case DOUBLE -> (meta & 4) != 0 ? w.getBlock(x, y - 1, z) == this : solid(w, x, y - 1, z);
                case HANGING -> y + 1 < physicalHeight
                    && (solid(w, x, y + 1, z) || w.getBlock(x, y + 1, z) == ARCHAIC_VINE
                        || w.getBlock(x, y + 1, z) == ARCHAIC_VINE_PLANT);
                case BRANCH -> {
                    int[] offset = branchOffset(meta & 7);
                    Block base = w.getBlock(x + offset[0], y, z + offset[1]);
                    yield base == this || base.getMaterial()
                        .blocksMovement();
                }
                case FACE -> {
                    int[] offset = faceSupport(meta);
                    yield solid(w, x + offset[0], y + offset[1], z + offset[2]);
                }
            };
        }

        @Override
        public void onNeighborBlockChange(World w, int x, int y, int z, Block b) {
            if (!canBlockStay(w, x, y, z)) {
                w.setBlockToAir(x, y, z);
                return;
            }
            int meta = w.getBlockMetadata(x, y, z);
            if (support == Support.STACK) {
                int wanted = y + 1 < LiveChunkLayout.of(w)
                    .height() && w.getBlock(x, y + 1, z) == this ? 0 : 1;
                if (meta != wanted) w.setBlockMetadataWithNotify(x, y, z, wanted, 2);
            } else if (support == Support.BRANCH) {
                int[] offset = branchOffset(meta & 7);
                boolean pines = w.getBlock(x - offset[0], y, z - offset[1]) != this;
                int wanted = (meta & 7) | (pines ? 8 : 0);
                if (meta != wanted) w.setBlockMetadataWithNotify(x, y, z, wanted, 2);
            }
        }

        @Override
        public void updateTick(World w, int x, int y, int z, Random random) {
            if (this != FLYTRAP) return;
            int meta = w.getBlockMetadata(x, y, z);
            if ((meta & 1) != 0) {
                w.setBlockMetadataWithNotify(x, y, z, 0, 2);
                w.scheduleBlockUpdate(x, y, z, this, 100 + random.nextInt(100));
            } else w.setBlockMetadataWithNotify(x, y, z, 1, 2);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(World w, int x, int y, int z, Random random) {
            if (this == FLYTRAP && (w.getBlockMetadata(x, y, z) & 1) != 0 && random.nextInt(3) == 0)
                industries.angelic.biomic.client.particle.PrimordialFlyParticle.spawn(w, x + .5, y + 1, z + .5);
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
            return customModel(this) ? BiomicRenderIds.PRIMORDIAL_MODEL : 1;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
            if (support != Support.BRANCH) return null;
            int rotation = w.getBlockMetadata(x, y, z) & 7;
            double minX = .0625, maxX = .9375, minZ = .0625, maxZ = .9375;
            if (rotation == 0) {
                minZ = 0;
                maxZ = .25;
            } else if (rotation == 2) {
                minX = 0;
                maxX = .25;
            } else if (rotation == 4) {
                minZ = .75;
                maxZ = 1;
            } else if (rotation == 6) {
                minX = .75;
                maxX = 1;
            }
            return AxisAlignedBB.getBoundingBox(x + minX, y + .0625, z + minZ, x + maxX, y + .9375, z + maxZ);
        }

        @Override
        public Item getItemDropped(int m, Random r, int f) {
            return Item.getItemFromBlock(this);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister r) {
            blockIcon = r.registerIcon("biomic:alexscaves/" + texture);
            if (customModel(this)) {
                modelIcons = new IIcon[MODEL_TEXTURES.length];
                for (int i = 0; i < MODEL_TEXTURES.length; i++)
                    modelIcons[i] = r.registerIcon("biomic:alexscaves/" + MODEL_TEXTURES[i]);
            }
            if (this == FLYTRAP) {
                flyIcons = new IIcon[] { r.registerIcon("biomic:alexscaves/fly_0"),
                    r.registerIcon("biomic:alexscaves/fly_1") };
            }
        }

        @SideOnly(Side.CLIENT)
        private IIcon[] flyIcons;
    }

    private static final class Foliage extends Block {

        Foliage(String name) {
            super(Material.leaves);
            setBlockName("biomic.alexs_primordial_" + name);
            setBlockTextureName("biomic:alexscaves/" + name);
            setHardness(.2F);
            setLightOpacity(1);
            setStepSound(soundTypeGrass);
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }
    }

    private static final class Light extends Block {

        Light() {
            super(Material.air);
            setBlockName("biomic.alexs_primordial_ambersol_light");
            setHardness(-1);
            setResistance(6000000);
            setLightLevel(1);
            setLightOpacity(0);
            setBlockBounds(0, 0, 0, 0, 0, 0);
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
            return -1;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
            return null;
        }

        @Override
        public Item getItemDropped(int m, Random r, int f) {
            return null;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void getSubBlocks(Item item, CreativeTabs tab, List values) {}

        @Override
        public void onNeighborBlockChange(World w, int x, int y, int z, Block b) {
            int physicalHeight = LiveChunkLayout.of(w)
                .height(), py = y + 1;
            while (py < physicalHeight && w.getBlock(x, py, z) == this) py++;
            if (py >= physicalHeight || w.getBlock(x, py, z) != AMBERSOL) w.setBlockToAir(x, y, z);
        }
    }

    private static boolean customModel(Block block) {
        return block == PEWEN_BRANCH || block == PEWEN_PINES
            || block == CYCAD
            || block == CURLY_FERN
            || block == FLYTRAP
            || block == TREE_STAR;
    }

    private static boolean solid(World world, int x, int y, int z) {
        return y >= 0 && y < LiveChunkLayout.of(world)
            .height()
            && world.getBlock(x, y, z)
                .getMaterial()
                .blocksMovement();
    }

    private static int[] branchOffset(int rotation) {
        return switch (rotation) {
            case 0 -> new int[] { 0, 1 };
            case 1 -> new int[] { -1, 1 };
            case 2 -> new int[] { -1, 0 };
            case 3 -> new int[] { -1, -1 };
            case 4 -> new int[] { 0, -1 };
            case 5 -> new int[] { 1, -1 };
            case 6 -> new int[] { 1, 0 };
            default -> new int[] { 1, 1 };
        };
    }

    private static int[] faceSupport(int metadata) {
        return switch (metadata) {
            case 0 -> new int[] { 0, 1, 0 };
            case 1 -> new int[] { 0, -1, 0 };
            case 2 -> new int[] { 0, 0, 1 };
            case 3 -> new int[] { 0, 0, -1 };
            case 4 -> new int[] { 1, 0, 0 };
            case 5 -> new int[] { -1, 0, 0 };
            default -> new int[] { 0, -1, 0 };
        };
    }

    @SideOnly(Side.CLIENT)
    public static IIcon flyIcon(int frame) {
        return ((Plant) FLYTRAP).flyIcons[frame & 1];
    }
}
