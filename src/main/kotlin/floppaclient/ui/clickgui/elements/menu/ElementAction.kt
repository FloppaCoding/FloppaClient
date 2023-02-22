package floppaclient.ui.clickgui.elements.menu

import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.ActionSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui

/**
 * Provides the Menu Button for action settings.
 *
 * @author Aton
 */
class ElementAction(iparent: ModuleButton, iset: Setting<*>) : Element()  {


    init {
        parent = iparent
        setting = iset
        type = ElementType.ACTION
        displayName = (setting as? ActionSetting)?.name
        super.setup()
    }

    /**
     * Render the element
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val color = ColorUtil.elementColor

        /** Rendering the box */
        Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), color)

        /** Titel und Checkbox rendern. */
        FontUtil.drawString(displayName, x + 1, y + 2, -0x1)
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
        return mouseX >= x && mouseX <= x + width && mouseY >= y  && mouseY <= y + height
    }
}