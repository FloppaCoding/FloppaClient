package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.fakeactions.FakeInventoryActionManager
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.inventory.Slot

object HotbarSwapper : Module(
    "Hotbar Swapper",
    category = Category.PLAYER,
    description = "Swaps out the the items in the specified slots in your hotbar with the slots above them when you press the key bind."
){
    private val slot0 = BooleanSetting("Slot 1",false, description = "Toggle whether this slot should be swapped.")
    private val slot1 = BooleanSetting("Slot 2",false, description = "Toggle whether this slot should be swapped.")
    private val slot2 = BooleanSetting("Slot 3",false, description = "Toggle whether this slot should be swapped.")
    private val slot3 = BooleanSetting("Slot 4",false, description = "Toggle whether this slot should be swapped.")
    private val slot4 = BooleanSetting("Slot 5",false, description = "Toggle whether this slot should be swapped.")
    private val slot5 = BooleanSetting("Slot 6",false, description = "Toggle whether this slot should be swapped.")
    private val slot6 = BooleanSetting("Slot 7",false, description = "Toggle whether this slot should be swapped.")
    private val slot7 = BooleanSetting("Slot 8",false, description = "Toggle whether this slot should be swapped.")

    private val slots = arrayListOf<Setting>(
        slot0,
        slot1,
        slot2,
        slot3,
        slot4,
        slot5,
        slot6,
        slot7,
    )

    init{
        this.addSettings(slots)
    }

    override fun keyBind() {
        if (this.enabled) {

            // Swap slots
            val swapHotbar: (GuiInventory) -> Unit = { inventory ->
                if (mc.thePlayer.inventory.itemStack == null) {
                    for (i in 0 until slots.size) {
                        if ((slots[i] as BooleanSetting).enabled) {
                            val slot = (inventory as GuiContainer).inventorySlots.inventorySlots[27 + i] as Slot
                            val slotId = slot.slotNumber
                            mc.playerController.windowClick(
                                (inventory as GuiContainer).inventorySlots.windowId, slotId, i, 2, mc.thePlayer
                            )
                        }
                    }
                }
            }
            FakeInventoryActionManager.addAction(swapHotbar)
        }
    }
}