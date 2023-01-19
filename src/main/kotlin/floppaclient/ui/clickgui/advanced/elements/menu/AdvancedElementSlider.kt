package floppaclient.ui.clickgui.advanced.elements.menu

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.advanced.elements.AdvancedElement
import floppaclient.ui.clickgui.advanced.elements.AdvancedElementType
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
 * Provides a slider element for the advanced gui.
 *
 * @author Aton
 */
class AdvancedElementSlider(
    parent: AdvancedMenu, module: Module, setting: NumberSetting,
) : AdvancedElement(parent, module, setting, AdvancedElementType.SLIDER) {
    private var dragging: Boolean = false

    /**
	 * Renders the element
	 */
    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float) : Int{
        val displayval = "" + ((setting as NumberSetting).value * 100.0).roundToInt() / 100.0
        val hoveredORdragged = isSliderHovered(mouseX, mouseY) || dragging
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, if (hoveredORdragged) 250 else 200).rgb
        val color2 = Color(temp.red, temp.green, temp.blue, if (hoveredORdragged) 255 else 230).rgb

        val percentBar = (setting.value - setting.min) / (setting.max - setting.min)

        /** Render the text */
        FontUtil.drawString(setting.name, 1, 2, -0x1)
        FontUtil.drawString(displayval, settingWidth - FontUtil.getStringWidth(displayval), 2, -0x1)

        /** Render the slider */
        Gui.drawRect(0, 12, settingWidth, 14, -0xefeff0)
        Gui.drawRect(0, 12, (percentBar * settingWidth).toInt(), 14, color)
        if (percentBar > 0 && percentBar < 1) Gui.drawRect(
            (percentBar * settingWidth - 2).toInt(), 12,
            (percentBar * settingWidth).toInt().coerceAtMost(settingWidth), 14, color2
        )


        /** Calculate and set new value when dragging */
        if (dragging) {
            val diff = setting.max - setting.min
            val newVal = setting.min + MathHelper.clamp_double((mouseX - parent.x - x) / settingWidth.toDouble(), 0.0, 1.0) * diff
            setting.value = newVal //Die Value im Setting updaten
        }

       return this.settingHeight
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
        return mouseX >= parent.x + x && mouseX <= parent.x + x + settingWidth && mouseY >= parent.y + y  && mouseY <= parent.y + y + settingHeight
    }
}