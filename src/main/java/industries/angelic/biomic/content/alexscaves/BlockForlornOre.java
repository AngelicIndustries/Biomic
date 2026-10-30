package industries.angelic.biomic.content.alexscaves;

import java.util.Random;

import net.minecraft.block.BlockOre;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

/** Host-textured resource ore; loot matches the corresponding vanilla coal/redstone ore. */
final class BlockForlornOre extends BlockOre {

    enum Kind {
        COAL,
        REDSTONE
    }

    private final Kind kind;

    BlockForlornOre(String name, Kind kind, float hardness, float resistance) {
        this.kind = kind;
        setBlockName("biomic.alexs_forlorn_" + name);
        setBlockTextureName("biomic:alexscaves/" + name);
        setHardness(hardness);
        setResistance(resistance);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", kind == Kind.REDSTONE ? 2 : 0);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return kind == Kind.COAL ? Items.coal : Items.redstone;
    }

    @Override
    public int quantityDropped(Random random) {
        return kind == Kind.COAL ? 1 : 4 + random.nextInt(2);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (fortune <= 0) return quantityDropped(random);
        int multiplier = random.nextInt(fortune + 2) - 1;
        if (multiplier < 0) multiplier = 0;
        return quantityDropped(random) * (multiplier + 1);
    }
}
