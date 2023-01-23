package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import kotlin.concurrent.schedule


/**
 * Simple module that turns mined blocks into ghost blocks. Helpful for high ping (However I don't know if it works cuz i haven't tested it
 *
 * PUT THIS INTO THE FAST BREAK STUFF
 *
 * @author Stivais
 */

object PinglessHardstone : Module(
    "Pingless Hardstone",
    category = Category.MISC,
    description = "Improves mining for people with higher ping."
) {

    private val cd = NumberSetting("Cooldown", 50.0, 10.0, 150.0, 10.0, description = "Speed of hardstone disappearing")

    init {
        addSettings(
            cd,
        )
    }

    private var isLeftClickDown = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null) return
        isLeftClickDown = mc.gameSettings.keyBindAttack.isKeyDown

    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            mine()
        }
    }


    private fun mine() {
        if (mc.thePlayer == null) return

        val pos = mc.objectMouseOver.blockPos
        Timer().schedule(cd.value.toLong()) {
            mc.theWorld.setBlockToAir(pos)
        }

    }
}