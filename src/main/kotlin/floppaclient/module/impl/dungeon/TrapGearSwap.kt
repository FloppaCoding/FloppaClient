package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.RoomChangeEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module to automatically equip the needed gear when entering new trap room.
 * @author Aton
 */
object TrapGearSwap : Module(
    "Trap Gear Swap",
    category = Category.DUNGEON,
    description = "Automatically swaps to a rabbit hat when entering new trap and swaps back to your previous helmet upon leaving the room. " +
            "Can be configured to swap out your boots for Spring Boots instead.\n" +
            "§eRequires Auto Scan to be enabled in Dungeon Map! "
){
    private val springBoots = BooleanSetting("Spring Boots", false, description = "Will swap your boots for Spring Boots instead of swapping your helmet for a Rabbit Hat.")

    init {
        this.addSettings(
            springBoots
        )
    }

    var previousName: String? = null

    @SubscribeEvent
    fun onRoomChange(event: RoomChangeEvent){
        // When entering trap
        val itemName = if (event.newRoom?.data?.name == "New Trap") {
            previousName = mc.thePlayer?.getEquipmentInSlot(if (springBoots.enabled) 1 else 4)?.displayName ?: return
            if (springBoots.enabled){
                "SPRING_BOOTS"
            }else {
                "Rabbit Hat"
            }
        }else if (event.oldRoom?.data?.name == "New Trap") {
            previousName ?: return
        }else return

        FakeActionUtils.swapArmorItem(itemName, ignoreCase = false)
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        previousName = null
    }
}