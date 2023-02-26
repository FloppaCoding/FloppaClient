package floppaclient.module.settings.impl

import floppaclient.module.impl.render.ClickGui
import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility
import net.minecraft.util.MathHelper
import java.awt.Color

/**
 * A color setting for Modules.
 *
 * Represented by 3 or 4 sliders in the GUI.
 * @author Aton
 */
class ColorSetting(
    name: String,
    override val default: Color,
    var allowAlpha: Boolean = true,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
) : Setting<Color>(name, visibility, description){

    override var value: Color = default
        set(value) {
            field = processInput(value)
        }
    private var hsbvals: FloatArray = Color.RGBtoHSB(default.red, default.green, default.blue, null)

    var red: Int
        get() = value.red
        set(input) {
            value = Color(MathHelper.clamp_int(input,0,255), green, blue, alpha)
        }
    var green: Int
        get() = value.green
        set(input) {
            value = Color(red, MathHelper.clamp_int(input,0,255), blue, alpha)
        }
    var blue: Int
        get() = value.blue
        set(input) {
            value = Color(red, green, MathHelper.clamp_int(input,0,255), alpha)
        }
    var hue: Float
        get() {
            updateHSB()
            return hsbvals[0]
        }
        set(input) {
            hsbvals[0] = input
            updateColor()
        }
    var saturation: Float
        get() {
            updateHSB()
            return hsbvals[1]
        }
        set(input) {
            hsbvals[1] = input
            updateColor()
        }
    var brightness: Float
        get() {
            updateHSB()
            return hsbvals[2]
        }
        set(input) {
            hsbvals[2] = input
            updateColor()
        }
    var alpha: Int
        get() = value.alpha
        set(input) {
            // prevents changing the alpha if not allowed
            if (!allowAlpha) return
            value = Color(red, green, blue, MathHelper.clamp_int(input,0,255))
        }

    /**
     * Updates the color stored in value from the hsb values stored in hsbvals
     */
    private fun updateColor() {
        val tempColor =  Color(Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]))
        value = Color(tempColor.red, tempColor.green, tempColor.blue, alpha)
    }

    /**
     * Updates the values in the hsbvals field.
     * Use this instead Color.RGBtoHSB(red, green, blue, hsbvals), to avoid setting the hue to 0 when eitehr saturation
     * or brightness are 0.
     */
    private fun updateHSB() {
        val newHSB = Color.RGBtoHSB(red, green, blue, null)
        hsbvals[2] = newHSB[2]
        if (newHSB[2] > 0) {
            hsbvals[1] = newHSB[1]
            if (newHSB[1] > 0){
                hsbvals[0] = newHSB[0]
            }
        }
    }

    /**
     * Gets the value for the given color.
     * @see ColorComponent.maxValue
     */
    fun getNumber(colorNumber: ColorComponent): Double {
        return when(colorNumber) {
            ColorComponent.RED -> red.toDouble()
            ColorComponent.GREEN -> green.toDouble()
            ColorComponent.BLUE -> blue.toDouble()
            ColorComponent.HUE -> hue.toDouble()
            ColorComponent.SATURATION -> saturation.toDouble()
            ColorComponent.BRIGHTNESS -> brightness.toDouble()
            ColorComponent.ALPHA -> alpha.toDouble()
        }
    }

    /**
     * Sets the value for the specified color.
     * @see ColorComponent.maxValue
     */
    fun setNumber(colorNumber: ColorComponent, number: Double) {
        when(colorNumber) {
            ColorComponent.RED -> red = number.toInt()
            ColorComponent.GREEN -> green = number.toInt()
            ColorComponent.BLUE -> blue = number.toInt()
            ColorComponent.HUE -> hue = number.toFloat()
            ColorComponent.SATURATION -> saturation = number.toFloat()
            ColorComponent.BRIGHTNESS -> brightness = number.toFloat()
            ColorComponent.ALPHA -> alpha = number.toInt()
        }
    }

    /**
     * Returns an array of the availiable settings. Those are either red, green and blue or red, green, blue and alpha.
     */
    fun colors(): Array<ColorComponent> {
        val tempArr = if (ClickGui.colorSettingMode.isSelected("RGB"))
            arrayOf(ColorComponent.RED, ColorComponent.GREEN, ColorComponent.BLUE)
        else
            arrayOf(ColorComponent.HUE, ColorComponent.SATURATION, ColorComponent.BRIGHTNESS)
        return if (allowAlpha)
            tempArr.plus(ColorComponent.ALPHA)
        else
            tempArr
    }

    /**
     * Enum to allow for a for loop through the values.
     * This is the best solution I could come up with on the spot to circumvent that I cannot pass a reference to the
     * int values.
     */
    enum class ColorComponent() {
        RED, GREEN, BLUE, HUE, SATURATION, BRIGHTNESS, ALPHA;

        fun getName(): String {
            return when(this) {
                RED -> "Red"
                GREEN -> "Green"
                BLUE -> "Blue"
                HUE -> "Hue"
                SATURATION -> "Saturation"
                BRIGHTNESS -> "Brightness"
                ALPHA -> "Alpha"
            }
        }

        /**
         * The maximum value for the components.
         * the values range from
         *
         * 0 to 255 for [RED], [GREEN], [BLUE], [ALPHA] and from
         *
         * 0 to 1 for [HUE], [SATURATION], [BRIGHTNESS].
         */
        fun maxValue(): Double {
            return when(this) {
                RED, GREEN, BLUE, ALPHA -> 255.0
                HUE, SATURATION, BRIGHTNESS -> 1.0
            }
        }
    }
}