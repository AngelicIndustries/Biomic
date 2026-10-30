package industries.angelic.biomic.unused.alexscaves;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.content.alexscaves.AbyssalClientRendering;

/** Inert environment-only shells used by the sanitized rusty Abyssal ruin templates. */
public final class BlockAbyssalRuinShell extends Block {

    public enum Kind {
        SCRAP,
        DEPTH_GLASS,
        SCAFFOLDING,
        DRAIN,
        MAGNETIC_ACTIVATOR,
        LIGHTNING_ROD,
        WET_IRON_TRAPDOOR,
        CEILING_BUTTON
    }

    private final Kind kind;
    private final String[] textures;
    private IIcon[] icons;

    BlockAbyssalRuinShell(String name, Kind kind, String... textures) {
        super(kind == Kind.DEPTH_GLASS ? Material.glass : kind == Kind.CEILING_BUTTON ? Material.rock : Material.iron);
        this.kind = kind;
        this.textures = textures.clone();
        setBlockName("biomic.alexs_abyssal_" + name);
        setBlockTextureName("biomic:alexscaves/" + textures[0]);
        if (kind == Kind.DEPTH_GLASS) {
            setHardness(.2F);
            setStepSound(soundTypeGlass);
            setLightOpacity(0);
        } else if (kind == Kind.CEILING_BUTTON) {
            setHardness(.5F);
            setStepSound(soundTypeStone);
            setLightOpacity(0);
        } else {
            setHardness(5F);
            setResistance(15F);
            setStepSound(soundTypeMetal);
            setHarvestLevel("pickaxe", 0);
            if (kind == Kind.LIGHTNING_ROD || kind == Kind.WET_IRON_TRAPDOOR) setLightOpacity(0);
        }
    }

    public Kind kind() {
        return kind;
    }

    @SideOnly(Side.CLIENT)
    public IIcon icon(int index) {
        return icons[Math.min(index, icons.length - 1)];
    }

    @Override
    public boolean isOpaqueCube() {
        return kind == Kind.SCRAP || kind == Kind.MAGNETIC_ACTIVATOR;
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
        return kind == Kind.DEPTH_GLASS ? 1 : 0;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return kind == Kind.DEPTH_GLASS ? pass == 1
            : kind == Kind.SCAFFOLDING || kind == Kind.DRAIN
                || kind == Kind.LIGHTNING_ROD
                || kind == Kind.WET_IRON_TRAPDOOR ? pass == 0 || pass == 1 : pass == 0;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (kind == Kind.DEPTH_GLASS && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    /** Pinned Mth position seed followed by LegacyRandomSource.nextLong and WeightedBakedModel selection. */
    public static int scrapVariant(int x, int y, int z) {
        long seed = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        seed = (seed * seed * 42317861L + seed * 11L) >> 16;
        long random = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
        random = (random * 0x5DEECE66DL + 11) & ((1L << 48) - 1);
        random = (random * 0x5DEECE66DL + 11) & ((1L << 48) - 1);
        return Math.abs((int) (random >>> 16)) % 7;
    }

    public int depthConnections(IBlockAccess world, int x, int y, int z) {
        if (kind != Kind.DEPTH_GLASS) return 0;
        int mask = 0;
        if (world.getBlock(x, y - 1, z) == this) mask |= 1;
        if (world.getBlock(x, y + 1, z) == this) mask |= 2;
        if (world.getBlock(x, y, z - 1) == this) mask |= 4;
        if (world.getBlock(x, y, z + 1) == this) mask |= 8;
        if (world.getBlock(x - 1, y, z) == this) mask |= 16;
        if (world.getBlock(x + 1, y, z) == this) mask |= 32;
        return mask;
    }

    public boolean waterlogged(int metadata) {
        return kind == Kind.SCAFFOLDING && (metadata & 2) != 0 || kind == Kind.LIGHTNING_ROD
            || kind == Kind.WET_IRON_TRAPDOOR;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        switch (kind) {
            case LIGHTNING_ROD -> {
                switch (metadata) {
                    case 1 -> setBlockBounds(6 / 16F, 0, 6 / 16F, 10 / 16F, 1, 10 / 16F);
                    case 2, 3 -> setBlockBounds(6 / 16F, 6 / 16F, 0, 10 / 16F, 10 / 16F, 1);
                    case 4, 5 -> setBlockBounds(0, 6 / 16F, 6 / 16F, 1, 10 / 16F, 10 / 16F);
                    default -> setBlockBounds(0, 0, 0, 1, 1, 1);
                }
            }
            case WET_IRON_TRAPDOOR -> setBlockBounds(0, 13 / 16F, 0, 1, 1, 1);
            case CEILING_BUTTON -> {
                if (metadata == 4 || metadata == 5) setBlockBounds(6 / 16F, 14 / 16F, 5 / 16F, 10 / 16F, 1, 11 / 16F);
                else setBlockBounds(5 / 16F, 14 / 16F, 6 / 16F, 11 / 16F, 1, 10 / 16F);
            }
            default -> setBlockBounds(0, 0, 0, 1, 1, 1);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[textures.length];
        for (int i = 0; i < textures.length; i++) icons[i] = register.registerIcon("biomic:alexscaves/" + textures[i]);
        blockIcon = icons[0];
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {
        if (kind != Kind.SCAFFOLDING) {
            super.addCollisionBoxesToList(world, x, y, z, mask, boxes, entity);
            return;
        }
        // Source distance=0 scaffolding exposes its stable top/poles only to entities descending from above.
        if (entity != null && (entity.isSneaking() || entity.motionY > 0D || entity.boundingBox.minY < y + .999D))
            return;
        add(mask, boxes, x, y, z, 0, 14, 0, 16, 16, 16);
        add(mask, boxes, x, y, z, 0, 0, 0, 2, 16, 2);
        add(mask, boxes, x, y, z, 14, 0, 0, 16, 16, 2);
        add(mask, boxes, x, y, z, 0, 0, 14, 2, 16, 16);
        add(mask, boxes, x, y, z, 14, 0, 14, 16, 16, 16);
    }

    private static void add(AxisAlignedBB mask, List<AxisAlignedBB> boxes, int x, int y, int z, double x0, double y0,
        double z0, double x1, double y1, double z1) {
        AxisAlignedBB box = AxisAlignedBB
            .getBoundingBox(x + x0 / 16D, y + y0 / 16D, z + z0 / 16D, x + x1 / 16D, y + y1 / 16D, z + z1 / 16D);
        if (mask.intersectsWith(box)) boxes.add(box);
    }
}
