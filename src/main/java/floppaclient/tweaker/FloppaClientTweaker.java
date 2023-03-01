package floppaclient.tweaker;

import gg.essential.loader.stage0.EssentialSetupTweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

@SuppressWarnings("unused")
public class FloppaClientTweaker extends EssentialSetupTweaker {

    public static LaunchClassLoader launchClassLoader;

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        super.injectIntoClassLoader(classLoader);
        launchClassLoader = classLoader;
    }
}
