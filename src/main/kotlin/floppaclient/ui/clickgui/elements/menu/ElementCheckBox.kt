package floppaclient.ui.clickgui.elements.menu

import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui

/**
 * Provides a checkbox element.
 *
 * @author  Aton
 */
class ElementCheckBox(parent: ModuleButton, setting: BooleanSetting) :
    Element<BooleanSetting>(parent, setting, ElementType.CHECK_BOX) {

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        val buttonColor = if (setting.enabled)
            ColorUtil.clickGUIColor.rgb
        else ColorUtil.buttonColor

        /** Rendering the name and the checkbox */
        FontUtil.drawString(displayName, 1, 3)
        Gui.drawRect(width -13, 2, width - 1, 13, buttonColor)
        if (isCheckHovered(mouseX, mouseY))
            Gui.drawRect(width -13, 2, width - 1, 13, ColorUtil.boxHoverColor)

        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed
	 */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isCheckHovered(mouseX, mouseY)) {
            setting.toggle()
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
	 * Checks whether this element is hovered
	 */
    private fun isCheckHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute + width - 13 && mouseX <= xAbsolute + width - 1 && mouseY >= yAbsolute + 2 && mouseY <= yAbsolute + height - 2
    }
}