package industries.angelic.biomic.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.content.alexscaves.PrimordialContentBlocks;

/** Client-only legacy adaptation of the pinned two-frame fly orbit used by open Primordial flytraps. */
@SideOnly(Side.CLIENT)
public final class PrimordialFlyParticle extends EntityFX {

    private final double orbitX, orbitY, orbitZ, offsetX, offsetZ;
    private final boolean reverseOrbit;
    private final float orbitSpeed, baseScale;

    private PrimordialFlyParticle(World world, double x, double y, double z) {
        super(world, x, y, z);
        particleScale *= 1F + world.rand.nextFloat() * .3F;
        baseScale = particleScale;
        orbitX = x;
        orbitY = y;
        orbitZ = z;
        particleMaxAge = 40 + world.rand.nextInt(10);
        offsetX = (.5F - rand.nextFloat()) * 2F;
        offsetZ = (.5F - rand.nextFloat()) * 2F;
        reverseOrbit = rand.nextBoolean();
        orbitSpeed = 3 + rand.nextFloat() * 3F;
        noClip = false;
        setParticleIcon(PrimordialContentBlocks.flyIcon(0));
    }

    public static void spawn(World world, double x, double y, double z) {
        Minecraft.getMinecraft().effectRenderer.addEffect(new PrimordialFlyParticle(world, x, y, z));
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        setParticleIcon(PrimordialContentBlocks.flyIcon(frame(particleAge)));
        if (particleAge++ >= particleMaxAge) {
            setDead();
            return;
        }
        double[] target = orbitPosition(particleAge, reverseOrbit, orbitSpeed, offsetX, offsetZ);
        double dx = orbitX + target[0] - posX, dy = orbitY - posY, dz = orbitZ + target[1] - posZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 1.0E-7D) {
            dx = dx / length * .1D;
            dy = dy / length * .1D;
            dz = dz / length * .1D;
        }
        motionX = dx + rand.nextGaussian() * .015D;
        motionY += dy + rand.nextGaussian() * .015D;
        if (onGround) motionY += .3D;
        motionZ += dz + rand.nextGaussian() * .015D;
        moveEntity(motionX, motionY, motionZ);
        motionX *= .8D;
        motionY *= .8D;
        motionZ *= .8D;
    }

    @Override
    public void renderParticle(Tessellator tess, float partial, float rotationX, float rotationXZ, float rotationZ,
        float rotationYZ, float rotationXY) {
        float old = particleScale;
        particleScale = baseScale
            * MathHelper.clamp_float((particleAge + partial) / (float) particleMaxAge * 16F, 0, 1);
        super.renderParticle(tess, partial, rotationX, rotationXZ, rotationZ, rotationYZ, rotationXY);
        particleScale = old;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    static int frame(int age) {
        return age % 4 >= 2 ? 1 : 0;
    }

    static double[] orbitPosition(float age, boolean reverse, float speed, double x, double z) {
        double angle = age * (reverse ? -speed : speed) * Math.PI / 180D, cos = Math.cos(angle), sin = Math.sin(angle);
        return new double[] { x * cos + z * sin, z * cos - x * sin };
    }
}
