package industries.angelic.biomic.core;

import java.util.List;
import java.util.Set;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import industries.angelic.angellib.core.AngelEarlyMixinLoader;
import industries.angelic.biomic.Biomic;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinLoader extends AngelEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins." + Biomic.MODID + ".early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return Mixins.getEarlyMixinsForLoader();
    }
}
