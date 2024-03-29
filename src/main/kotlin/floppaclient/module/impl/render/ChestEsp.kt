package floppaclient.module.impl.render

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSelectorSetting
import java.awt.Color

/**
 * Render chests trough walls.
 *
 * @author Aton
 */
object ChestEsp : Module(
    "Chest ESP",
    category = Category.RENDER,
    description = "Highlights chests through walls."
){
    private val mode = StringSelectorSetting("Mode", "Box", arrayListOf("Box", "Phase"), description = "Determines how the chest wil be visualized.")
    private val color = ColorSetting("Box Color", Color(255,0,0), description = "Color of the box when box mode is selected.")
    private val thickness = NumberSetting("Thickness", 1.0, 0.0,5.0, 0.1, description = "Line width of the box when box mode is selected.")

    /**
     * Referenced by the ChestRendererMixin to determine whether the chest is drawn in the world or something in a hud.
     * Set by
     */
    var isDrawingWorld = false

    init {
        this.addSettings(
            mode,
            color,
            thickness
        )
    }

    /**
     * Accessed by the ChestRenderer Mixin.
     */
    fun isPhaseMode(): Boolean {
        return this.enabled && mode.isSelected("Phase")
    }

    /**
     * Accessed by the CestRenderer Mixin.
     */
    fun isBoxMode(): Boolean {
        return this.enabled && mode.isSelected("Box")
    }

    /**
     * Accessed by the CestRenderer Mixin.
     */
    fun getBoxColor(): Color {
        return color.value
    }

    /**
     * Accessed by the CestRenderer Mixin.
     */
    fun getBoxThickness(): Float {
        return thickness.value.toFloat()
    }
}