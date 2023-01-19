package floppaclient.ui.clickgui.advanced.elements

import floppaclient.module.Module
import floppaclient.module.settings.Setting
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

/**
 * Class for all setting elements in the advanced menu to inherit from
 *
 * @author Aton
 */
open class AdvancedElement(
    val parent: AdvancedMenu,
    val module: Module,
    val setting: Setting,
    val type: AdvancedElementType,
) {
    var x = 0
    var y = 0
    var width = 150
    var settingWidth = 116
    var settingHeight = 15
    var height = 15

    var comboextended = false

    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)

        //Rendering the box behind the element.
        Gui.drawRect(0, 0, width, height, Color(ColorUtil.bgColor, true).brighter().rgb)
        Gui.drawRect(0, 0, settingWidth, settingHeight, Color(ColorUtil.elementColor, true).darker().rgb)

        // Render the element.
        val l1 = renderElement(mouseX, mouseY, partialTicks)

        // Render the descriton right of the Setting
        val l2 = rednerDescription()
        this.settingHeight = l1
        this.height = l1.coerceAtLeast(l2)

        GlStateManager.popMatrix()
    }

    open fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float) : Int{ return settingHeight }

    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        return false
    }

    open fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {}

    /**
     * Overridden in the elements to enable key detection. Returns true when an action was taken.
     */
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean { return false }

    fun rednerDescription() : Int{
        var descriptionHeight = 0
        setting.description?.let {
            FontUtil.drawSplitString(
                it, settingWidth + 10,
                2, width - settingWidth - 10, ColorUtil.textcolor
            )
            descriptionHeight = FontUtil.getSplitHeight(it, width - settingWidth - 10)
        }
        return descriptionHeight + 4
    }

    fun setDimensions(x: Int, y: Int, width: Int, height: Int){
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    fun setPosition(x: Int, y: Int){
        this.x = x
        this.y = y
    }
}