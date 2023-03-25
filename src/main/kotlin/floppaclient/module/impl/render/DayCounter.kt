package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.RegisterHudElement
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.ui.hud.HudElement
import floppaclient.utils.Utils.timeFormat

/**
 * Displays a hud showing the age of servers in both (Minecraft) days and minutes.
 * @author Stivais
 */

object DayCounter : Module(
    "Day Counter",
    category = Category.RENDER,
    description = "Renders the server's day."
) {
    private val accurateDay: Boolean by BooleanSetting("Advanced", false, description = "Displays more information.")

    @RegisterHudElement
    object DayCounter : HudElement(this, 3, 250,
        mc.fontRendererObj.getStringWidth("Day (32)"),
        mc.fontRendererObj.FONT_HEIGHT * 1 + 1,
    ) {
        override fun renderHud() {
            val time = mc.theWorld.worldTime
            val dayText = "Day: ${(time / 24000).toInt()}"

            mc.fontRendererObj.drawString(dayText, 0, 0, 0xffffff)
            this.width = mc.fontRendererObj.getStringWidth(dayText)

            if (accurateDay) {
                val accurateText = "Server age: ${timeFormat((time/0.02).toLong())}"

                mc.fontRendererObj.drawString(accurateText, 0, (mc.fontRendererObj.FONT_HEIGHT + 1), 0xffffff)
                this.width = mc.fontRendererObj.getStringWidth(accurateText)
                this.height = mc.fontRendererObj.FONT_HEIGHT * 2 + 1
            }
        }
    }
}