package floppaclient.config

import cc.polyfrost.oneconfig.config.Config
import cc.polyfrost.oneconfig.config.annotations.Slider
import cc.polyfrost.oneconfig.config.data.Mod
import cc.polyfrost.oneconfig.config.data.ModType
import floppaclient.module.impl.render.ItemAnimations

class OneConfigTest : Config(
    Mod("FloppaClient", ModType.SKYBLOCK, "assets/floppaclient/gui/Icon.png"),
    "floppaclient.json",
) {
    init {
        initialize()
    }

    @Slider(
        name = "Size",
        min = -1.5f, max = 1.5f,
    )
    var size = ItemAnimations.size.default


}