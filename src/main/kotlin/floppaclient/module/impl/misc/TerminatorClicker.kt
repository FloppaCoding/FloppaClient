package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
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
    description = "Auto clicks terminator when holding right click with it."
){
    private val sleep = NumberSetting("Sleep ms",50.0,10.0,100.0,5.0, description = "Delay in between clicks.")

    private var lastTermClick: Long = 0L
    init {
        this.addSettings(
            sleep
        )
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent?) {
        if (mc.gameSettings.keyBindUseItem.isKeyDown) {
            val held = mc.thePlayer.inventory.getCurrentItem()
            if (held != null) {
                val displayName = held.displayName
                if (displayName.contains("Terminator") || displayName.contains("Juju")) {
                    if (System.currentTimeMillis() - lastTermClick >= sleep.value) {
                        if (mc.playerController.sendUseItem(
                                mc.thePlayer as EntityPlayer,
                                mc.theWorld as World,
                                held
                            )
                        ) mc.entityRenderer.itemRenderer.resetEquippedProgress2()
                        lastTermClick =
                            System.currentTimeMillis() - ((System.currentTimeMillis() - lastTermClick) % sleep.value.toInt())
                    }
                }
            }
        }
    }
}