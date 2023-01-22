package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module

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
}