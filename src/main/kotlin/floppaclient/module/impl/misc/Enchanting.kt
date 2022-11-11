package floppaclient.module.impl.misc

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * This module will automatically solve Enchanting mini-games. So for it will do the Chronomatron and Supersequencer.
 *
 * @author Aton
 */
object Enchanting : Module(
    "Enchanting",
    category = Category.MISC,
    description = "Automatically completes the enchanting minigames."
){
    private val sleep = NumberSetting("Sleep", 200.0, 0.0, 1000.0, 10.0, description = "Delay in between clicks.")

    init {
        this.addSettings(
            sleep
        )
    }

    private var currentType = Type.NONE
    private var lastAdded = 0
    private var lastClick = 0L

    private val clickOrder: MutableList<Slot> = mutableListOf()
    private var listenTime = 0L
    private var listening = false
     set(value) {
         if (!field && value) {
             clickOrder.clear()
         }
         field = value
         if (value){
             listenTime = System.currentTimeMillis() + 30
         }
     }
     get() {
         return field || System.currentTimeMillis() < listenTime
     }

    /**
     * Valid indices for where to check and click are: 19..25 and 37..43
     */
    private val chronoIndices = listOf(
        19, 20, 21, 22, 23, 24, 25,
        37, 38, 39, 40, 41, 42, 43
    )

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiChest || !FloppaClient.inSkyblock) return
        val container = (event.gui as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val chestName = container.lowerChestInventory.displayName.unformattedText
            if (chestName.startsWith("Chronomatron (")) {
                currentType = Type.CHRONO
                lastAdded = 0
                clickOrder.clear()
            }else if(chestName.startsWith("Ultrasequencer (")) {
                currentType = Type.SEQUENCE
                lastAdded = 0
                clickOrder.clear()
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (currentType == Type.NONE || mc.thePlayer == null) return
        val container = mc.thePlayer.openContainer ?: return
        if (container !is ContainerChest) return
        val inventoryName = container.inventorySlots?.get(0)?.inventory?.name
        if (inventoryName == null || !inventoryName.startsWith(if (currentType == Type.CHRONO)"Chronomatron (" else "Ultrasequencer (")) {
            currentType = Type.NONE
            return
        }

        // Index at bottom for the timer / glowstone Block: 49
        listening = (container.inventorySlots[49].stack?.item as? ItemBlock)?.block === Blocks.glowstone
        if (listening) {
            if (currentType == Type.CHRONO) {
                if (lastAdded > 0){
                    if ((container.inventorySlots[lastAdded].stack?.item as? ItemBlock)?.block === Blocks.stained_hardened_clay)
                        return
                    else
                        lastAdded = 0
                }
                for (ii in chronoIndices) {
                    val slot = container.inventorySlots[ii]
                    if ((slot.stack?.item as? ItemBlock)?.block === Blocks.stained_hardened_clay) {
                        clickOrder.add(slot)
                        lastAdded = slot.slotIndex
                        break
                    }
                }
            }
            else if (currentType == Type.SEQUENCE && clickOrder.isEmpty()) {
                clickOrder.addAll(
                    container.inventorySlots.subList(9,45)
                        .filter { it.stack?.item == Items.dye }
                        .sortedBy { it.stack.stackSize }
                )
            }
        }else {
            if (System.currentTimeMillis() < lastClick + sleep.value) return
            val slot = clickOrder.firstOrNull() ?: return
            lastAdded = slot.slotIndex
            mc.playerController.windowClick(
                container.windowId,
                slot.slotNumber,
                2,
                3,
                mc.thePlayer
            )
            clickOrder.removeAt(0)
            lastClick = System.currentTimeMillis()
        }
    }

    private enum class Type{
        NONE, CHRONO, SEQUENCE
    }
}