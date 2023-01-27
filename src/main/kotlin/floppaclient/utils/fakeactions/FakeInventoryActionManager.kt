package floppaclient.utils.fakeactions

import floppaclient.FloppaClient.Companion.mc
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

/**
 * A Utility class for performing item movements in the players inventory all within a single game tick.
 *
 * Use [addAction] to queue an interaction with the player inventory.
 *
 * All added actions will be executed at the end of the tick if possible and cleared.
 *
 * @author Aton
 */
object FakeInventoryActionManager {

    private val actions: MutableList<(GuiInventory) -> Unit> = mutableListOf()

    /**
     * Performs the inventory action and skips the rendering of the inventory.
     */
    @SubscribeEvent
    fun inTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (actions.isEmpty()) return
        val inventory = GuiInventory(mc.thePlayer)
        if (!mc.playerController.isRidingHorse) {
            val inventoryOpened = if (mc.currentScreen != (inventory as GuiScreen)) {
                mc.netHandler
                    .addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))
                mc.displayGuiScreen(inventory)
                true
            }else false

            // perform all the actions
            actions.forEach { it(inventory) }

            if (inventoryOpened) mc.thePlayer.closeScreen()
        }
        actions.clear()
        return
    }

    /**
     * Add an action to the list of actions that should be performed.
     */
    fun addAction(action: (GuiInventory) -> Unit) {
        actions.add(action)
    }
}