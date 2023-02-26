package floppaclient.ui.clickgui.elements.menu

import floppaclient.module.settings.impl.ActionSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.FontUtil

/**
 * Provides the Menu Button for action settings.
 *
 * @author Aton
 */
class ElementAction(parent: ModuleButton, setting: ActionSetting) :
    Element<ActionSetting>(parent, setting, ElementType.ACTION)  {

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        FontUtil.drawString(displayName, 1, 2)
        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed.
     * Used to activate the elements action.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isButtonHovered(mouseX, mouseY) ) {
            (setting as? ActionSetting)?.doAction()
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Checks whether this element is hovered
     */
    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute  && mouseY <= yAbsolute + height
    }
}