package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module

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