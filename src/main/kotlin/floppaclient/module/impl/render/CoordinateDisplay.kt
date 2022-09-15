package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.hud.HudElement
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.common.MinecraftForge
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Render a coordinate hud on your screen
 * @author Aton
 */
object CoordinateDisplay : Module(
    "Coordinate HUD",
    category = Category.RENDER,
    description = "Renders your coordinates on your screen."
) {
    private val showLookingAt = BooleanSetting("Looking At", false, description = "Displays the coordinates of the block you are looking at in a second line.")

    private val xHud = NumberSetting("x", default = 0.0, hidden = true)
    private val yHud = NumberSetting("y", default = 150.0, hidden = true)
    private val scaleHud = NumberSetting("scale",1.0,0.1,4.0, 0.01, hidden = true)

    init {
        this.addSettings(
            showLookingAt,
            xHud,
            yHud,
            scaleHud
        )
    }

    override fun onEnable() {
        MinecraftForge.EVENT_BUS.register(CoordinateHUD)
        super.onEnable()
    }

    override fun onDisable() {
        MinecraftForge.EVENT_BUS.unregister(CoordinateHUD)
        super.onDisable()
    }

    object CoordinateHUD : HudElement(
        xHud,
        yHud,
        mc.fontRendererObj.getStringWidth("123 / 12 / 123 (12.3 / 12.3)"),
        mc.fontRendererObj.FONT_HEIGHT * 2 + 1,
        scaleHud
    ) {
        override fun renderHud() {

            val player: EntityPlayer = mc.thePlayer

            var xDir = ((player.rotationYaw % 360 + 360) % 360).toDouble()
            if (xDir > 180) xDir -= 360.0
            xDir = (xDir * 10.0).roundToInt().toDouble() / 10.0
            val yDir = (player.rotationPitch * 10.0).roundToInt().toDouble() / 10.0

            val coordText =
                "${floor(player.posX).toInt()} / ${floor(player.posY).toInt()} / ${floor(player.posZ).toInt()} ($xDir / $yDir)"

            mc.fontRendererObj.drawString(coordText, 0, 0, 0xffffff)

            // handle looking at
            if (showLookingAt.enabled) {
                val la = mc.objectMouseOver

                if (la != null && la.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    val laText = "Looking at: ${la.blockPos.x} / ${la.blockPos.y} / ${la.blockPos.z}"
                    mc.fontRendererObj.drawString(laText, 0, mc.fontRendererObj.FONT_HEIGHT + 1, 0xffffff)
                }
            }

            this.width = mc.fontRendererObj.getStringWidth(coordText)

            super.renderHud()
        }
    }

}