package floppaclient.ui.clickgui.elements.menu

import floppaclient.FloppaClient
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.ui.clickgui.elements.Element
import floppaclient.ui.clickgui.elements.ElementType
import floppaclient.ui.clickgui.elements.ModuleButton
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
 * Provides a color selector element.
 * Based on HeroCode's gui.
 *
 * @author Aton
 */
class ElementColor(iparent: ModuleButton, iset: Setting) : Element() {
    var dragging: Int?

    private val hueScale = ResourceLocation("floppaclient", "gui/HueScale.png")

    init {
        parent = iparent
        setting = iset
        dragging = null
        type = ElementType.COLOR
        super.setup()
    }

    /**
     * Renders the element
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val temp = ColorUtil.clickGUIColor
        val color = Color(temp.red, temp.green, temp.blue, 150).rgb
        val color2 = Color(temp.red, temp.green, temp.blue,  230).rgb

        val colorValue = (setting as ColorSetting).value.rgb

        /** Render the box and text */
        if(parent?.parent?.shouldRender(y + 15) == true) {
            Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), ColorUtil.elementColor)
            FontUtil.drawString(displayName, x + 1, y + 2, -0x1)

            // Render the color preview
            Gui.drawRect(
                (x + width - 26).toInt(),
                (y + 2).toInt(),
                (x + width - 1).toInt(),
                (y + 11).toInt(),
                colorValue
            )

            // Render the tab indicating the drop down
            Gui.drawRect(x.toInt(), (y + 13).toInt(), (x + width).toInt(), (y + 15).toInt(), 0x77000000)
            Gui.drawRect(
                (x + width * 0.4).toInt(),
                (y + 12).toInt(),
                (x + width * 0.6).toInt(),
                (y + 15).toInt(),
                color
            )
        }

        // Render the extended
        if (comboextended) {
            val startY = parent?.parent?.validStart(y + 15) ?: (y + 15)
            Gui.drawRect(x.toInt(), (startY).toInt(), (x + width).toInt(), (y + height).toInt(), -0x55ededee)
            var ay = y + 15
            val increment = 15

            // Render the color sliders
            for (currentColor in (setting as ColorSetting).colors()) {
                if(parent?.parent?.shouldRender(ay + increment) == true) {
                    // If hue, render the hue bar.
                    if (currentColor == ColorSetting.ColorComponent.HUE) {
                        hueScale.let {
                            GL11.glPushMatrix()
                            GlStateManager.color(255f, 255f, 255f, 255f)
                            FloppaClient.mc.textureManager.bindTexture(it)
                            Gui.drawModalRectWithCustomSizedTexture(
                                x.toInt(),
                                ay.toInt(),
                                0f, 0f, width.toInt(), 11, width.toFloat(), 11.toFloat()
                            )
                            GL11.glPopMatrix()
                        }
                    }

                    val dispVal = "" + ((setting as ColorSetting).getNumber(currentColor) * 100.0).roundToInt() / 100.0
                    FontUtil.drawString(currentColor.getName(), x + 1, ay + 2, -0x1)
                    FontUtil.drawString(dispVal, x + width - FontUtil.getStringWidth(dispVal), ay + 2, -0x1)

                    val maxVal = currentColor.maxValue()
                    val percentage = (setting as ColorSetting).getNumber(currentColor)  / maxVal
                    Gui.drawRect(x.toInt(), (ay + 12).toInt(), (x + width).toInt(), (ay + 13.5).toInt(), -0xefeff0)
                    Gui.drawRect(x.toInt(), (ay + 12).toInt(), (x + percentage * width).toInt(), (ay + 13.5).toInt(), color)
                    if (percentage > 0 && percentage < 1) Gui.drawRect(
                        (x + percentage * width - 1).toInt(), (ay + 12).toInt(), (x + (percentage * width).coerceAtMost(width)).toInt(), (ay + 13.5).toInt(), color2
                    )

                    /** Calculate and set new value when dragging */
                    if (dragging == currentColor.ordinal) {
                        val newVal = MathHelper.clamp_double((mouseX - x) / width, 0.0, 1.0) * maxVal
                        (setting as ColorSetting).setNumber(currentColor, newVal)
                    }
                }
                ay += increment
            }

        }
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
            for (currentColor in (setting as ColorSetting).colors()) {
                if(parent?.parent?.shouldRender(ay + increment) == true) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= ay && mouseY <= ay + increment) {
                        dragging = currentColor.ordinal
                        return true
                    }
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
        val scaledresolution = ScaledResolution(FloppaClient.mc)
        val i1: Int = scaledresolution.scaledWidth
        val j1: Int = scaledresolution.scaledHeight
        val k1: Int = Mouse.getX() * i1 / FloppaClient.mc.displayWidth
        val l1: Int = j1 - Mouse.getY() * j1 / FloppaClient.mc.displayHeight - 1
        val scale = 2.0 / FloppaClient.mc.gameSettings.guiScale
        val scaledMouseX = (k1 / scale).toInt()
        val scaledMouseY = (l1 / scale).toInt()

        var ay = y + 15
        val increment = 15
        for (currentColor in (setting as ColorSetting).colors()) {
            if(parent?.parent?.shouldRender(ay + increment) == true) {
                if (scaledMouseX >= x && scaledMouseX <= x + width && scaledMouseY >= ay && scaledMouseY <= ay + increment) {
                    if (keyCode == Keyboard.KEY_RIGHT){
                        (setting as ColorSetting).setNumber(currentColor, (setting as ColorSetting).getNumber(currentColor)+currentColor.maxValue()/255.0)
                    }
                    if (keyCode == Keyboard.KEY_LEFT){
                        (setting as ColorSetting).setNumber(currentColor, (setting as ColorSetting).getNumber(currentColor)-currentColor.maxValue()/255.0)
                    }
                    return true
                }
            }
            ay += 15
        }
        return super.keyTyped(typedChar, keyCode)
    }


    /**
     * Checks whether the mouse is hovering the selector
     */
    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 15 && parent?.parent?.shouldRender(y+15) ?: false
    }
}