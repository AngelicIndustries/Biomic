package industries.angelic.biomic.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * The boat RU's boat art is drawn for (Minecraft's since 1.9: a hull of five boards and two oars, on a 128 by 64
 * sheet), with the hull's long side along x like 1.7.10's boat. 1.7.10's boat does not row, so the oars rest where
 * modern Minecraft's rest between strokes.
 */
final class WoodBoatModel extends ModelBase {

    /** Modern Minecraft's oar angles at rowing time 0. */
    private static final float OAR_PITCH = -0.6544985F, OAR_YAW = 0.6608898F;
    private final ModelRenderer[] parts = new ModelRenderer[7];

    WoodBoatModel() {
        textureWidth = 128;
        textureHeight = 64;
        parts[0] = part(0, 0, 0, 3, 1, (float) Math.PI / 2, 0, 0);
        parts[0].addBox(-14, -9, -3, 28, 16, 3);
        parts[1] = part(0, 19, -15, 4, 4, 0, (float) Math.PI * 1.5F, 0);
        parts[1].addBox(-13, -7, -1, 18, 6, 2);
        parts[2] = part(0, 27, 15, 4, 0, 0, (float) Math.PI / 2, 0);
        parts[2].addBox(-8, -7, -1, 16, 6, 2);
        parts[3] = part(0, 35, 0, 4, -9, 0, (float) Math.PI, 0);
        parts[3].addBox(-14, -7, -1, 28, 6, 2);
        parts[4] = part(0, 43, 0, 4, 9, 0, 0, 0);
        parts[4].addBox(-14, -7, -1, 28, 6, 2);
        parts[5] = part(62, 0, 3, -5, 9, OAR_PITCH, OAR_YAW, 0.19634955F);
        parts[5].addBox(-1, 0, -5, 2, 2, 18);
        parts[5].addBox(-1.001F, -3, 8, 1, 6, 7);
        parts[6] = part(62, 20, 3, -5, -9, OAR_PITCH, (float) Math.PI - OAR_YAW, 0.19634955F);
        parts[6].addBox(-1, 0, -5, 2, 2, 18);
        parts[6].addBox(0.001F, -3, 8, 1, 6, 7);
    }

    private ModelRenderer part(int u, int v, float x, float y, float z, float pitch, float yaw, float roll) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.setRotationPoint(x, y, z);
        part.rotateAngleX = pitch;
        part.rotateAngleY = yaw;
        part.rotateAngleZ = roll;
        return part;
    }

    @Override
    public void render(Entity entity, float swing, float swingAmount, float age, float yaw, float pitch, float scale) {
        for (ModelRenderer part : parts) part.render(scale);
    }
}
