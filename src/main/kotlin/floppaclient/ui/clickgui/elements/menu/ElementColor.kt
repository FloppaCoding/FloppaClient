package floppaclient.ui.clickgui.elements.menu

import floppaclient.FloppaClient
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.roundToInt

/**
 * Provides a color selector element.
 *
 * @author Aton
 */
class ElementColor(parent: ModuleButton, setting: ColorSetting) :
    Element<ColorSetting>(parent, setting, ElementType.COLOR) {
    var dragging: Int? = null

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        val colorValue = setting.value.rgb

        FontUtil.drawString(displayName, 1, 2)

        /** Render the color preview */
        Gui.drawRect(width - 26, 2, width - 1, 11, colorValue)

        /** Render the tab indicating the drop-down */
        Gui.drawRect(0,  13, width, 15, ColorUtil.tabColorBg)
        Gui.drawRect((width * 0.4).toInt(), 12, (width * 0.6).toInt(), 15, ColorUtil.tabColor)

        /** Render the extended */
        if (extended) {
            Gui.drawRect(0, DEFAULT_HEIGHT,  width, height, ColorUtil.dropDownColor)
            var currentDrawY = DEFAULT_HEIGHT
            val increment = DEFAULT_HEIGHT

            /** Render the color sliders */
            for (currentColor in setting.colors()) {
                val isColorDragged = dragging == currentColor.ordinal
                /** For hue render the hue bar. */
                if (currentColor == ColorSetting.ColorComponent.HUE) {
                    GL11.glPushMatrix()
                    GlStateManager.color(255f, 255f, 255f, 255f)
                    FloppaClient.mc.textureManager.bindTexture(HUE_SCALE)
                    Gui.drawModalRectWithCustomSizedTexture(0, currentDrawY,
                        0f, 0f, width, 11, width.toFloat(), 11.toFloat()
                    )
                    GL11.glPopMatrix()
                }

                val dispVal = "" + (setting.getNumber(currentColor) * 100.0).roundToInt() / 100.0
                FontUtil.drawString(currentColor.getName(), 1, currentDrawY + 2)
                FontUtil.drawString(dispVal, width - FontUtil.getStringWidth(dispVal), currentDrawY + 2)

                val maxVal = currentColor.maxValue()
                val percentage = setting.getNumber(currentColor)  / maxVal
                Gui.drawRect(0, currentDrawY + 12, width, currentDrawY + 13, -0xefeff0)
                Gui.drawRect(0, currentDrawY + 12, (percentage * width).toInt(), currentDrawY + 13, ColorUtil.sliderColor(isColorDragged))
                if (percentage > 0 && percentage < 1) Gui.drawRect(
                    (percentage * width - 1).toInt(),
                    (currentDrawY + 12), (percentage * width).toInt().coerceAtMost(width), currentDrawY + 13, ColorUtil.sliderKnobColor(isColorDragged)
                )

                /** Calculate and set new value when dragging */
                if (isColorDragged) {
                    val newVal = MathHelper.clamp_double(((mouseX - xAbsolute) / width.toDouble()), 0.0, 1.0) * maxVal
                    setting.setNumber(currentColor, newVal)
                }

                currentDrawY += increment
            }

        }


        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    /**
     * Handles interaction with this element.
     * Returns true if interacted with the element to cancel further interactions.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (isButtonHovered(mouseX, mouseY)) {
                // for now also extend on left click
                extended = !extended
                return true
            }

            if (!extended) return false
            var ay = DEFAULT_HEIGHT
            val increment = DEFAULT_HEIGHT
            for (currentColor in setting.colors()) {
                if (mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute + ay && mouseY <= yAbsolute + ay + increment) {
                    dragging = currentColor.ordinal
                    return true
                }
                ay += increment
            }
        } else if( mouseButton == 1) {
            if (isButtonHovered(mouseX, mouseY)) {
                extended = !extended
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Stops slider action on mouse release
     */
    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging = null
    }

    /**
     * Check for arrow keys to move the slider by one increment.
     */
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!extended) return false
        val scaledMouseX = clickgui.getScaledMouseX()
        val scaledMouseY = clickgui.getScaledMouseY()

        var ay = DEFAULT_HEIGHT
        val increment = DEFAULT_HEIGHT
        for (currentColor in setting.colors()) {
            if (scaledMouseX >= xAbsolute && scaledMouseX <= xAbsolute + width && scaledMouseY >= yAbsolute + ay && scaledMouseY <= yAbsolute + ay + increment) {
                if (keyCode == Keyboard.KEY_RIGHT){
                    setting.setNumber(currentColor, setting.getNumber(currentColor)+currentColor.maxValue()/255.0)
                }
                if (keyCode == Keyboard.KEY_LEFT){
                    setting.setNumber(currentColor, setting.getNumber(currentColor)-currentColor.maxValue()/255.0)
                }
                return true
            }
            ay += increment
        }
        return super.keyTyped(typedChar, keyCode)
    }


    /**
     * Checks whether the mouse is hovering the selector
     */
    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + 15
    }

    companion object {
        private val HUE_SCALE = ResourceLocation(FloppaClient.RESOURCE_DOMAIN, "gui/HueScale.png")
    }
}