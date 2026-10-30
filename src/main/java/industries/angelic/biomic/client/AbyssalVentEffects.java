package industries.angelic.biomic.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.content.alexscaves.AbyssalVentBehavior;
import industries.angelic.biomic.content.alexscaves.BlockAbyssalAquatic;

/** Client-only natural vent port of Alex's Caves 4718f428 (LGPL-3.0-only), no server tile/entity payload. */
@SideOnly(Side.CLIENT)
public final class AbyssalVentEffects {

    public static final AbyssalVentEffects INSTANCE = new AbyssalVentEffects();
    private static final int MAX_EMITTERS = 256, DISCOVERY_TTL = 200;
    private final Map<ChunkCoordinates, Emitter> emitters = new LinkedHashMap<>();
    private WorldClient world;
    private long previousTick = Long.MIN_VALUE;

    private static final class Emitter {

        long expires;
        int soundTime;

        Emitter(long expires) {
            this.expires = expires;
        }
    }

    private AbyssalVentEffects() {}

    /** Legacy randomDisplayTick discovers only; repeated discovery never resets the source sound cooldown. */
    public static void track(int x, int y, int z) {
        WorldClient current = Minecraft.getMinecraft().theWorld;
        INSTANCE.bind(current);
        if (current == null) return;
        ChunkCoordinates p = new ChunkCoordinates(x, y, z);
        Emitter old = INSTANCE.emitters.get(p);
        if (old != null) {
            old.expires = current.getTotalWorldTime() + DISCOVERY_TTL;
            return;
        }
        if (INSTANCE.emitters.size() < MAX_EMITTERS)
            INSTANCE.emitters.put(p, new Emitter(current.getTotalWorldTime() + DISCOVERY_TTL));
    }

    private void bind(WorldClient current) {
        if (current != world) {
            emitters.clear();
            world = current;
            previousTick = Long.MIN_VALUE;
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        bind(mc.theWorld);
        if (world == null) return;
        long now = world.getTotalWorldTime();
        if (now == previousTick) return;
        previousTick = now;
        Iterator<Map.Entry<ChunkCoordinates, Emitter>> iterator = emitters.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ChunkCoordinates p = entry.getKey();
            Emitter emitter = entry.getValue();
            if (now > emitter.expires || p.posY < 1
                || p.posY + 1 >= world.getActualHeight()
                || !world.blockExists(p.posX, p.posY - 1, p.posZ)
                || !world.blockExists(p.posX, p.posY + 1, p.posZ)) {
                iterator.remove();
                continue;
            }
            if (!(world.getBlock(p.posX, p.posY, p.posZ) instanceof BlockAbyssalAquatic block)
                || !AbyssalVentBehavior.vent(block.kind())) {
                iterator.remove();
                continue;
            }
            int meta = world.getBlockMetadata(p.posX, p.posY, p.posZ);
            var above = world.getBlock(p.posX, p.posY + 1, p.posZ);
            boolean aboveVent = above instanceof BlockAbyssalAquatic a && AbyssalVentBehavior.vent(a.kind());
            if (!AbyssalVentBehavior.emits(meta) || aboveVent
                || above.getMaterial()
                    .blocksMovement()
                || mc.thePlayer == null
                || mc.thePlayer.getDistanceSq(p.posX + .5, p.posY + .5, p.posZ + .5) > 14400
                || world.rand.nextBoolean()) continue;
            boolean secondary = world.rand.nextInt(3) == 0;
            int smoke = meta & 3;
            float x = (world.rand.nextFloat() - .5F) * .25F, z = (world.rand.nextFloat() - .5F) * .25F;
            double vx = x * .15F, vy = .03F + world.rand.nextFloat() * .2F, vz = z * .15F;
            if (secondary && smoke == 1)
                world.spawnParticle("explode", p.posX + .5 + x, p.posY + 1, p.posZ + .5 + z, vx, vy, vz);
            else mc.effectRenderer
                .addEffect(new SmokeFX(world, p.posX + .5 + x, p.posY + 1, p.posZ + .5 + z, vx, vy, vz, smoke));
            // 1.7 lacks modern squid-ink FX: its black secondary uses the source custom black plume instead.
            if (emitter.soundTime-- <= 0) {
                emitter.soundTime = 30 + world.rand.nextInt(20);
                boolean underwater = (meta & 8) != 0
                    || above.getMaterial() == net.minecraft.block.material.Material.water
                    || above.getMaterial() == net.minecraft.block.material.Material.lava;
                world.playSound(
                    p.posX + .5,
                    p.posY + .5,
                    p.posZ + .5,
                    "biomic:abyssal_vent_bubble" + (underwater ? "_underwater" : ""),
                    underwater ? 2.5F : 1.5F,
                    .8F + world.rand.nextFloat() * .4F,
                    false);
            }
        }
    }

    private static final class SmokeFX extends EntityFX {

        private final ResourceLocation texture;
        private final float quad;

        SmokeFX(World world, double x, double y, double z, double vx, double vy, double vz, int smoke) {
            super(world, x, y, z);
            motionX = vx;
            motionY = vy;
            motionZ = vz;
            setSize(.5F, .5F);
            quad = .8F + world.rand.nextFloat() * .3F;
            particleMaxAge = AbyssalVentBehavior.lifetime(world.rand.nextDouble());
            texture = new ResourceLocation(
                "biomic",
                "textures/particles/alexscaves/vent/big_smoke_" + world.rand.nextInt(12) + ".png");
            float color = (smoke == 2 ? .1F : .95F) + world.rand.nextFloat() * .05F;
            particleRed = particleGreen = particleBlue = color;
        }

        @Override
        public void onUpdate() {
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            particleAlpha = AbyssalVentBehavior.alpha(particleAge, particleMaxAge);
            if (particleAge++ >= particleMaxAge) {
                setDead();
                return;
            }
            moveEntity(motionX, motionY, motionZ);
            motionX *= .99;
            motionY *= .99;
            motionZ *= .99;
        }

        @Override
        public int getFXLayer() {
            return 3;
        }

        @Override
        public boolean shouldRenderInPass(int pass) {
            return pass == 1;
        }

        @Override
        public boolean isInRangeToRenderDist(double distance) {
            return distance <= 14400;
        }

        @Override
        public void renderParticle(Tessellator tess, float partial, float rx, float rz, float ryz, float rxy,
            float rxz) {
            float s = quad * AbyssalVentBehavior.growth(particleAge, partial, particleMaxAge);
            double x = prevPosX + (posX - prevPosX) * partial - interpPosX,
                y = prevPosY + (posY - prevPosY) * partial - interpPosY,
                z = prevPosZ + (posZ - prevPosZ) * partial - interpPosZ;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            try {
                Minecraft.getMinecraft()
                    .getTextureManager()
                    .bindTexture(texture);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthMask(false);
                tess.startDrawingQuads();
                tess.setBrightness(getBrightnessForRender(partial));
                tess.setColorRGBA_F(particleRed, particleGreen, particleBlue, particleAlpha);
                tess.addVertexWithUV(x - rx * s - rxy * s, y - rz * s, z - ryz * s - rxz * s, 1, 1);
                tess.addVertexWithUV(x - rx * s + rxy * s, y + rz * s, z - ryz * s + rxz * s, 1, 0);
                tess.addVertexWithUV(x + rx * s + rxy * s, y + rz * s, z + ryz * s + rxz * s, 0, 0);
                tess.addVertexWithUV(x + rx * s - rxy * s, y - rz * s, z + ryz * s - rxz * s, 0, 1);
                tess.draw();
            } finally {
                GL11.glPopAttrib();
            }
        }
    }
}
