package floppaclient.ui.clickgui.elements

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Module
import floppaclient.module.impl.keybinds.KeyBind
import floppaclient.module.impl.render.ClickGui
import floppaclient.module.settings.impl.*
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.Panel
import floppaclient.ui.clickgui.elements.menu.*
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import java.awt.Color

/**
 * Provides the toggle button for modules in the click gui.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
class ModuleButton(val module: Module, val parent: Panel) {
    val menuElements: ArrayList<Element> = ArrayList()
    var x = 0.0
    var y = 0.0
    var width = 0.0
    var height: Double = (mc.fontRendererObj.FONT_HEIGHT + 2).toDouble()
    var extended = false

    init {
        /** Register the corresponding gui element for all non-hidden settings in the module */
        updateElements()
        menuElements.add(ElementKeyBind(this, module))
    }

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
                    is StringSelectorSetting ->   ElementSelector(this, setting)
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
	 * Render the Button
	 */
    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, 150).rgb
        val color2 = Color(temp.red, temp.green, temp.blue, 100).darker().rgb

        // Change the text color and put a colored box on the element if it is toggled
        val textcolor = -0x101011
        if (module.enabled) {
            Gui.drawRect((x - 2).toInt(), y.toInt(), (x + width + 2).toInt(), (y + height + 1).toInt(), color)
        }

        /** Change color on hover */
        if (isHovered(mouseX, mouseY)) {
            if (module.enabled)
                Gui.drawRect((x - 2).toInt(), y.toInt(), (x + width + 2).toInt(), (y + height + 1).toInt(), 0x55111111)
            else
                Gui.drawRect((x - 2).toInt(), y.toInt(), (x + width + 2).toInt(), (y + height + 1).toInt(), color2)
        }

        /** Rendering the name in the middle */
        val displayName = if (module is KeyBind){
            module.bindName.text
        } else {
            module.name
        }
        FontUtil.drawTotalCenteredStringWithShadow(displayName, x + width / 2, y + 1 + height / 2, textcolor)
    }

    /**
	 * Handles mouse clicks for this element and returns true if an action was performed
	 */
    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) return false

        /** Toggle the mod on left click, expand its settings on right click and show an info screen on middle click */
        if (mouseButton == 0) {
            module.toggle()
            if (ClickGui.sound.enabled) mc.thePlayer.playSound("random.click", 0.5f, 0.5f)
        } else if (mouseButton == 1) {
            /** toggle extended
             * Disable listening for all members*/
            if (menuElements.size > 0) {
                extended = !extended
                if (ClickGui.sound.enabled)
                    if (extended) {
                        mc.thePlayer.playSound("tile.piston.out", 1f, 1f)
                    }else {
                        mc.thePlayer.playSound("tile.piston.in", 1f, 1f)
                        menuElements.forEach {
                            if (it.type == ElementType.KEY_BIND) {
                                (it as ElementKeyBind).listening = false
                            } else if (it. type == ElementType.TEXT_FIELD) {
                                (it as ElementTextField).listening = false
                            }
                        }
                    }
            }
        } else if (mouseButton == 2) {
            parent.clickgui.advancedMenu = AdvancedMenu(module)
        }
        return true
    }

    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }
}