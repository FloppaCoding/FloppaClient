package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

/**
 * Module that makes you always sprint.
 * @author Aton
 */
object AutoSprint : Module(
    "Auto Sprint",
    Keyboard.KEY_O,
    category = Category.MISC,
    description = "A simple auto sprint module that toggles sprinting when moving forwards and not collided " +
            "with anything."
) {
    @SubscribeEvent
    fun onPositionUpdate(event: PositionUpdateEvent.Pre) {
        if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.moveForward > 0) {
            mc.thePlayer.isSprinting = true
        }
    }
}