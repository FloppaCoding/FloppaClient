package floppaclient.ui.clickgui.elements

import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.ui.clickgui.ClickGUI
import floppaclient.ui.clickgui.util.FontUtil

/**
 * Parent class to the settings elements in the click gui.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
open class Element {
    var clickgui: ClickGUI? = null
    var parent: ModuleButton? = null
    var setting: Setting? = null
    var offset = 0.0
    var x = 0.0
    var y = 0.0
    var width = 0.0
    var height = 0.0
    var displayName: String? = null
    var comboextended = false
    var type: ElementType? = null
    fun setup() {
        clickgui = parent!!.parent.clickgui
    }

    fun update() {
        /** Positioning the Element. Offset is handled in ClickGUI to prevent overlap */
        x = parent!!.x
        y = parent!!.y + offset
        width = parent!!.width
        height = 15.0

        /** Determine the title and expand box if needed */
        val name = setting?.name ?: if (type == ElementType.KEY_BIND) "Key Bind" else return
        displayName = name
//        displayName = name.forceCapitalize()
        when (type) {
            ElementType.CHECK_BOX -> {
//            val textx = x + width - FontUtil.getStringWidth(displayName)
//            if (textx < x + 13) {
//                width += x + 13 - textx + 1
//            }
            }
            ElementType.SELECTOR -> {
                height = if (comboextended)
                    ((setting as SelectorSetting).options.size * (FontUtil.fontHeight + 2) + 15).toDouble()
                else
                    15.0
//            var longest = FontUtil.getStringWidth(displayName)
//            for (option in (setting as SelectorSetting).options) {
//                val temp = FontUtil.getStringWidth(option)
//                if (temp > longest) {
//                    longest = temp
//                }
//            }
//            val textx = x + width - longest
//            if (textx < x) {
//                width += x - textx + 1
//            }
            }
            ElementType.COLOR -> {
                height = if (comboextended)
                    if((setting as ColorSetting).allowAlpha)
                        15.0 * 5
                    else
                        15.0 * 4
                else
                    15.0
            }
            ElementType.SLIDER -> {
//            val displayval = "" + ((set as SliderSetting).value * 100.0).roundToInt() / 100.0
//            val displaymax = "" + ((setting as NumberSetting).max * 100.0).roundToInt() / 100.0
//            val textx = x + width - FontUtil.getStringWidth(displayName) - FontUtil.getStringWidth(displaymax) - 4
//            if (textx < x) {
//                width += x - textx + 1
//            }
            }
            ElementType.TEXT_FIELD -> {
                height = 12.0
//            val textx = x + width - FontUtil.getStringWidth(displayName)
//            if (textx < x) {
//                width += x - textx + 1
//            }
            }
            ElementType.KEY_BIND -> {
                height = 11.0
            }
            else -> {}
        }
    }

    open fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {}
    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        return isHovered(mouseX, mouseY)
    }

    /**
     * Overridden in the elements to enable key detection. Returns true when an action was taken.
     */
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean { return false }

    open fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {}
    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }
}