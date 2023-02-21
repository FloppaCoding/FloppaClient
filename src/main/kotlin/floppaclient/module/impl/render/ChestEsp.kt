package floppaclient.module.impl.render

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
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
    private val mode = SelectorSetting("Mode", "Box Outline", arrayListOf("Box Outline", "Box Filled", "Phase"), description = "Determines how the chest wil be visualized.")
    private val color = ColorSetting("Box Color", Color(255,0,0), true, description = "Color of the box when box mode is selected.")
        .withDependency { this.mode.index == 0 || this.mode.index == 1 }
    private val thickness = NumberSetting("Thickness", 1.0, 0.0,5.0, 0.1, description = "Line width of the box when box outline mode is selected.")
        .withDependency { this.mode.index == 0 }
    private val opacity = NumberSetting("Opacity", 0.3, 0.05,1.0, 0.05, description = "Opacity of the box when box filled mode is selected")
        .withDependency { this.mode.index == 1 }
    /**
     * Referenced by the ChestRendererMixin to determine whether the chest is drawn in the world or something in a hud.
     * Set by
     */
    var isDrawingWorld = false

    init {
        this.addSettings(
            mode,
            color,
            thickness,
            opacity
        )
    }

    /**
     * Accessed by the ChestRenderer Mixin.
     */
    fun isPhaseMode(): Boolean {
        return this.enabled && mode.isSelected("Phase")
    }

    /**
     * Accessed by the ChestRenderer Mixin.
     */
    fun isBoxMode(): Boolean {
        return this.enabled && mode.isSelected("Box Outline")
    }
    /**
     * Accessed by the ChestRenderer Mixin.
     */
    fun isFillMode(): Boolean {
        return this.enabled && mode.isSelected("Box Filled")
    }

    /**
     * Accessed by the ChestRenderer Mixin.
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
    /**
     * Accessed by the CestRenderer Mixin.
     */
    fun getBoxOpacity(): Float {
        return opacity.value.toFloat()
    }
}