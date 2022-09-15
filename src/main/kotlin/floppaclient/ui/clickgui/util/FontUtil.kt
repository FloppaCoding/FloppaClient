package floppaclient.ui.clickgui.util

import floppaclient.FloppaClient.Companion.mc
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.StringUtils
import java.util.*

/**
 * Provides methods for rending text.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
object FontUtil {
    private var fontRenderer: FontRenderer? = null
    fun setupFontUtils() {
        fontRenderer = mc.fontRendererObj
    }

    fun getStringWidth(text: String?): Int {
        return fontRenderer!!.getStringWidth(StringUtils.stripControlCodes(text))
    }

    fun getSplitHeight(text: String, wrapWidth: Int): Int {
        var dy = 0
        for (s in mc.fontRendererObj.listFormattedStringToWidth(text, wrapWidth)) {
            dy += mc.fontRendererObj.FONT_HEIGHT
        }
        return dy
    }

    val fontHeight: Int
        get() = fontRenderer!!.FONT_HEIGHT

    fun drawString(text: String?, x: Double, y: Double, color: Int) {
        drawString(text, x.toInt(), y.toInt(), color)
    }

    fun drawString(text: String?, x: Int, y: Int, color: Int) {
        fontRenderer!!.drawString(text, x, y, color)
    }

    fun drawStringWithShadow(text: String?, x: Double, y: Double, color: Int) {
        fontRenderer?.drawStringWithShadow(text, x.toFloat(), y.toFloat(), color)
    }

    fun drawCenteredString(text: String?, x: Double, y: Double, color: Int) {
        drawString(text, x - fontRenderer!!.getStringWidth(text) / 2, y, color)
    }

    fun drawCenteredStringWithShadow(text: String?, x: Double, y: Double, color: Int) {
        drawStringWithShadow(text, x - fontRenderer!!.getStringWidth(text) / 2, y, color)
    }

    fun drawTotalCenteredString(text: String?, x: Double, y: Double, color: Int) {
        drawString(text, x - fontRenderer!!.getStringWidth(text) / 2, y - fontRenderer!!.FONT_HEIGHT / 2, color)
    }

    fun drawTotalCenteredStringWithShadow(text: String?, x: Double, y: Double, color: Int) {
        drawStringWithShadow(
            text,
            x - fontRenderer!!.getStringWidth(text) / 2,
            y - fontRenderer!!.FONT_HEIGHT / 2f,
            color
        )
    }

    /**
     * Draws a string with line wrapping.
     */
    fun drawSplitString(text: String, x: Int, y: Int, wrapWidth: Int, color: Int) {
        fontRenderer?.drawSplitString(text, x, y, wrapWidth, color)
    }

    /**
     * Returns a copy of the String where the first letter is capitalized.
     */
    fun String.forceCapitalize(): String {
        return this.substring(0, 1).uppercase(Locale.getDefault()) + this.substring(1, this.length)
    }

    /**
     * Returns a copy of the String where the only first letter is capitalized.
     */
    fun String.capitalizeOnlyFirst(): String {
        return this.substring(0, 1).uppercase(Locale.getDefault()) + this.substring(1, this.length).lowercase()
    }
}