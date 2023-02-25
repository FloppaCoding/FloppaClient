package floppaclient.ui.hud

import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.render.HUDRenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

/**
 * Provides functionality for game overlay elements.
 * @author Aton
 */
abstract class HudElement{

    private val xSett: NumberSetting
    private val ySett: NumberSetting
    val scale: NumberSetting

    var width: Int
    var height: Int

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
     * Sets up a hud Element.
     * This constructor takes care of creating the [NumberSetting]s required to save the position and scale of the hud
     * element to the config.
     */
    constructor(module: Module, xDefault: Int = 0, yDefault: Int = 0, width: Int = 10, height: Int = 10, defaultScale: Double = 1.0) {
        val id = module.settings.count { it.name.startsWith("xHud") }
        val xHud = NumberSetting("xHud_$id", default = xDefault.toDouble(), visibility = Visibility.HIDDEN)
        val yHud = NumberSetting("yHud_$id", default = yDefault.toDouble(), visibility = Visibility.HIDDEN)
        val scaleHud = NumberSetting("scaleHud_$id",defaultScale,0.1,4.0, 0.01, visibility = Visibility.HIDDEN)

        module.addSettings(xHud, yHud, scaleHud)

        this.xSett = xHud
        this.ySett = yHud
        this.scale = scaleHud

        this.width = width
        this.height = height
    }

    /**
     * It is advised to use the other constructor unless this one is required.
     */
    constructor(xHud: NumberSetting, yHud: NumberSetting, width: Int = 10, height: Int = 10, scale: NumberSetting) {
        this.xSett = xHud
        this.ySett = yHud
        this.scale = scale

        this.width = width
        this.height = height
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
     * Override this method in your implementations.
     *
     * This method is responsible for rendering the HUD element.
     * Within this method coordinates are already transformed regarding to the HUD position [x],[x] and [scale].
     */
    abstract fun renderHud()

    /**
     * Used for moving the hud element.
     * Draws a rectangle in place of the actual element
     */
    fun renderPreview() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scale.value, scale.value, 1.0)

        HUDRenderUtils.renderRect(
            0.0,
            0.0,
            width.toDouble(),
            height.toDouble(),
            Color(-0x44eaeaeb, true)
        )

        GlStateManager.popMatrix()
    }
}