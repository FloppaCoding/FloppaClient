package floppaclient.ui.clickgui.elements.menu

import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import kotlin.math.roundToInt

/**
 * Provides a slider element.
 *
 * @author Aton
 */
class ElementSlider(parent: ModuleButton, setting: NumberSetting) :
    Element<NumberSetting>(parent, setting, ElementType.SLIDER) {
    var dragging: Boolean = false

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        val displayval = "" + (setting.value * 100.0).roundToInt() / 100.0
        val hoveredORdragged = isSliderHovered(mouseX, mouseY) || dragging
        val percentBar = (setting.value - setting.min) / (setting.max - setting.min)

        /** Render the text */
        FontUtil.drawString(displayName, 1, 2, )
        FontUtil.drawString(displayval, width - FontUtil.getStringWidth(displayval), 2)

        /** Render the slider */
        Gui.drawRect(0, 12, width, 13, ColorUtil.sliderBackground)
        Gui.drawRect(0, 12, (percentBar * width).toInt(), 13, ColorUtil.sliderColor(hoveredORdragged))
        if (percentBar > 0 && percentBar < 1) Gui.drawRect(
            (percentBar * width - 1).toInt(), 12, ((percentBar * width).toInt().coerceAtMost(width)), 13,
            ColorUtil.sliderKnobColor(hoveredORdragged)
        )

        /** Calculate and set new value when dragging */
        if (dragging) {
            val diff = setting.max - setting.min
            val newVal = setting.min + MathHelper.clamp_double(((mouseX - xAbsolute) / width.toDouble()), 0.0, 1.0) * diff
            setting.value = newVal
        }

        return super.renderElement(mouseX, mouseY, partialTicks)
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
        val scaledMouseX = clickgui.getScaledMouseX()
        val scaledMouseY = clickgui.getScaledMouseY()

        if (isSliderHovered(scaledMouseX, scaledMouseY)){
            if (keyCode == Keyboard.KEY_RIGHT){
                setting.value += setting.increment
                return true
            }
            if (keyCode == Keyboard.KEY_LEFT){
                setting.value -= setting.increment
                return true
            }
        }
        return super.keyTyped(typedChar, keyCode)
    }

    /**
	 * Checks whether the mouse is hovering the slider
	 */
    private fun isSliderHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute  && mouseY <= yAbsolute + height
    }
}