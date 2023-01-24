package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module that hold down sprint key for you.
 * @author Aton
 */
object AutoSprint : Module(
    "Auto Sprint",
    category = Category.MISC,
    description = "A simple auto sprint module that toggles sprinting when moving forwards and not collided anything."
) {


    @SubscribeEvent
    fun onPositionUpdate(event: PositionUpdateEvent.Pre) {
        if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.moveForward > 0) {
            mc.thePlayer.isSprinting = true
        }
    }
}