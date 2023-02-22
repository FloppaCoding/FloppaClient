package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withInputTransform
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils.identicalToOneOf
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks

/**
 * A module with the aim of allowing the player to see obstructed blocks.
 *
 * @author Aton
 * @see floppaclient.mixins.BlockMixin
 * @see floppaclient.mixins.render.BlockFluidRendererMixin
 * @see floppaclient.mixins.render.RenderChunkMixin
 * @see floppaclient.mixins.render.BlockRendererDispatcherMixin
 */
object XRay : Module(
    "XRay",
    category = Category.RENDER,
    description = "See certain Blocks through walls."
){
    /*
     * TODO in the future replace these boolean settings with a new setting type that works as a multi select dropdown.
     *  This could be derived from the SelectorSetting but with mutiple selections being possible.
     *  Also a gui for block selection would be nice. That could also be used for EditMode.
     */
    private val showOres = BooleanSetting("Ores", true, description = "Makes the module show ores")
    private val showGlass = BooleanSetting("Glass", true, description = "Makes the module show glass blocks and panes")
    private val showMithril = BooleanSetting("Mithril", true, description = "Makes the module show aquamarine and prismarine blocks.")
    private val showFluids = BooleanSetting("Fluids", false, description = "If enabled the opacity of fluids will not be affected.")

    private val opacity = NumberSetting("Opacity", 180.0, 0.0, 255.0, 1.0, description = "The opacity of hidden blocks.")
        .withInputTransform { input ->
            // store the value in an int to avoid needing a type conversion for every rendered block surface.
            // This might help with performance, or not idk.
            alphaInt = input.toInt()
            alphaFloat = input.toFloat() / 255f
            if(this@XRay.enabled) mc.renderGlobal.loadRenderers()
            input
        }

    private var alphaInt: Int = 180
    var alphaFloat: Float = 0.7f
        private set

    init {
        this.addSettings(
            showOres,
            showGlass,
            showMithril,
            showFluids,
            opacity,
        )
        listOf(
            showOres,
            showGlass,
            showMithril,
            showFluids
        ).forEach { setting ->
            setting.processInput = { input ->
                if(this@XRay.enabled) mc.renderGlobal.loadRenderers()
                input
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        mc.renderGlobal.loadRenderers()
    }

    override fun onDisable() {
        super.onDisable()
        mc.renderGlobal.loadRenderers()
    }

    /**
     * @see floppaclient.replacements.render.BlockRenderer.renderModel
     */
    fun getBlockAlpha(state: IBlockState): Int {
        if (!shouldBlockBeRevealed(state.block)) {
            return alphaInt
        }
        return -1
    }

    /**
     * Reveal obstructed blocks for the XRay.
     * @see floppaclient.mixins.BlockMixin.onShouldSideBeRendered
     */
    fun modifyRenderdSideHook(block: Block, returnValue: Boolean) : Boolean{
        if(!returnValue && shouldBlockBeRevealed(block)) {
            return true
        }
        return returnValue
    }

    /**
     * @see floppaclient.mixins.render.BlockFluidRendererMixin
     */
    fun shouldTweakFluids(): Boolean {
        return !showFluids.enabled
    }

    /**
     * Checks the modules settings whether the specified block should be visible.
     */
    private fun shouldBlockBeRevealed(block: Block): Boolean {
        return when {
            block.identicalToOneOf(*ores) -> showOres.enabled
            block.identicalToOneOf(*mithril) -> showMithril.enabled
            block.identicalToOneOf(*glasses) -> showGlass.enabled
            else -> false
        }
    }

    private val ores = arrayOf(
        Blocks.coal_ore,
        Blocks.iron_ore,
        Blocks.gold_ore,
        Blocks.diamond_ore,
        Blocks.emerald_ore,
    )

    private val glasses = arrayOf(
        Blocks.stained_glass,
        Blocks.stained_glass_pane,
    )

    private val mithril = arrayOf(
        Blocks.prismarine,
        )
}