package floppaclient.ui.hud

import floppaclient.funnymap.utils.RenderUtils
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

/**
 * Provides functionality for game overlay elements
 */
open class HudElement(
    private val xSett: NumberSetting,
    private val ySett: NumberSetting,
    var width: Int = 10,
    var height: Int = 10,
    val scale: NumberSetting = NumberSetting("scale",1.0,0.1,4.0, 0.02, visibility = Visibility.HIDDEN),
) {

    private val zoomIncrement = 0.05

    /**
     * Use these instead of a direct reference to the NumberSetting
     */
    var x: Int
     get() = xSett.value.toInt()
     set(value) {
         xSett.value = value.toDouble()
     }

    var y: Int
        get() = ySett.value.toInt()
        set(value) {
            ySett.value = value.toDouble()
        }

    /**
     * Resets the position of this hud element by setting the value of xSett and ySett to their default.
     *
     * Can be overridden in the implementation.
     */
    open fun resetElement() {
        xSett.value = xSett.default
        ySett.value = ySett.default
        scale.value = scale.default
    }

    /**
     * Handles scroll wheel action for this element.
     * Can be overridden in implementation.
     */
    open fun scroll(amount: Int) {
        this.scale.value = this.scale.value + amount * zoomIncrement
    }

    /**
     * This will initiate the hud render and translate to the correct position and scale.
     */
    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scale.value, scale.value, 1.0)

        renderHud()

        GlStateManager.popMatrix()
    }

    /**
     * to be overwritten in implementations.
     */
    open fun renderHud() { }

    /**
     * Used for moving the hud element.
     * Draws a rectangle in place of the actual element
     */
    fun renderPreview() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scale.value, scale.value, 1.0)

        RenderUtils.renderRect(
            0.0,
            0.0,
            width.toDouble(),
            height.toDouble(),
            Color(-0x44eaeaeb, true)
        )

        GlStateManager.popMatrix()
    }

}