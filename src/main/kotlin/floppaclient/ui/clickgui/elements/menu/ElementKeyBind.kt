package floppaclient.ui.clickgui.elements.menu

import floppaclient.module.Module
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color

/**
 * Provides a key bind element.
 *
 * @author Aton
 */
class ElementKeyBind(iparent: ModuleButton, val mod: Module) : Element() {

    var listening = false

    private val keyBlackList = intArrayOf()

    init {
        parent = iparent
        type = ElementType.KEY_BIND
        super.setup()
    }

    /**
     * Render the element
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val keyName = if(mod.keyCode > 0)
            Keyboard.getKeyName(mod.keyCode) ?: "Err"
        else if (mod.keyCode < 0)
            Mouse.getButtonName(mod.keyCode + 100)
        else
            ".."
        val displayValue = "[$keyName]"
        val temp = ColorUtil.clickGUIColor
        val color = if (listening) {
            Color(temp.red, temp.green, temp.blue, 200).rgb
        }else {
            ColorUtil.elementColor
        }

        /** Rendering the box */
        Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), color)

        /** Titel und Checkbox rendern. */
        FontUtil.drawString(displayName, x + 1, y + 2, -0x1)
        FontUtil.drawString(displayValue, x + width - FontUtil.getStringWidth(displayValue), y + 2, -0x1)
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed.
     * Used to interact with the element and to register mouse binds.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isCheckHovered(mouseX, mouseY) ) {
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
            }else if (!keyBlackList.contains(keyCode)) {
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
        return mouseX >= x && mouseX <= x + width && mouseY >= y  && mouseY <= y + height
    }
}