package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * A simple full bright module that lets players see in the dark.
 * This module works by setting a gamma value higher than available in the game settings.
 *
 * @author Aton
 */
object FullBright : Module(
    "Full Bright",
    category = Category.RENDER,
    description = "Lets you see in the dark."
) {
    private var oldGamma = 1f

    override fun onEnable() {
        oldGamma = mc.gameSettings.gammaSetting
        mc.gameSettings.gammaSetting = 100f
    }

    override fun onDisable() {
        mc.gameSettings.gammaSetting = oldGamma
    }

    /**
     * Make sure that full bright is enabled even if the game settings were reloaded.
     * This should not be required but it seems like the game settings are loaded after the postInit event when the modules
     * are loaded, which should not be the case.
     */
    @SubscribeEvent
    fun onWorldJoin(event: WorldEvent.Load) {
        if (mc.gameSettings.gammaSetting != 100f) onEnable()
    }
}