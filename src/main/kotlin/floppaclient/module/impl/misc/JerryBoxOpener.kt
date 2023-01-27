package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.ItemUtils.itemID
import floppaclient.utils.ItemUtils.lore
import floppaclient.utils.Utils
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.util.Calendar

/**
 * A macro to automatically open all jerry boxes in the inventory.
 *
 * @author Aton
 */
object JerryBoxOpener : Module(
    "Jerry Box Opener",
    category = Category.MISC,
    description = "Opens all the jerry boxes you have in your inventory. To start the opening process right click a jerry box.\n" +
            "To abort run the command \"/fcl stop\""
){
    private var nextItemUse = 0L
    private var nextOpen = 0L
    private var nextClaim = 0L

    private var startTime = 0L

    private var pendingTime = 0L

    private var opening = false
    private var inJerryGui = false

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (!inSkyblock || opening) return
        if (mc.thePlayer.heldItem.itemID.matches(jerryIDMatcher)) {
            opening = true
            startTime = System.currentTimeMillis()
            nextItemUse = System.currentTimeMillis() + 1000L
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minutes = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)
            modMessage("Started opening Jerry Boxes at $hour:$minutes:$seconds.")
        }
    }

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (mc.thePlayer == null) return
        if (!opening || event.gui !is GuiChest) return
        val container = (event.gui as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val chestName = container.lowerChestInventory.displayName.unformattedText
            if (chestName.startsWith("Open a Jerry Box")) {
                inJerryGui = true
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if(event.phase != TickEvent.Phase.START || !opening) return
        // If in the gui handle that
        val container = mc.thePlayer.openContainer
        if (inJerryGui) run boxGUi@{
            val inventoryName = (container as? ContainerChest)?.lowerChestInventory?.displayName?.unformattedText
            if (inventoryName == null || !inventoryName.startsWith("Open a Jerry Box")) {
                inJerryGui = false
                return@boxGUi
            }
            when (container.inventorySlots[22].stack?.lore?.last()) {
                "§eClick to open!" -> {
                    if (System.currentTimeMillis() >= nextOpen) {
                        Utils.leftClickWindow(container.windowId, 22)
                        nextOpen = System.currentTimeMillis() + 1000L
                    }
                }
                "§eClick to claim!" -> {
                    if (System.currentTimeMillis() >= nextClaim) {
                        Utils.leftClickWindow(container.windowId, 22)
                        nextClaim = System.currentTimeMillis() + 1000L
                    }
                }
//                "§7It's rolling..." -> {}
            }
        }
        // If not in the jerry gui (anymore) and still opening: open the next box
        if (!inJerryGui && mc.currentScreen == null && System.currentTimeMillis() >= nextItemUse) {
            if (mc.thePlayer?.heldItem?.itemID?.matches(jerryIDMatcher) == true) {
                Utils.rightClick()
                nextItemUse = System.currentTimeMillis() + 1000L
            }else if (System.currentTimeMillis() >= pendingTime){
                val keepGoing = FakeActionUtils.swapItemToSlot(jerryIDMatcher, mc.thePlayer.inventory.currentItem, matchMode = 2)
                if (keepGoing) {
                    // Theoretically we could already queue a click on the jerry box here, but we can also just wait for the next tick.
                    pendingTime = System.currentTimeMillis() + 500L
                }else {
                    opening = false
                    val calendar = Calendar.getInstance()
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minutes = calendar.get(Calendar.MINUTE)
                    val seconds = calendar.get(Calendar.SECOND)
                    modMessage("Finished opening all jerry boxes at $hour:$minutes:$seconds after ${(System.currentTimeMillis() - startTime) / 1000} seconds")
                }
            }
        }
    }

    /**
     * Stops opening boxes.
     */
    fun abort() {
        opening = false
    }

    @SubscribeEvent
    fun onWarp(event: WorldEvent.Unload) {
        opening = false
        inJerryGui = false
    }

    private var jerryIDMatcher = Regex("JERRY_BOX_(GREEN|BLUE|PURPLE|GOLDEN)")
}