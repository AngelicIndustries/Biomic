package industries.angelic.biomic.client;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import industries.angelic.biomic.content.alexscaves.BlockAbyssalAquatic;
import industries.angelic.biomic.world.alexscaves.AlexCavesSimplexNoise;

/** Source custom-render particle: an ephemeral animal visual, never a world entity or spawn entry. */
public final class AbyssalTubeWormEffects {

    private static final int MAX_ACTIVE = 256;
    private static final Set<Long> ACTIVE = new HashSet<>();
    private static World activeWorld;

    private AbyssalTubeWormEffects() {}

    public static void add(int x, int y, int z, Random random) {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.theWorld;
        if (world == null) return;
        if (world != activeWorld) {
            activeWorld = world;
            ACTIVE.clear();
        }
        long key = key(x, y, z);
        if (ACTIVE.size() >= MAX_ACTIVE || !ACTIVE.add(key)) return;
        minecraft.effectRenderer.addEffect(new WormFX(world, x, y, z, key, random));
    }

    static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    static float emergence(float age, float tuck, float offset) {
        float open = 1F - tuck;
        return open * 10F - open * (1F + (float) Math.sin(age * .3F + offset)) * 2F + 5F;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) y & 0xFFFL;
    }

    private static final class WormFX extends EntityFX {

        private static final ResourceLocation TEXTURE = new ResourceLocation(
            "biomic",
            "textures/particles/alexscaves/tube_worm.png");
        private static final WormModel MODEL = new WormModel();
        private final int blockX, blockY, blockZ;
        private final long activeKey;
        private final float offset, yaw;
        private int scareCooldown;
        private float tuck = 1F, previousTuck = 1F;
        private boolean scared;

        WormFX(World world, int x, int y, int z, long activeKey, Random random) {
            super(world, x + .5, y + .5, z + .5);
            blockX = x;
            blockY = y;
            blockZ = z;
            this.activeKey = activeKey;
            particleMaxAge = 90 + random.nextInt(50);
            offset = (float) AlexCavesSimplexNoise.sample3D(x, y, z, 6D);
            // Source: Direction.from2DDataValue(2 + nextInt(3)): north, east or south.
            yaw = switch (random.nextInt(3)) {
                case 0 -> 180F;
                case 1 -> 270F;
                default -> 0F;
            };
            scareCooldown = 5 + random.nextInt(10);
            particleGravity = 0;
            setSize(.6F, 1.5F);
        }

        @Override
        public void onUpdate() {
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            previousTuck = tuck;
            if (particleAge++ >= particleMaxAge
                || !BlockAbyssalAquatic.supportsEmergingWorm(worldObj, blockX, blockY, blockZ)) {
                setDead();
                return;
            }
            if (scareCooldown-- <= 0) {
                scareCooldown = 10 + rand.nextInt(10);
                scared = !worldObj
                    .getEntitiesWithinAABB(
                        EntityLivingBase.class,
                        AxisAlignedBB.getBoundingBox(
                            blockX - 4.5,
                            blockY - 1,
                            blockZ - 4.5,
                            blockX + 5.5,
                            blockY + 3.5,
                            blockZ + 5.5))
                    .isEmpty();
            }
            float target = scared || particleAge >= particleMaxAge - 10 ? 1F : 0F;
            tuck = approach(tuck, target, .1F);
        }

        @Override
        public void setDead() {
            super.setDead();
            if (worldObj == activeWorld) ACTIVE.remove(activeKey);
        }

        @Override
        public int getFXLayer() {
            return 3;
        }

        @Override
        public void renderParticle(Tessellator ignored, float partial, float rx, float rz, float ryz, float rxy,
            float rxz) {
            float renderTuck = previousTuck + (tuck - previousTuck) * partial;
            double x = prevPosX + (posX - prevPosX) * partial - interpPosX,
                y = prevPosY + (posY - prevPosY) * partial - interpPosY,
                z = prevPosZ + (posZ - prevPosZ) * partial - interpPosZ;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            try {
                Minecraft.getMinecraft()
                    .getTextureManager()
                    .bindTexture(TEXTURE);
                // Source render type entityCutoutNoCull, lit by the world like an entity.
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, .1F);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glColor4f(1, 1, 1, 1);
                int light = getBrightnessForRender(partial);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
                RenderHelper.enableStandardItemLighting();
                // The particle sits at the tube block's centre; one block up and flipped, model Y 24 is the block
                // floor.
                GL11.glTranslated(x, y + 1, z);
                GL11.glScalef(-1, -1, 1);
                MODEL.render(particleAge + partial, renderTuck, offset, yaw);
            } finally {
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }

        @Override
        public boolean shouldRenderInPass(int pass) {
            return pass == 1;
        }

        @Override
        public boolean isInRangeToRenderDist(double distance) {
            return distance < 4096D;
        }
    }

    /**
     * Source TubeWormModel on Citadel's box transform: pivot, then Z, Y and X rotation, then the box's own scale, so
     * the half-height tuck squashes around the pivot. Wobble is Citadel's walk/flap (cosine).
     */
    private static final class WormModel extends ModelBase {

        final ModelRenderer worm;

        WormModel() {
            textureWidth = 64;
            textureHeight = 64;
            worm = new ModelRenderer(this);
            worm.setTextureOffset(18, 23)
                .addBox(-3, -18, -3, 6, 3, 6);
            worm.setTextureOffset(0, 0)
                .addBox(-3, -15, -3, 6, 15, 6);
        }

        void render(float age, float tuck, float offset, float yaw) {
            float open = 1F - tuck;
            GL11.glTranslatef(0, (24F - emergence(age, tuck, offset)) / 16F, 0);
            GL11.glRotatef((float) Math.toDegrees(Math.cos(age * .35F + offset + 2F) * .2F * open), 0, 0, 1);
            GL11.glRotatef(yaw, 0, 1, 0);
            GL11.glRotatef((float) Math.toDegrees(Math.cos(age * .35F + offset - 1F) * .15F * open), 1, 0, 0);
            GL11.glScalef(1, Math.min(.5F + open, 1F), 1);
            worm.render(1 / 16F);
        }
    }
}
