package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent

/**
 * Remove the front view from the toggle perspective rotation.
 * @author Aton
 */
object RemoveFrontView : Module(
    "No Front View",
    category = Category.MISC,
    description = "Skips the front view when toggling perspective."
){
    @SubscribeEvent
    fun onKey(event: InputEvent.KeyInputEvent) {
        /** adds +1 tp the perspective setting to skip front view, does not override the vanilla toggle
         * isKeyDown has to be used here instead of isKeyPressed because the vanilla function is called first.*/
        if (mc.gameSettings.keyBindTogglePerspective.isKeyDown) {
            if(mc.gameSettings.thirdPersonView >= 2) { //front view
                mc.gameSettings.thirdPersonView = 0
            }
        }
    }
}