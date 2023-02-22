package floppaclient.ui.clickgui.elements.menu

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Provides a slider element.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
class ElementSlider(iparent: ModuleButton, iset: Setting<*>) : Element() {
    var dragging: Boolean

    init {
        parent = iparent
        setting = iset
        dragging = false
        type = ElementType.SLIDER
        super.setup()
    }

    /**
	 * Renders the element
	 */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val displayval = "" + ((setting as NumberSetting).value * 100.0).roundToInt() / 100.0
        val hoveredORdragged = isSliderHovered(mouseX, mouseY) || dragging
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, if (hoveredORdragged) 250 else 200).rgb
        val color2 = Color(temp.red, temp.green, temp.blue, if (hoveredORdragged) 255 else 230).rgb

        val percentBar = ((setting as NumberSetting).value - (setting as NumberSetting).min) / ((setting as NumberSetting).max - (setting as NumberSetting).min)

        /** Render the box */
        Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), ColorUtil.elementColor)

        /** Render the text */
        FontUtil.drawString(displayName, x + 1, y + 2, -0x1)
        FontUtil.drawString(displayval, x + width - FontUtil.getStringWidth(displayval), y + 2, -0x1)

        /** Render the slider */
        Gui.drawRect(x.toInt(), (y + 12).toInt(), (x + width).toInt(), (y + 13.5).toInt(), -0xefeff0)
        Gui.drawRect(x.toInt(), (y + 12).toInt(), (x + percentBar * width).toInt(), (y + 13.5).toInt(), color)
        if (percentBar > 0 && percentBar < 1) Gui.drawRect(
            (x + percentBar * width - 1).toInt(), (y + 12).toInt(), (x + (percentBar * width).coerceAtMost(width)).toInt(), (y + 13.5).toInt(), color2
        )


        /** Calculate and set new value when dragging */
        if (dragging) {
            val diff = (setting as NumberSetting).max - (setting as NumberSetting).min
            val newVal = (setting as NumberSetting).min + MathHelper.clamp_double((mouseX - x) / width, 0.0, 1.0) * diff
            (setting as NumberSetting).value = newVal //Die Value im Setting updaten
        }
    }

    /**
	 * Handles interaction with this element.
     * Returns true if interacted with the element to cancel further interactions.
	 */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isSliderHovered(mouseX, mouseY)) {
            dragging = true
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
	 * Stops slider action on mouse release
	 */
    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging = false
    }

    /**
     * Check for arrow keys to move the slider by one increment.
     */
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        val scaledresolution = ScaledResolution(mc)
        val i1: Int = scaledresolution.scaledWidth
        val j1: Int = scaledresolution.scaledHeight
        val k1: Int = Mouse.getX() * i1 / mc.displayWidth
        val l1: Int = j1 - Mouse.getY() * j1 / mc.displayHeight - 1

        val scale = 2.0 / mc.gameSettings.guiScale
        val scaledMouseX = (k1 / scale).toInt()
        val scaledMouseY = (l1 / scale).toInt()

        if (isSliderHovered(scaledMouseX, scaledMouseY)){
            if (keyCode == Keyboard.KEY_RIGHT){
                (setting as NumberSetting).value += (setting as NumberSetting).increment
                return true
            }
            if (keyCode == Keyboard.KEY_LEFT){
                (setting as NumberSetting).value -= (setting as NumberSetting).increment
                return true
            }
        }
        return super.keyTyped(typedChar, keyCode)
    }

    /**
	 * Checks whether the mouse is hovering the slider
	 */
    private fun isSliderHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y  && mouseY <= y + height
    }
}