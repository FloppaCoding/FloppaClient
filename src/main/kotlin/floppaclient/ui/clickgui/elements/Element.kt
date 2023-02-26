package floppaclient.ui.clickgui.elements

import floppaclient.module.impl.render.ClickGui
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.module.settings.impl.StringSelectorSetting
import floppaclient.ui.clickgui.ClickGUI
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager

/**
 * Parent class to the settings elements in the click gui.
 *
 * @author Aton
 */
abstract class Element<S: Setting<*>>(
    val parent: ModuleButton,
//    val module: Module,
    val setting: S,
    val type: ElementType
) {
    val clickgui: ClickGUI = parent.panel.clickgui
    /** Relative position of this element in respect to [parent]. */
    var x = 2
    /** Relative position of this element in respect to [parent]. */
    var y = 0
    val width = parent.width - 2 - x
    /** Height of the complete element included optional dropdown. */
    var height: Int
    var displayName: String = setting.name
    var extended = false
    var listening = false

    /** Absolute position of the panel on the screen. */
    val xAbsolute: Int
        get() = x + parent.x + parent.panel.x
    /** Absolute position of the panel on the screen. */
    val yAbsolute: Int
        get() = y + parent.y + parent.panel.y

    init {
        height = when (type) {
            ElementType.TEXT_FIELD -> 12
            ElementType.KEY_BIND -> 11
            ElementType.ACTION -> 11
            else -> DEFAULT_HEIGHT
        }
    }

    /**
     * Updates the height of the Element based on [extended].
     */
    fun update() {
        displayName = setting.name
        when (type) {
            ElementType.SELECTOR -> {
                height = if (extended)
                    (((setting as? StringSelectorSetting)?.options?.size ?: (setting as SelectorSetting<*>).options.size) * (FontUtil.fontHeight + 2) + DEFAULT_HEIGHT)
                else
                    DEFAULT_HEIGHT
            }
            ElementType.COLOR -> {
                height = if (extended)
                    if((setting as ColorSetting).allowAlpha)
                        DEFAULT_HEIGHT * 5
                    else
                        DEFAULT_HEIGHT * 4
                else
                    DEFAULT_HEIGHT
            }
            else -> {}
        }
    }

    /**
     * Sets up the rendering of the element and dispatches rendering of the individual implementations.
     * @return the height of the element.
     * @see renderElement
     */
    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) : Int {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)

        val color = if (listening) {
            ColorUtil.clickGUIColor.rgb
        }else {
            ColorUtil.elementColor
        }

        /** Rendering the box */
        Gui.drawRect(0, 0, width, height, color)
        /** The decor */
        if (ClickGui.design.isSelected("New")) {
            Gui.drawRect(width, 0, width + 2, height, ColorUtil.outlineColor)
        }

        // Render the element.
        val elementLength = renderElement(mouseX, mouseY, partialTicks)

        GlStateManager.popMatrix()
        return elementLength
    }

    /**
     * To be overridden in the implementations.
     * @return the height of the element.
     */
    protected open fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float) : Int{ return height }

    /**
     * Handles mouse clicks on the Element.
     * To be overridden in the implementations.
     * @return whether an action was performed.
     */
    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        return isHovered(mouseX, mouseY)
    }

    open fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {}

    /**
     * Overridden in the elements to enable key detection.
     * @return true when an action was taken.
     */
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean { return false }

    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + height
    }

    companion object {
        const val DEFAULT_HEIGHT = 15
    }
}