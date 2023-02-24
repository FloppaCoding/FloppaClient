package floppaclient.ui.clickgui.elements

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Module
import floppaclient.module.impl.keybinds.KeyBind
import floppaclient.module.impl.render.ClickGui
import floppaclient.module.settings.impl.*
import floppaclient.ui.clickgui.Panel
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.elements.menu.*
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager

/**
 * Provides the toggle button for modules in the click gui.
 *
 * @author Aton
 */
class ModuleButton(val module: Module, val panel: Panel) {
    val menuElements: ArrayList<Element<*>> = ArrayList()
    /** Relative position of this button in respect to [panel]. */
    var x = 0
    /** Relative position of this button in respect to [panel]. */
    var y = 0
    val width = panel.width
    val height = (mc.fontRendererObj.FONT_HEIGHT + 2)
    var extended = false
    /** Absolute position of the panel on the screen. */
    val xAbsolute: Int
        get() = x + panel.x
    /** Absolute position of the panel on the screen. */
    val yAbsolute: Int
        get() = y + panel.y

    init {
        /** Register the corresponding gui element for all non-hidden settings in the module */
        updateElements()
        menuElements.add(ElementKeyBind(this, module))
    }

    /**
     * Updates the [menuElements].
     *
     * This is used to initially populate the elements and to update the list based on the visibility condition of the settings.
     * @see floppaclient.module.settings.Setting.shouldBeVisible
     */
    fun updateElements() {
        var position = -1 // This looks weird, but it starts at -1 because it gets incremented before being used.
        for (setting in module.settings) {
            /** Don't show hidden settings */
            if (setting.visibility.visibleInClickGui && setting.shouldBeVisible) run addElement@{
                position++
                if (menuElements.any { it.setting === setting }) return@addElement
                val newElement = when (setting) {
                    is BooleanSetting ->    ElementCheckBox(this, setting)
                    is NumberSetting ->     ElementSlider(this, setting)
                    is StringSelectorSetting ->   ElementStringSelector(this, setting)
                    is SelectorSetting ->   ElementSelector(this, setting)
                    is StringSetting ->     ElementTextField(this, setting)
                    is ColorSetting ->      ElementColor(this, setting)
                    is ActionSetting ->     ElementAction(this, setting)
                    else -> return@addElement
                }
                menuElements.add(position, newElement)
            }else {
                menuElements.removeIf {
                    it.setting === setting
                }
            }
        }
    }

    /**
	 * Render the Button.
     * Dispatches rendering of its [menuElements].
	 */
    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) : Int {

        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)

        Gui.drawRect(0, 0, width, height + 1, ColorUtil.moduleButtonColor)
        if (ClickGui.design.isSelected("New")) {
            Gui.drawRect(0, 0, 2, height + 1, ColorUtil.outlineColor)
        }

        /** Draw the highlight when the module is enabled. */
        if (module.enabled) {
            Gui.drawRect(0, 0, width, height + 1, ColorUtil.outlineColor)
        }

        /** Change color on hover */
        if (isButtonHovered(mouseX, mouseY)) {
            if (module.enabled)
                Gui.drawRect(0, 0,  width, height + 1, 0x55111111)
            else
                Gui.drawRect(0, 0, width , height + 1, ColorUtil.hoverColor)
        }

        /** Rendering the name in the middle */
        val displayName = if (module is KeyBind){
            module.bindName.text
        } else {
            module.name
        }
        FontUtil.drawTotalCenteredStringWithShadow(displayName, width / 2.0, 1 + height / 2.0)

        /** Render the settings elements */
        var offs = height + 1
        if (extended && menuElements.isNotEmpty()) {
            for (menuElement in menuElements) {
                menuElement.y = offs
                menuElement.update()

                offs += menuElement.drawScreen(mouseX, mouseY, partialTicks)
            }
        }

        GlStateManager.popMatrix()

        return offs
    }

    /**
	 * Handles mouse clicks for this element and dispatches them to its [menuElements].
     * @return true if an action was performed.
     * @see Element.mouseClicked
	 */
    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (isButtonHovered(mouseX, mouseY)) {
            /** Toggle the mod on left click, expand its settings on right click and show an info screen on middle click */
            if (mouseButton == 0) {
                module.toggle()
                return true
            } else if (mouseButton == 1) {
                /** toggle extended
                 * Disable listening for all members*/
                if (menuElements.size > 0) {
                    extended = !extended
                    if (!extended) {
                        menuElements.forEach {
                            it.listening = false
                        }
                    }
                }
                return true
            } else if (mouseButton == 2) {
                panel.clickgui.advancedMenu = AdvancedMenu(module)
                return true
            }
        }else if (isMouseUnderButton(mouseX, mouseY)) {
            for (menuElement in menuElements.reversed()) {
                if (menuElement.mouseClicked(mouseX, mouseY, mouseButton)) {
                    updateElements()
                    return true
                }
            }
        }
        return false
    }

    /**
     * Dispatches mouse released actions to its [menuElements]
     * @see Element.mouseReleased
     */
    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (extended) {
            for (menuElement in menuElements.reversed()) {
                menuElement.mouseReleased(mouseX, mouseY, state)
            }
        }
    }

    /**
     * Dispatches key press to its [menuElements].
     * @return true if any of the elements used the input.
     * @see Element.keyTyped
     */
    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (extended) {
            for (menuElement in menuElements.reversed()) {
                if (menuElement.keyTyped(typedChar, keyCode)) return true
            }
        }
        return false
    }

    /**
     * Returns true when the mouse is hovering over the module button.
     */
    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + height
    }

    /**
     * Returns true when the Settings are extended and the mouse below the Module Button.
     */
    private fun isMouseUnderButton(mouseX: Int, mouseY: Int): Boolean {
        if (!extended) return false
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY > yAbsolute + height
    }
}