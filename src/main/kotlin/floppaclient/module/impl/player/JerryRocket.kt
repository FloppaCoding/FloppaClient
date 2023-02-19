package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.VelocityUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.inventory.SkyblockItem
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * The only way is up!
 * @author Aton
 */
object JerryRocket : Module(
    "Jerry Rocket",
    category = Category.PLAYER,
    description = "Cancels horizontal kb when holding the jerry chine gun."
){

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun handleEntityVelocity(event: VelocityUpdateEvent){
        if (mc.theWorld.getEntityByID(event.packet.entityID) == mc.thePlayer) {
            if (mc.thePlayer.isHolding(SkyblockItem.JERRY_GUN) && event.packet.motionY == 4800) {
                mc.thePlayer.motionY = event.packet.motionY/8000.0
                event.isCanceled = true
            }
        }
    }
}