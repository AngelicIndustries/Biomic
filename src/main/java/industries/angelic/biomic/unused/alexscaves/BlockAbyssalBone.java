package industries.angelic.biomic.unused.alexscaves;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.content.alexscaves.AbyssalClientRendering;

/** Source whale-bone shapes. Complex blocks render as exact unions of these cuboids. */
public final class BlockAbyssalBone extends Block {

    public enum Kind {
        SMOOTH,
        SLAB,
        STAIR_STRAIGHT,
        STAIR_INNER,
        STAIR_OUTER,
        WALL,
        HOLLOW,
        THIN,
        NODULE,
        RIBS,
        BALEEN,
        WORMS
    }

    public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {}

    private final Kind kind;
    private final String texture;

    public BlockAbyssalBone(String name, Kind kind) {
        this(name, kind, null);
    }

    public BlockAbyssalBone(String name, Kind kind, String texture) {
        super(kind == Kind.WORMS ? Material.water : Material.rock);
        this.kind = kind;
        this.texture = texture;
        setBlockName("biomic." + name);
        setHardness(kind == Kind.WORMS ? 0F : 2F);
        setResistance(kind == Kind.WORMS ? 0F : 6F);
        setStepSound(kind == Kind.WORMS ? soundTypeGrass : soundTypeStone);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public boolean isOpaqueCube() {
        return kind == Kind.SMOOTH;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return kind == Kind.SMOOTH;
    }

    @Override
    public int getRenderType() {
        return kind == Kind.SMOOTH ? 0
            : kind == Kind.WORMS ? AbyssalClientRendering.generatedItemRenderId() : AbyssalClientRendering.renderId();
    }

    @Override
    public int getRenderBlockPass() {
        return kind == Kind.WORMS ? 1 : 0;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return kind == Kind.WORMS ? (pass == 0 || pass == 1) : pass == 0;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        if (kind == Kind.WORMS) return null;
        Box bounds = envelope(cuboids(world.getBlockMetadata(x, y, z)));
        return AxisAlignedBB
            .getBoundingBox(x + bounds.x0, y + bounds.y0, z + bounds.z0, x + bounds.x1, y + bounds.y1, z + bounds.z1);
    }

    @Override
    public void setBlockBoundsBasedOnState(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        Box b = envelope(cuboids(world.getBlockMetadata(x, y, z)));
        setBlockBounds((float) b.x0, (float) b.y0, (float) b.z0, (float) b.x1, (float) b.y1, (float) b.z1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        String selected = texture != null ? texture : switch (kind) {
            case HOLLOW -> "hollow_bone";
            case THIN, BALEEN -> "thin_bone";
            case NODULE -> "bone_nodule";
            case RIBS -> "bone_ribs";
            case WORMS -> "bone_worms";
            default -> "smooth_bone";
        };
        blockIcon = register.registerIcon("biomic:alexscaves/" + selected);
    }

    public List<Box> cuboids(int metadata) {
        return switch (kind) {
            case SMOOTH -> List.of(box(0, 0, 0, 16, 16, 16));
            case SLAB -> List.of((metadata & 1) == 0 ? box(0, 0, 0, 16, 8, 16) : box(0, 8, 0, 16, 16, 16));
            case STAIR_STRAIGHT -> stairs(metadata, false, false);
            case STAIR_INNER -> stairs(metadata, true, false);
            case STAIR_OUTER -> stairs(metadata, false, true);
            case WALL -> walls(metadata);
            case HOLLOW -> hollow(metadata);
            case THIN -> thin(metadata);
            case NODULE -> attached(metadata, 4, 12);
            case RIBS -> ribs(metadata);
            case BALEEN -> baleen(metadata);
            case WORMS -> attachedWide(metadata & 7);
        };
    }

    private static List<Box> stairs(int meta, boolean inner, boolean outer) {
        boolean top = (meta & 4) != 0, right = (meta & 8) != 0;
        int facing = meta & 3;
        double y0 = top ? 8 : 0, y1 = top ? 16 : 8;
        java.util.ArrayList<Box> out = new java.util.ArrayList<>();
        out.add(box(0, y0, 0, 16, y1, 16));
        double sy0 = top ? 0 : 8, sy1 = top ? 8 : 16;
        Box half = switch (facing) {
            case 0 -> box(0, sy0, 8, 16, sy1, 16);
            case 1 -> box(0, sy0, 0, 8, sy1, 16);
            case 2 -> box(0, sy0, 0, 16, sy1, 8);
            default -> box(8, sy0, 0, 16, sy1, 16);
        };
        out.add(half);
        if (inner) out.add(switch (facing) {
            case 0 -> right ? box(8, sy0, 0, 16, sy1, 8) : box(0, sy0, 0, 8, sy1, 8);
            case 1 -> right ? box(8, sy0, 8, 16, sy1, 16) : box(8, sy0, 0, 16, sy1, 8);
            case 2 -> right ? box(0, sy0, 8, 8, sy1, 16) : box(8, sy0, 8, 16, sy1, 16);
            default -> right ? box(0, sy0, 0, 8, sy1, 8) : box(0, sy0, 8, 8, sy1, 16);
        });
        if (outer) {
            out.remove(1);
            out.add(switch (facing) {
                case 0 -> right ? box(8, sy0, 8, 16, sy1, 16) : box(0, sy0, 8, 8, sy1, 16);
                case 1 -> right ? box(0, sy0, 8, 8, sy1, 16) : box(0, sy0, 0, 8, sy1, 8);
                case 2 -> right ? box(0, sy0, 0, 8, sy1, 8) : box(8, sy0, 0, 16, sy1, 8);
                default -> right ? box(8, sy0, 0, 16, sy1, 8) : box(8, sy0, 8, 16, sy1, 16);
            });
        }
        return out;
    }

    private static List<Box> walls(int meta) {
        java.util.ArrayList<Box> out = new java.util.ArrayList<>();
        boolean straightNS = (meta & 3) == 3 && (meta & 12) == 0, straightEW = (meta & 12) == 12 && (meta & 3) == 0;
        if (!straightNS && !straightEW) out.add(box(4, 0, 4, 12, 16, 12));
        if ((meta & 1) != 0) out.add(box(5, 0, 0, 11, 14, 8));
        if ((meta & 2) != 0) out.add(box(5, 0, 8, 11, 14, 16));
        if ((meta & 4) != 0) out.add(box(0, 0, 5, 8, 14, 11));
        if ((meta & 8) != 0) out.add(box(8, 0, 5, 16, 14, 11));
        return out;
    }

    private static List<Box> hollow(int axis) {
        return switch (axis) {
            case 0 -> List.of(
                box(0, 0, 0, 16, 3, 16),
                box(0, 13, 0, 16, 16, 16),
                box(0, 3, 0, 16, 13, 3),
                box(0, 3, 13, 16, 13, 16));
            case 1 -> List.of(
                box(0, 0, 0, 3, 16, 16),
                box(13, 0, 0, 16, 16, 16),
                box(3, 0, 0, 13, 16, 3),
                box(3, 0, 13, 13, 16, 16));
            default -> List.of(
                box(0, 0, 0, 16, 16, 3),
                box(0, 0, 13, 16, 16, 16),
                box(0, 0, 3, 3, 16, 13),
                box(13, 0, 3, 16, 16, 13));
        };
    }

    private static List<Box> thin(int meta) {
        int axis = meta / 3, offset = meta % 3;
        double shift = (offset - 1) * 6;
        return List.of(switch (axis) {
            case 0 -> box(0, 6 + shift, 6, 16, 10 + shift, 10);
            case 1 -> box(6, 0, 6, 10, 16, 10);
            default -> box(6, 6 + shift, 0, 10, 10 + shift, 16);
        });
    }

    private static List<Box> attached(int facing, int from, int to) {
        return List.of(switch (facing) {
            case 0 -> box(4, 16 - to, 4, 12, 16 - from, 12);
            case 1 -> box(4, from, 4, 12, to, 12);
            case 2 -> box(4, 4, 16 - to, 12, 12, 16 - from);
            case 3 -> box(4, 4, from, 12, 12, to);
            case 4 -> box(16 - to, 4, 4, 16 - from, 12, 12);
            default -> box(from, 4, 4, to, 12, 12);
        });
    }

    private static List<Box> attachedWide(int facing) {
        return List.of(switch (facing) {
            case 0 -> box(1, 12, 1, 15, 16, 15);
            case 1 -> box(1, 0, 1, 15, 4, 15);
            case 2 -> box(1, 1, 12, 15, 15, 16);
            case 3 -> box(1, 1, 0, 15, 15, 4);
            case 4 -> box(12, 1, 1, 16, 15, 15);
            default -> box(0, 1, 1, 4, 15, 15);
        });
    }

    private static List<Box> ribs(int meta) {
        int facing = meta & 3;
        boolean under = (meta & 4) != 0;
        java.util.ArrayList<Box> out = new java.util.ArrayList<>();
        out.add(under ? box(0, 0, 0, 16, 2, 16) : box(0, 14, 0, 16, 16, 16));
        out.add(switch (facing) {
            case 0 -> box(0, 0, 0, 16, 16, 2);
            case 1 -> box(14, 0, 0, 16, 16, 16);
            case 2 -> box(0, 0, 14, 16, 16, 16);
            default -> box(0, 0, 0, 2, 16, 16);
        });
        return out;
    }

    private static List<Box> baleen(int meta) {
        return (meta & 1) == 0 ? List.of(box(6, 0, 0, 10, 4, 16), box(7.5, 0, 0, 8.5, 16, 16))
            : List.of(box(0, 0, 6, 16, 4, 10), box(0, 0, 7.5, 16, 16, 8.5));
    }

    private static Box box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new Box(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
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
}
