package industries.angelic.biomic.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Legacy translucent, vanilla-smoke-sheet adaptation of the source pink whistle particle. */
@SideOnly(Side.CLIENT)
public final class GardensWhistleSmokeParticle extends EntityFX {

    private GardensWhistleSmokeParticle(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        motionX = vx;
        motionY = vy;
        motionZ = vz;
        particleMaxAge = lifetime(world.rand.nextFloat());
        particleAlpha = .9F;
        particleRed = 1F;
        particleGreen = 181 / 255F;
        particleBlue = 206 / 255F;
        setParticleIcon(icon(0, particleMaxAge));
    }

    public static void spawn(World world, double x, double y, double z, double vx, double vy, double vz) {
        Minecraft.getMinecraft().effectRenderer.addEffect(new GardensWhistleSmokeParticle(world, x, y, z, vx, vy, vz));
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
        setParticleIcon(icon(particleAge, particleMaxAge));
        motionY -= 1.2E-7D;
        moveEntity(motionX, motionY, motionZ);
        motionX *= .98D;
        motionY *= .98D;
        motionZ *= .98D;
        if (onGround) {
            motionX *= .7D;
            motionZ *= .7D;
        }
    }

    static int lifetime(float random) {
        return Math.max((int) (8 / (random * .8F + .2F)), 1);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    private static net.minecraft.util.IIcon icon(int age, int lifetime) {
        return ((industries.angelic.biomic.content.gardens.BlockWhistlecane) industries.angelic.biomic.content.gardens.GardensContent.WHISTLECANE)
            .whistleSmokeIcon(frame(age, lifetime));
    }

    static int frame(int age, int lifetime) {
        return Math.min(11, age * 12 / lifetime);
    }
}
