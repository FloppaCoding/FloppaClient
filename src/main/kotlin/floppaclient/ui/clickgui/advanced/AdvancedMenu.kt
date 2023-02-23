package floppaclient.ui.clickgui.advanced

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Module
import floppaclient.module.impl.render.ClickGui
import floppaclient.module.settings.impl.*
import floppaclient.ui.clickgui.advanced.elements.AdvancedElement
import floppaclient.ui.clickgui.advanced.elements.menu.*
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.ColorUtil.textcolor
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Provides an advanced menu screen for click gui modules.
 *
 * @author Aton
 */

class AdvancedMenu(val module: Module) {
    private val elements: MutableList<AdvancedElement<*>> = mutableListOf()

    // Position parameters, for simplicity all the logic is handled in the getters and setters, so that the values dont have to be updated once every render
    private val s
        get() = ScaledResolution(mc)
    var x = 10
        set(value) {
            ClickGui.advancedRelX.value = value / s.scaledWidth.toDouble()
            field = (s.scaledWidth * ClickGui.advancedRelX.value).toInt()
        }
    var y = 10
        set(value) {
            ClickGui.advancedRelY.value = value / s.scaledHeight.toDouble()
            field = (s.scaledHeight * ClickGui.advancedRelY.value).toInt()
        }
    private var width = 10
    private var height = 10

    // For repositioning the screen.
    private var dragging = false
    private var x2 = 0
    private var y2 = 0

    // For scrolling
    private var length = 0
    private val scrollAmmount = 15
    private var scrollOffs = 0
        set(value) {
            field = MathHelper.clamp_int(value,0, length)
        }

    private val indent = 5

    init {
        for (setting in module.settings) {
            if (!setting.visibility.visibleInAdvanced) continue
            when (setting) {
                is BooleanSetting   -> elements.add(AdvancedElementCheckBox (this, module, setting))
                is NumberSetting    -> elements.add(AdvancedElementSlider   (this, module, setting))
                is StringSelectorSetting  -> elements.add(AdvancedElementStringSelector (this, module, setting))
                is SelectorSetting  -> elements.add(AdvancedElementSelector (this, module, setting))
                is StringSetting    -> elements.add(AdvancedElementTextField(this, module, setting))
                is ColorSetting     -> elements.add(AdvancedElementColor    (this, module, setting))
                is ActionSetting    -> elements.add(AdvancedElementAction   (this, module, setting))
            }
        }
        elements.add(AdvancedElementKeyBind(this, module))
    }

    /**
     * Returns true if any of the elements is listening for key inputs.
     * In that case the esc key should not exit the menu.
     */
    fun isListening() : Boolean{
        return elements.any { it.listening }
    }

    /**
     * Render the menu
     */
    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (dragging) {
            x = x2 + mouseX
            y = y2 + mouseY
        }
        updatePosition()

        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, 200).rgb

        // Set up Transform
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)

        /** Rendering the background box */
        Gui.drawRect(0, 0,  width, height, ColorUtil.elementColor)

        // Render a title bar containing the name of the module
        Gui.drawRect(0, 0, width, 15, color)
        FontUtil.drawTotalCenteredStringWithShadow(module.name, width / 2.0,  1 + 15 / 2.0, textcolor)

        //<editor-fold desc="RENDERING THE COMPONENTS IN THE GUI.">
        // Set up the Scissor Box
        val scale = mc.displayHeight /  ScaledResolution(mc).scaledHeight
        GL11.glScissor(
            (mc.displayWidth * ClickGui.advancedRelX.value).toInt(),
            (mc.displayHeight * (1- ClickGui.advancedRelHeight -ClickGui.advancedRelY.value) + indent * scale).toInt(),
            (mc.displayWidth * ClickGui.advancedRelWidth).toInt(),
            (mc.displayHeight * ClickGui.advancedRelHeight - 15 * scale - indent * scale).toInt()
        )
        GL11.glEnable(GL11.GL_SCISSOR_TEST)

        /**
         * Current render position.
         */
        var dy = 20 - scrollOffs

        /** Render the module description text */
        FontUtil.drawSplitString(module.description, indent, dy, width - 2 * indent , textcolor)
        dy += FontUtil.getSplitHeight(module.description, width-2*indent) + 10
        //Render the settings.
        for (element in elements) {
            element.setPosition(indent, dy)
            element.width = this.width - 2* indent
            element.drawScreen(mouseX, mouseY, partialTicks)
            dy += element.height
        }
        length = dy + scrollOffs

        //</editor-fold>

        // Resetting the scissor
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        GlStateManager.popMatrix()
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isMouseOnTopBar(mouseX, mouseY)) {
            x2 = x - mouseX
            y2 = y - mouseY
            dragging = true
            return true
        }
        if (isMouseInBox(mouseX, mouseY)) {
            for (element in elements.reversed()) {
                if(element.mouseClicked(mouseX, mouseY, mouseButton)) return true
            }
        }
        return isMouseInBox(mouseX, mouseY)
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state == 0) {
            dragging = false
        }
        for (element in elements.reversed()) {
            element.mouseReleased(mouseX, mouseY, state)
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        for (element in elements.reversed()) {
            if(element.keyTyped(typedChar, keyCode)) return true
        }
        return false
    }

    /**
     * Scrolls the settings by the given number of lines.
     *
     * @param amount The amount to scroll
     */
    fun scroll(amount: Int, mouseX: Int, mouseY: Int): Boolean {
        if (!isMouseInBox(mouseX, mouseY)) return false

        val diff = -amount * scrollAmmount
        scrollOffs = (scrollOffs + diff).coerceAtMost(length - height + 20).coerceAtLeast(0)
        return true

    }

    /**
     * Updates the position of the element.
     */
    private fun updatePosition() {
        val s = ScaledResolution(mc)
        x = (s.scaledWidth * ClickGui.advancedRelX.value).toInt()
        y = (s.scaledHeight * ClickGui.advancedRelY.value).toInt()
        width = (s.scaledWidth*ClickGui.advancedRelWidth).toInt()
        height = (s.scaledHeight*ClickGui.advancedRelHeight).toInt()
    }

    /**
     * Detects whether the mouse is within this gui.
     * @return true when the mouse is over the box of the Gui.
     */
    private fun isMouseInBox(mouseX: Int, mouseY: Int): Boolean{
        return mouseX >= x && mouseX < x + width && mouseY  >= y && mouseY < y + height
    }

    /**
     * Detects whether the mouse is on the top Bar of the Gui.
     */
    private fun isMouseOnTopBar(mouseX: Int, mouseY: Int): Boolean{
        return mouseX >= x && mouseX < x + width && mouseY  >= y && mouseY < y + 15
    }
}