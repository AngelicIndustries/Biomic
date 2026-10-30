package industries.angelic.biomic.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle;

/**
 * Modern Nether ambient particles on 1.7.10's own particle sheet: spores are its smallest dot, ash its smoke puffs
 * shrinking with age. Motion and colours follow vanilla's spore and ash particles; soulblight spores follow Gardens of
 * the Dead (MIT).
 */
@SideOnly(Side.CLIENT)
public final class NetherAmbientParticle extends EntityFX {

    private final boolean ash;
    private final float friction;

    private NetherAmbientParticle(World world, double x, double y, double z, AmbientParticle kind) {
        // The moving constructor gives the small random drift vanilla's ash starts with. Spores start a little lower.
        super(world, x, isAsh(kind) ? y : y - .125, z, 0, 0, 0);
        noClip = true;
        setSize(.01F, .01F);
        ash = isAsh(kind);
        if (ash) {
            friction = .96F;
            particleScale *= .75F;
            particleMaxAge = lifetime(rand.nextFloat(), 20);
            motionX *= .1;
            motionY *= -.1;
            motionZ *= .1;
            setParticleTextureIndex(frame(0, particleMaxAge));
        } else {
            friction = 1F;
            particleScale *= rand.nextFloat() * .6F + .6F;
            particleMaxAge = lifetime(rand.nextFloat(), 16);
            setParticleTextureIndex(0);
        }
        switch (kind) {
            case CRIMSON_SPORE -> {
                setRBGColorF(.9F, .4F, .5F);
                motionX = rand.nextGaussian() * .002;
                motionY = .004 + rand.nextFloat() * .006;
                motionZ = rand.nextGaussian() * .002;
            }
            case WARPED_SPORE -> {
                setRBGColorF(.1F, .1F, .3F);
                motionX = motionZ = 0;
                motionY = rand.nextFloat() * -1.9 * rand.nextFloat() * .1;
            }
            case SOULBLIGHT_SPORE -> {
                setRBGColorF(0xB2 / 255F, 0x80 / 255F, 0x46 / 255F);
                motionX = rand.nextGaussian() * .01;
                motionY = Math.abs(rand.nextGaussian() * .03);
                motionZ = rand.nextGaussian() * .01;
                particleMaxAge *= 2;
            }
            case ASH -> {
                float grey = rand.nextFloat() * .5F;
                setRBGColorF(grey, grey, grey);
                particleGravity = .1F;
            }
            case WHITE_ASH -> {
                // Pale flakes blown sideways.
                setRBGColorF(.7294118F, .69411767F, .7607843F);
                particleGravity = .0125F;
                motionX += rand.nextFloat() * -1.9 * rand.nextFloat() * .1;
                motionY += rand.nextFloat() * -.5 * rand.nextFloat() * .5;
                motionZ += rand.nextFloat() * -1.9 * rand.nextFloat() * .1;
            }
            case SNOW -> {
                setRBGColorF(1F, 1F, 1F);
                particleGravity = .0125F;
                motionX += rand.nextGaussian() * .01;
                motionY -= .01 + rand.nextFloat() * .02;
                motionZ += rand.nextGaussian() * .01;
            }
        }
    }

    public static void spawn(World world, AmbientParticle kind, double x, double y, double z) {
        Minecraft.getMinecraft().effectRenderer.addEffect(new NetherAmbientParticle(world, x, y, z, kind));
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setDead();
            return;
        }
        if (ash) setParticleTextureIndex(frame(particleAge, particleMaxAge));
        motionY -= .04 * particleGravity;
        moveEntity(motionX, motionY, motionZ);
        motionX *= friction;
        motionY *= friction;
        motionZ *= friction;
    }

    private static boolean isAsh(AmbientParticle kind) {
        return kind == AmbientParticle.ASH || kind == AmbientParticle.WHITE_ASH || kind == AmbientParticle.SNOW;
    }

    /** Vanilla's lifetime: base ticks stretched by up to five times. */
    static int lifetime(float random, int base) {
        return Math.max(1, (int) (base / (random * .8F + .2F)));
    }

    /** The smoke puff for an age: the largest (7) when new, the smallest (0) at the end. */
    static int frame(int age, int lifetime) {
        return Math.max(0, 7 - age * 7 / lifetime);
    }
}
