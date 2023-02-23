package floppaclient.ui.clickgui.elements.menu

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.impl.render.ClickGui
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.StringSelectorSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import java.awt.Color
import java.util.*

/**
 * Provides a selector element.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
@Deprecated("Use enum version instead")
class ElementStringSelector(iparent: ModuleButton, iset: Setting<*>) : Element() {

    init {
        parent = iparent
        setting = iset
        type = ElementType.SELECTOR
        super.setup()
    }

    /**
	 * Renders the element
	 */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, 150).rgb
        val displayValue = (setting as StringSelectorSetting).selected

        /** Render the box and text */
        if(parent?.parent?.shouldRender(y + 15) == true) {
            Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), ColorUtil.elementColor)
            if (FontUtil.getStringWidth(displayValue + "00" + displayName) <= width) {
                FontUtil.drawString(displayName, x + 1, y + 2, -0x1)
                FontUtil.drawString(displayValue, x + width - FontUtil.getStringWidth(displayValue), y + 2, -0x1)
            } else {
                if (isButtonHovered(mouseX, mouseY)) {
                    FontUtil.drawCenteredStringWithShadow(displayValue, x + width / 2, y + 2, -0x1)
                } else {
                    FontUtil.drawCenteredString(displayName, x + width / 2, y + 2, -0x1)
                }
            }

            Gui.drawRect(x.toInt(), (y + 13).toInt(), (x + width).toInt(), (y + 15).toInt(), 0x77000000)
            Gui.drawRect(
                (x + width * 0.4).toInt(),
                (y + 12).toInt(),
                (x + width * 0.6).toInt(),
                (y + 15).toInt(),
                color
            )
        }

        if (comboextended) {
            val clr2 = temp.rgb
            var ay = y + 15
            val increment = FontUtil.fontHeight + 2
            for (option in (setting as StringSelectorSetting).options) {
                if(parent?.parent?.shouldRender(ay + increment) == true) {
                    Gui.drawRect(x.toInt(), (ay).toInt(), (x + width).toInt(), (ay + increment).toInt(), -0x55ededee)
                    val elementtitle =
                        option.substring(0, 1).uppercase(Locale.getDefault()) + option.substring(1, option.length)
                    FontUtil.drawCenteredString(elementtitle, x + width / 2, ay + 2, -0x1)

                    /** Highlights the element if it is selected */
                    if (option.equals((setting as StringSelectorSetting).selected, ignoreCase = true)) {
                        Gui.drawRect(
                            x.toInt(),
                            ay.toInt(),
                            (x + 1.5).toInt(),
                            (ay + increment).toInt(),
                            color
                        )
                    }
                    /** Highlights the element when it is hovered */
                    if (mouseX >= x && mouseX <= x + width && mouseY >= ay && mouseY < ay + increment) {
                        Gui.drawRect(
                            (x + width - 1.2).toInt(),
                            ay.toInt(),
                            (x + width).toInt(),
                            (ay + increment).toInt(),
                            clr2
                        )
                    }
                }
                ay += increment.toDouble()
            }
        }
    }

    /**
     * Handles interaction with this element.
     * Returns true if interacted with the element to cancel further interactions.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (isButtonHovered(mouseX, mouseY)) {
                (setting as StringSelectorSetting).index += 1
                return true
            }

            if (!comboextended) return false
            var ay = y + 15
            val increment = FontUtil.fontHeight + 2
            for (option in (setting as StringSelectorSetting).options) {
                if(parent?.parent?.shouldRender(ay + increment) == true) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= ay && mouseY <= ay + increment) {
                        if (ClickGui.sound.enabled) mc.thePlayer.playSound("tile.piston.in", 20.0f, 20.0f)
                        if (clickgui != null) (setting as StringSelectorSetting).selected =
                            option.lowercase(
                                Locale.getDefault()
                            )
                        return true
                    }
                }
                ay += increment.toDouble()
            }
        } else if( mouseButton == 1) {
            if (isButtonHovered(mouseX, mouseY)) {
                comboextended = !comboextended
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Checks whether the mouse is hovering the selector
     */
    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 15) && parent?.parent?.shouldRender(y+15) ?: false
    }
}