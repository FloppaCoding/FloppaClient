package floppaclient.ui.clickgui.advanced.elements.menu

import floppaclient.FloppaClient.Companion.RESOURCE_DOMAIN
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Module
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.advanced.elements.AdvancedElement
import floppaclient.ui.clickgui.advanced.elements.AdvancedElementType
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Provides a color selector element for the advanced gui.
 *
 * @author Aton
 */
class AdvancedElementColor(
    parent: AdvancedMenu, module: Module, setting: ColorSetting,
) : AdvancedElement<ColorSetting>(parent, module, setting, AdvancedElementType.COLOR) {
    private var dragging: Int? = null

    private val hueScale = ResourceLocation(RESOURCE_DOMAIN, "gui/HueScale.png")

    /**
     * Renders the element
     */
    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float) : Int{
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, 150).rgb
        val color2 = Color(temp.red, temp.green, temp.blue,  230).rgb

        val colorValue = setting.value.rgb

        //<editor-fold desc="Render the box and text">
        FontUtil.drawString(setting.name, 1, 2, -0x1)

        // Render the color preview
        Gui.drawRect(
            settingWidth - 26,
            2,
            settingWidth - 1,
            11,
            colorValue
        )

        // Render the tab indicating the drop down
        Gui.drawRect(0, 13, settingWidth, 15, 0x77000000)
        Gui.drawRect(
            (settingWidth * 0.4).toInt(),
            12,
            (settingWidth * 0.6).toInt(),
            15,
            color
        )
        //</editor-fold>

        // Render the extended
        var ay = 15
        if (comboextended) {
            val startY = 15
            Gui.drawRect(0, startY, settingWidth, settingHeight, -0x55ededee)
            val increment = 15

            // Render the color sliders
            for (currentColor in setting.colors()) {

                // If hue, render the hue bar.
                if (currentColor == ColorSetting.ColorComponent.HUE) {
                    hueScale.let {
                        GL11.glPushMatrix()
                        GlStateManager.color(255f, 255f, 255f, 255f)
                        mc.textureManager.bindTexture(it)
                        Gui.drawModalRectWithCustomSizedTexture(
                            0, ay, 0f, 0f, settingWidth, 11, settingWidth.toFloat(), 11.toFloat()
                        )
                        GL11.glPopMatrix()
                    }
                }

                val dispVal = "" + (setting.getNumber(currentColor) * 100.0).roundToInt() / 100.0
                FontUtil.drawString(currentColor.getName(), 1, ay + 2, -0x1)
                FontUtil.drawString(dispVal, settingWidth - FontUtil.getStringWidth(dispVal), ay + 2, -0x1)

                val maxVal = currentColor.maxValue()
                val percentage = setting.getNumber(currentColor)  / maxVal
                Gui.drawRect(0, (ay + 12), settingWidth, ay + 14, -0xefeff0)
                Gui.drawRect(0, (ay + 12), (percentage * settingWidth).toInt(), ay + 14, color)
                if (percentage > 0 && percentage < 1) Gui.drawRect(
                    (percentage * settingWidth - 2).toInt(), ay + 12, ((percentage * settingWidth).toInt().coerceAtMost(settingWidth)), ay + 14, color2
                )

                /** Calculate and set new value when dragging */
                if (dragging == currentColor.ordinal) {
                    val newVal = MathHelper.clamp_double((mouseX - parent.x - x) / settingWidth.toDouble(), 0.0, 1.0) * maxVal
                    setting.setNumber(currentColor, newVal)
                }

                ay += increment
            }

        }
        return ay
    }

    /**
     * Handles interaction with this element.
     * Returns true if interacted with the element to cancel further interactions.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (isButtonHovered(mouseX, mouseY)) {
                // for now also extend on left click
                comboextended = !comboextended
                return true
            }

            if (!comboextended) return false
            var ay = y + 15
            val increment = 15
            for (currentColor in setting.colors()) {
                if (mouseX >= parent.x + x && mouseX <= parent.x + x + settingWidth && mouseY >= parent.y + ay && mouseY <= parent.y + ay + increment) {
                    dragging = currentColor.ordinal
                    return true
                }
                ay += 15
            }
        } else if( mouseButton == 1) {
            if (isButtonHovered(mouseX, mouseY)) {
                comboextended = !comboextended
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Stops slider action on mouse release
     */
    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging = null
    }

    /**
     * Check for arrow keys to move the slider by one increment.
     */
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!comboextended) return false
        val scaledresolution = ScaledResolution(mc)
        val i1: Int = scaledresolution.scaledWidth
        val j1: Int = scaledresolution.scaledHeight
        val k1: Int = Mouse.getX() * i1 / mc.displayWidth
        val l1: Int = j1 - Mouse.getY() * j1 / mc.displayHeight - 1
        val scale = 2.0 / mc.gameSettings.guiScale
        val scaledMouseX = (k1 / scale).toInt()
        val scaledMouseY = (l1 / scale).toInt()

        var ay = y + 15
        val increment = 15
        for (currentColor in setting.colors()) {

            if (scaledMouseX >= parent.x + x && scaledMouseX <= parent.x + x + settingWidth && scaledMouseY >= parent.y + ay && scaledMouseY <= parent.y + ay + increment) {
                if (keyCode == Keyboard.KEY_RIGHT){
                    setting.setNumber(currentColor, setting.getNumber(currentColor)+currentColor.maxValue()/255.0)
                }
                if (keyCode == Keyboard.KEY_LEFT){
                    setting.setNumber(currentColor, setting.getNumber(currentColor)-currentColor.maxValue()/255.0)
                }
                return true
            }

            ay += 15
        }
        return super.keyTyped(typedChar, keyCode)
    }


    /**
     * Checks whether the mouse is hovering the selector
     */
    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= parent.x + x && mouseX <= parent.x + x + settingWidth && mouseY >= parent.y + y && mouseY <= parent.y + y + 15
    }
}