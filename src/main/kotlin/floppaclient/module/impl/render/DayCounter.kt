package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.hud.HudElement
import net.minecraftforge.common.MinecraftForge

/**
 * Displays a hud showing the age of servers in both (Minecraft) days and minutes.
 * @author Stivais
 */

object DayCounter : Module(
    "Day Counter",
    category = Category.RENDER,
    description = "Renders the server's day."
) {
    private val accurateDay = BooleanSetting("Advanced", false, description = "Displays more information.")

    private val xHud = NumberSetting("x", default = 0.0, visibility = Visibility.HIDDEN)
    private val yHud = NumberSetting("y", default = 150.0, visibility = Visibility.HIDDEN)
    private val scaleHud = NumberSetting("scale", 1.0, 0.1, 4.0, 0.01, Visibility.HIDDEN)

    init {
        this.addSettings(
            accurateDay,
            xHud,
            yHud,
            scaleHud
        )
    }

    override fun onEnable() {
        MinecraftForge.EVENT_BUS.register(DayCounter)
        super.onEnable()
    }

    override fun onDisable() {
        MinecraftForge.EVENT_BUS.unregister(DayCounter)
        super.onDisable()
    }

    object DayCounter : HudElement(
        xHud,
        yHud,
        mc.fontRendererObj.getStringWidth("Day (32)"),
        mc.fontRendererObj.FONT_HEIGHT * 2 + 1,
        scaleHud
    ) {
        override fun renderHud() {
            val time = mc.theWorld.worldTime

            val dayText = "Day: ${(time / 24000).toInt()}"

            mc.fontRendererObj.drawString(dayText, 0, 0, 0xffffff)
            this.width = mc.fontRendererObj.getStringWidth(dayText)

            if (accurateDay.enabled) {
                val adText = "Server age: ${(time / 1200).toInt()} Minutes"

                mc.fontRendererObj.drawString(adText, 0, (mc.fontRendererObj.FONT_HEIGHT + 1), 0xffffff)
                this.width = mc.fontRendererObj.getStringWidth(adText)
            }
            super.renderHud()
        }
    }
}