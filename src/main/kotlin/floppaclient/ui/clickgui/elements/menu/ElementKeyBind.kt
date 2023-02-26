package floppaclient.ui.clickgui.elements.menu

import floppaclient.module.Module
import floppaclient.module.settings.impl.DummySetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.FontUtil
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

/**
 * Provides a key bind element.
 *
 * @author Aton
 */
class ElementKeyBind(parent: ModuleButton, val mod: Module) :
    Element<DummySetting>(parent, DummySetting("Key Bind"), ElementType.KEY_BIND) {

    private val keyBlackList = intArrayOf()


    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        val keyName = if (mod.keyCode > 0)
            Keyboard.getKeyName(mod.keyCode) ?: "Err"
        else if (mod.keyCode < 0)
            Mouse.getButtonName(mod.keyCode + 100)
        else
            ".."
        val displayValue = "[$keyName]"

        FontUtil.drawString(displayName, 1, 2)
        FontUtil.drawString(displayValue, width - FontUtil.getStringWidth(displayValue), 2)

        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed.
     * Used to interact with the element and to register mouse binds.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isCheckHovered(mouseX, mouseY)) {
            listening = !listening
            return true
        } else if (listening) {
            mod.keyCode = -100 + mouseButton
            listening = false
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Register key strokes. Used to set the key bind.
     */
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) {
                mod.keyCode = Keyboard.KEY_NONE
                listening = false
            } else if (keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                listening = false
            } else if (!keyBlackList.contains(keyCode)) {
                mod.keyCode = keyCode
                listening = false
            }
            return true
        }
        return super.keyTyped(typedChar, keyCode)
    }

    /**
     * Checks whether this element is hovered
     */
    private fun isCheckHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + height
    }
}