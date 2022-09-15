package floppaclient.ui.clickgui.util

import floppaclient.module.impl.render.ClickGui
import java.awt.Color

/**
 * Provides color for the click gui.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
object ColorUtil {
    val clickGUIColor: Color
        get() = ClickGui.color.value

    val elementColor: Int
     get() = if (ClickGui.design.isSelected("New"))
             newColor
         else if (ClickGui.design.isSelected("JellyLike"))
             jellyColor
         else
             0

    val bgColor: Int
        get() = if (ClickGui.design.isSelected("New"))
            newColor
        else if (ClickGui.design.isSelected("JellyLike"))
            Color(255,255,255,50).rgb
        else
            0
    const val jellyColor = -0x44eaeaeb
    const val newColor = -0xdcdcdd
    const val moduleButtonColor = -0xe5e5e6
    const val textcolor = -0x101011

}