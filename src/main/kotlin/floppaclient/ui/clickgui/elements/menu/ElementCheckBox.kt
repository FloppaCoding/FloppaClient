package floppaclient.ui.clickgui.elements.menu

import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.ui.clickgui.elements.ElementType
import net.minecraft.client.gui.Gui
import java.awt.Color

/**
 * Provides a checkbox element.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
class ElementCheckBox(iparent: ModuleButton, iset: Setting<*>) : Element() {

    init {
        parent = iparent
        setting = iset
        type = ElementType.CHECK_BOX
        super.setup()
    }

    /**
	 * Render the element
	 */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, 200).rgb

        /** Rendering the box */
        Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), ColorUtil.elementColor)

        /** Rendering the name and the checkbox */
        FontUtil.drawString(displayName, x + 1,
            y + FontUtil.fontHeight / 2 - 0.5, -0x1
        )
        Gui.drawRect((x + width -13).toInt(), (y + 2).toInt(), (x + width - 1).toInt(), (y + 13).toInt(),
            if ((setting as BooleanSetting).enabled) color else -0x1000000
        )
        if (isCheckHovered(mouseX, mouseY)) Gui.drawRect((x + width -13).toInt(), (y + 2).toInt(), (x + width - 1).toInt(),
            (y + 13).toInt(), 0x55111111
        )
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed
	 */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isCheckHovered(mouseX, mouseY)) {
            (setting as BooleanSetting).toggle()
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
	 * Checks whether this element is hovered
	 */
    private fun isCheckHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x + width - 13 && mouseX <= x + width - 1 && mouseY >= y + 2 && mouseY <= y + height - 2
    }
}