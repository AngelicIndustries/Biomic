package industries.angelic.biomic.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicWoods;
import industries.angelic.biomic.content.entity.EntityWoodBoat;

/**
 * Draws a wood's boat: vanilla's boat placement and hit wobble, the modern boat model, the wood's RU art. Unmoved,
 * the modern hull's gunwale sits where vanilla's does (3/16 over the boat's position), so riders sit in it as in
 * vanilla's.
 */
public final class WoodBoatRenderer extends Render {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[BiomicWoods.Wood.values().length];
    private final WoodBoatModel model = new WoodBoatModel();

    public WoodBoatRenderer() {
        shadowSize = 0.5F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityWoodBoat boat = (EntityWoodBoat) entity;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        float hurt = boat.getTimeSinceHit() - partialTicks,
            damage = Math.max(0.0F, boat.getDamageTaken() - partialTicks);
        if (hurt > 0.0F)
            GL11.glRotatef(MathHelper.sin(hurt) * hurt * damage / 10.0F * boat.getForwardDirection(), 1.0F, 0.0F, 0.0F);
        bindEntityTexture(boat);
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        model.render(boat, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        BiomicWoods.Wood wood = ((EntityWoodBoat) entity).wood();
        ResourceLocation texture = TEXTURES[wood.ordinal()];
        if (texture == null) TEXTURES[wood
            .ordinal()] = texture = new ResourceLocation(Biomic.MODID, "textures/entity/boat/" + wood.id + ".png");
        return texture;
    }
}
