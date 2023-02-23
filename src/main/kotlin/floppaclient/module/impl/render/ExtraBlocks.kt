package floppaclient.module.impl.render

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.StringSelectorSetting

/**
 * Map extras block settings module.
 *
 * The actual implementation is in floppaclient.floppamap
 */
object ExtraBlocks : Module(
    "Extras",
    category = Category.RENDER,
    description = "Create coordinate blocks. Use edit mode with /em (block)."
){

    val defaultStairs = StringSelectorSetting("Default Stairs","jungle", arrayListOf(
        "birch",
        "acacia",
        "stone_brick",
        "brick",
        "sandstone",
        "dark_oak",
        "nether_brick",
        "jungle",
        "oak",
        "quartz",
        "red_sandstone",
        "stone",
        "spruce"),
        description = "Stairs type that will be selected with /em stairs or /em stair."
    )
    val defaultFence = StringSelectorSetting("Default Fence","birch", arrayListOf(
        "oak",
        "dark_oak",
        "acacia",
        "birch",
        "jungle",
        "nether_brick",
        "spruce"),
        description = "Fence type that will be selected with /em fence."
    )
    val defaultSlab = StringSelectorSetting("Default Slab","stone", arrayListOf(
        "stone",
        "oak"),
        description = "Slab type that will be selected with /em slab."
    )

    init {
        this.addSettings(
            defaultStairs,
            defaultFence,
            defaultSlab
        )
    }

}