package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.inventory.SkyblockItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Spams right click when holding down RMB wile terminator in hand. STOP CARPEL TUNNEL!
 * @author Aton.
 */
object TerminatorClicker : Module(
    "Terminator Clicker",
    category = Category.MISC,
    description = "Automatically shoots the held shortbow when holding right click with it. " +
            "The clicks from this happen additionally to the 5 cps auto clicker from vanilla minecraft."
){
    private val sleep: Double by NumberSetting("Sleep ms",65.0,10.0,100.0,5.0, description = "Delay in between clicks.")

    private var nextTermClick: Long = 0L

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (mc.gameSettings.keyBindUseItem.isKeyDown) {
            val held = mc.thePlayer.inventory.getCurrentItem()
            if (held != null) {
                if (mc.thePlayer.isHolding(SkyblockItem.Attribute.SHORTBOW)) {
                    val nowMillis = System.currentTimeMillis()
                    if (nowMillis >= nextTermClick) {
                        if (mc.playerController.sendUseItem(
                                mc.thePlayer as EntityPlayer,
                                mc.theWorld as World,
                                held
                            )
                        ) mc.entityRenderer.itemRenderer.resetEquippedProgress2()
                        val overshoot =  (nowMillis - nextTermClick).takeIf { it < 200 } ?: 0L
                        nextTermClick = nowMillis + (sleep.toLong() - overshoot).coerceAtLeast(0L)
                    }
                }
            }
        }
    }
}