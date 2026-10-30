package industries.angelic.biomic.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import industries.angelic.biomic.Biomic;

/** Small, versioned packet boundary for Biomic's client-facing diagnostics. */
public final class BiomicNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Biomic.MODID);
    private static boolean initialized;

    private BiomicNetwork() {}

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        CHANNEL.registerMessage(CopyPositionMessage.Handler.class, CopyPositionMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(
            industries.angelic.biomic.browser.BrowserMessages.Request.Handler.class,
            industries.angelic.biomic.browser.BrowserMessages.Request.class,
            1,
            Side.SERVER);
        CHANNEL.registerMessage(
            industries.angelic.biomic.browser.BrowserMessages.Catalogue.Handler.class,
            industries.angelic.biomic.browser.BrowserMessages.Catalogue.class,
            2,
            Side.CLIENT);
        CHANNEL.registerMessage(
            industries.angelic.biomic.browser.BrowserMessages.Go.Handler.class,
            industries.angelic.biomic.browser.BrowserMessages.Go.class,
            3,
            Side.SERVER);
        CHANNEL.registerMessage(
            industries.angelic.biomic.browser.BrowserMessages.Status.Handler.class,
            industries.angelic.biomic.browser.BrowserMessages.Status.class,
            4,
            Side.CLIENT);
        CHANNEL.registerMessage(
            industries.angelic.biomic.browser.BrowserMessages.GoTo.Handler.class,
            industries.angelic.biomic.browser.BrowserMessages.GoTo.class,
            5,
            Side.SERVER);
        CHANNEL.registerMessage(
            industries.angelic.biomic.browser.BrowserMessages.Survey.Handler.class,
            industries.angelic.biomic.browser.BrowserMessages.Survey.class,
            6,
            Side.SERVER);
        CHANNEL.registerMessage(
            industries.angelic.biomic.browser.BrowserMessages.Distances.Handler.class,
            industries.angelic.biomic.browser.BrowserMessages.Distances.class,
            7,
            Side.CLIENT);
        initialized = true;
    }

    public static void send(cpw.mods.fml.common.network.simpleimpl.IMessage message,
        net.minecraft.entity.player.EntityPlayerMP player) {
        CHANNEL.sendTo(message, player);
    }

    public static void sendToServer(cpw.mods.fml.common.network.simpleimpl.IMessage message) {
        CHANNEL.sendToServer(message);
    }

}
