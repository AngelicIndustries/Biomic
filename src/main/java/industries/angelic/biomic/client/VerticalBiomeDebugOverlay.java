package industries.angelic.biomic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.api.BiomicBiomeApi;

/** Makes Forge's 1.7 F3 biome line use Biomic's synchronized position-aware biome lookup. */
@SideOnly(Side.CLIENT)
public final class VerticalBiomeDebugOverlay {

    private static final String BIOME_PREFIX = " b: ";
    private static final String BLOCK_LIGHT_PREFIX = " bl: ";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void replaceSurfaceBiomeName(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.theWorld;
        EntityPlayer player = minecraft.thePlayer;
        if (world == null || player == null || !minecraft.gameSettings.showDebugInfo) {
            return;
        }

        int blockX = MathHelper.floor_double(player.posX);
        int blockY = MathHelper.floor_double(player.boundingBox.minY);
        int blockZ = MathHelper.floor_double(player.posZ);
        BiomeGenBase biome = BiomicBiomeApi.getBiome(world, blockX, blockY, blockZ);
        for (int index = 0; index < event.left.size(); index++) {
            String line = event.left.get(index);
            String replacement = replaceBiomeName(line, biome.biomeName);
            if (replacement != line) {
                event.left.set(index, replacement);
                return;
            }
        }
    }

    static String replaceBiomeName(String line, String biomeName) {
        if (line == null || biomeName == null) {
            return line;
        }
        int biomeStart = line.indexOf(BIOME_PREFIX);
        int biomeEnd = line.indexOf(BLOCK_LIGHT_PREFIX, biomeStart + BIOME_PREFIX.length());
        if (biomeStart < 0 || biomeEnd < 0) {
            return line;
        }
        return line.substring(0, biomeStart + BIOME_PREFIX.length()) + biomeName + line.substring(biomeEnd);
    }
}
