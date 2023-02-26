package floppaclient.ui.clickgui.advanced.elements.menu

import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.advanced.elements.AdvancedElement
import floppaclient.ui.clickgui.advanced.elements.AdvancedElementType
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.ColorUtil.textcolor
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import java.awt.Color

/**
 * Provides a checkbox element for the advanced gui.
 *
 * @author Aton
 */
class AdvancedElementCheckBox(
    parent: AdvancedMenu, module: Module, setting: BooleanSetting,
) : AdvancedElement<BooleanSetting>(parent, module, setting, AdvancedElementType.CHECK_BOX) {


    /**
     * Render the element
     */
    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float) : Int{
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, 200).rgb

        /** Rendering the name and the checkbox */
        FontUtil.drawString(setting.name, 1, 2, textcolor)
        Gui.drawRect(
            (settingWidth - 13), 2, settingWidth - 1, 13,
            if (setting.enabled) color else -0x1000000
        )
        if (isCheckHovered(mouseX, mouseY)) Gui.drawRect(
            settingWidth - 13,  2, settingWidth -1,
            13, 0x55111111
        )
        return this.settingHeight
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
        return mouseX >= parent.x + x + settingWidth - 13 && mouseX <= parent.x + x + settingWidth - 1 && mouseY >= parent.y + y + 2 && mouseY <= parent.y + y + settingHeight - 2
    }
}