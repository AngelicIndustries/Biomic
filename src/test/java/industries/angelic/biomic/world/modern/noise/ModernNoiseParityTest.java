package industries.angelic.biomic.world.modern.noise;

import org.junit.Assert;
import org.junit.Test;

/** Values captured directly from the pinned 1.21.1 named runtime. */
public class ModernNoiseParityTest {

    @Test
    public void xoroshiroStreamMatchesReference() {
        ModernXoroshiroRandom random = new ModernXoroshiroRandom(42L);
        long[] expected = { -4695948378737616609L, 7341713790291473579L, -7542733514721318211L, 4888889476139319686L,
            8419651034331256779L, -6491934549477179079L, 8279452174680803839L, 6246239634032559210L };
        for (long value : expected) Assert.assertEquals(value, random.nextLong());
    }

    @Test
    public void worldgenDecorationAndBoundedRollsMatchPinnedRuntime() {
        ModernWorldgenRandom random = new ModernWorldgenRandom();
        long decorationSeed = random.setDecorationSeed(42L, 3168, -4032);
        Assert.assertEquals(3378567511191176394L, decorationSeed);
        random.setFeatureSeed(decorationSeed, 232, 9);
        Assert.assertEquals(5, random.nextInt(16));
        Assert.assertEquals(8, random.nextInt(16));
        Assert.assertEquals(10, random.nextInt(17));
    }

    @Test
    public void perlinCompositionMatchesReference() {
        ModernPositionalRandom root = new ModernXoroshiroRandom(42L).forkPositional();
        ModernPerlinNoise noise = new ModernPerlinNoise(root.fromHashOf("parity-test"), -3, 1.0D, 0.5D, 0.0D, 2.0D);
        Assert.assertEquals(-0.09673975812245618D, noise.sample(0.0D, 0.0D, 0.0D), 1.0E-15D);
        Assert.assertEquals(0.0804752640311255D, noise.sample(12.5D, -31.25D, 999.75D), 1.0E-15D);
    }

    @Test
    public void normalNoiseMatchesReferenceAtNearAndFarCoordinates() {
        ModernNormalNoise noise = ModernNormalNoise.named(42L, "minecraft:temperature", -10, 1.5D, 0.0D, 1.0D, 0.5D);
        Assert.assertEquals(0.12754031548231795D, noise.sample(0.0D, 0.0D, 0.0D), 1.0E-15D);
        Assert.assertEquals(0.4290930389571113D, noise.sample(12.5D, -31.25D, 999.75D), 1.0E-15D);
        Assert.assertEquals(0.6159901150893637D, noise.sample(-1048576.125D, 63.0D, 33554431.75D), 1.0E-15D);
    }
}
