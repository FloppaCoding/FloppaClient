package floppaclient.utils.fakeactions

import floppaclient.FloppaClient.Companion.mc
import floppaclient.mixins.packet.C02Accessor
import floppaclient.utils.Utils
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.Entity
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemSkull
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

/**
 * Collection of functions for performing fake player interactions.
 */
object FakeActionUtils {

    /**
     * Interacts with the entity with the given id by sending the interaction package.
     */
    fun clickEntity(entityId: Int) {
        val packet = C02PacketUseEntity()
        (packet as C02Accessor).setEntityId(entityId)
        (packet as C02Accessor).setAction(C02PacketUseEntity.Action.INTERACT)
        mc.netHandler.networkManager.sendPacket(packet as Packet<*>)
    }

    /**
     * Interacts with the given entity by sending the interaction package.
     */
    fun clickEntity(entity: Entity) {
        this.clickEntity(entity.entityId)
    }

    /**
     * Interacts with the block at the gives blockpos with the specified item or slot.
     * Attempts to use the specified item, if not found or specified the specified slot, if not specified the current held item.
     * Performs a check whether that block is in the specified range first.
     * @param fromInv will also look in the inventory for the item that should be clicked with.
     * @param abortIfNotFound Will abort the click attempt if the specified item can not be found in the inventory. Will return false.
     * @return true when the block is clicked, and false when it is out of range and the click is aborted.
     */
    fun clickBlockWithItem(blockPos: BlockPos, slot: Int? = null, name: String = "", range: Double = 10.0, fromInv: Boolean = false, abortIfNotFound: Boolean = false): Boolean {
        val previous = mc.thePlayer.inventory.currentItem
        val itemSlot = when (name) {
            "" -> slot
            else -> {
                Utils.findItem(name, inInv = fromInv) ?: if (abortIfNotFound) return false else slot
            }
        } ?: previous

        // Range check.
        if (mc.thePlayer.getDistance(
                blockPos.x.toDouble(),
                blockPos.y.toDouble() - mc.thePlayer.eyeHeight,
                blockPos.z.toDouble()
            ) >= range
        ) return false

        if (itemSlot < 9) {
            mc.thePlayer.inventory.currentItem = itemSlot
            clickBlock(blockPos, range)
            mc.thePlayer.inventory.currentItem = previous
        }
        else if (itemSlot < 36 && fromInv){
            val inventory = GuiInventory(mc.thePlayer)
            // return if on horse.
            if (mc.playerController.isRidingHorse) return false

            // Swap slots
            val clickBlockFromInv: (GuiInventory) -> Unit = {
                if (mc.thePlayer.inventory.itemStack == null) {
                    val swapSlot = (inventory as GuiContainer).inventorySlots.inventorySlots[itemSlot] as Slot
                    val slotId = swapSlot.slotIndex
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, slotId, mc.thePlayer.inventory.currentItem, 2, mc.thePlayer)
                    clickBlock(blockPos, range)
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, slotId, mc.thePlayer.inventory.currentItem, 2, mc.thePlayer)
                }

            }
            FakeInventoryActionManager.addAction(clickBlockFromInv)
        }
        return true
    }

    /**
     * Interacts with the block at the gives blockpos with the currently held item.
     * performs a check whether that block is in the specified range first.
     * Returns true when the block is clicked, and false when it is out of range and the click is aborted.
     */
    fun clickBlock(blockPos: BlockPos, range: Double = 10.0): Boolean {
        if (mc.thePlayer.getDistance(
                blockPos.x.toDouble(),
                blockPos.y.toDouble() - mc.thePlayer.eyeHeight,
                blockPos.z.toDouble()
            ) >= range
        )
            return false
        mc.playerController.onPlayerRightClick(
            mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(),
            blockPos, EnumFacing.fromAngle(mc.thePlayer.rotationYaw.toDouble()), Vec3(0.0, 0.0, 0.0)
        )
        return true
    }

    /**
     * Swaps to and uses the specified item slot.
     */
    fun useItem(itemSlot: Int, swapBack: Boolean = true, fromInv: Boolean = false): Boolean{
        if (itemSlot < 9) {
            val previous = mc.thePlayer.inventory.currentItem

            mc.thePlayer.inventory.currentItem = itemSlot
            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(itemSlot))
            mc.thePlayer.sendQueue.addToSendQueue(
                C08PacketPlayerBlockPlacement(
                    mc.thePlayer.inventory.getStackInSlot(
                        itemSlot
                    )
                )
            )
            if (swapBack) {
                mc.thePlayer.inventory.currentItem = previous
                mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(previous))
            }
        }
        else if (itemSlot < 36 && fromInv) {
            // return if on horse.
            if (mc.playerController.isRidingHorse) return false

            // Swap slots
            val useItemFromInv: (GuiInventory) -> Unit = { inventory ->
                if (mc.thePlayer.inventory.itemStack == null) {
                    val slot = (inventory as GuiContainer).inventorySlots.inventorySlots[itemSlot] as Slot
                    val slotId = slot.slotIndex
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, slotId, mc.thePlayer.inventory.currentItem, 2, mc.thePlayer)
                    mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, slotId, mc.thePlayer.inventory.currentItem, 2, mc.thePlayer)
                }
            }
            FakeInventoryActionManager.addAction(useItemFromInv)
        }
        return true
    }

    /**
     * Attempts to swap to and the item with the specified name.
     * Returns true if successful.
     */
    fun useItem(name: String, swapBack: Boolean = true, fromInv: Boolean = false, ignoreCase: Boolean = false): Boolean {
        val itemSlot = Utils.findItem(name, ignoreCase, fromInv) ?: return false
        this.useItem(itemSlot, swapBack, fromInv)
        return true
    }

    /**
     * Attempts to swap to and click the item containing given name in the hotbar.
     * The same action as on a manual click is performed. so this will prioritise to interact
     * with things instead of using the item ability.
     * returns true if successful, false otherwise.
     */
    fun clickItem(name: String, rightClick: Boolean = true, swapBack: Boolean = true): Boolean {
        val itemSlot = Utils.findItem(name) ?: return false
        val previous = mc.thePlayer.inventory.currentItem
        mc.thePlayer.inventory.currentItem = itemSlot
        if (rightClick) {
            Utils.rightClick()
        } else {
            Utils.leftClick()
        }
        if (swapBack) mc.thePlayer.inventory.currentItem = previous
        return true
    }

    /**
     * Swaps out the first item found that matches the given name with the corresponding worn armor piece.
     *
     * Armor swaps must only be performed when the inventory is open. It is checked whether the inventory is alr open, if not
     * the method will return null or open the inventory if specified through openInv.
     * @param blockedSlots 4 bits containing which armor pieces should not be swapped. The right most bit is the helmet.
     * If it is 1 the helmet will not be swapped.
     * @return The armor slot that swapped or null if nothing was done. 0 is Helmet, 1 is Chest plate, 2 is Leggings, 3 is Boots.
     */
    fun swapArmorItem(itemName: String, blockedSlots:Int = 0b0000, ignoreCase: Boolean = true): Int?{

        if (mc.playerController.isRidingHorse) {
            // return if on horse.
            return null
        }

        val inventory = GuiInventory(mc.thePlayer)

        // Perform the swap and all required checks.
        val swappedIndex = run swapSlots@{
            if (mc.thePlayer.inventory.itemStack != null) return@swapSlots null
            if (blockedSlots == 0b1111) return@swapSlots null
            if (itemName == "") return@swapSlots null
            val itemSlot = Utils.findItem(itemName, ignoreCase, true) ?: return@swapSlots null
            if (itemSlot > 35) return@swapSlots null

            // The indices for the inventory slots of the GuiInventory do not match the indices of InventoryPlayer.
            // If the item is in the hotbar the index has to be adapted.
            val fromHotbar = itemSlot < 9

            val inventorySlot = itemSlot + if (fromHotbar) 36 else 0

            val slot = (inventory as GuiContainer).inventorySlots.inventorySlots[inventorySlot] as Slot
            val slotId = slot.slotIndex
            val item = slot.stack?.item ?: return@swapSlots null

            // Crafting slots seem to be 0...4, armor 5..8, and the main inventory 9..44 starting top left when
            // it come to slot indices
            val armorIndex = if (item is ItemArmor){
                item.armorType
            } else if (item is ItemSkull) {
                0
            } else {
                return@swapSlots null
            }

            // Here is kept track of alr swapped pieces
            val swapNum = 0b1 shl armorIndex
            if (blockedSlots and swapNum > 0) return@swapSlots null

            val doArmorSwap: (GuiInventory) -> Unit = {
                if(fromHotbar) {
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, 5 + armorIndex, itemSlot, 2, mc.thePlayer)
                }else {
                    // swap the item through the hot bar. That way we don't have to deal with mouse release and other stuff possibly breaking it.
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, slotId, 0, 2, mc.thePlayer)
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, 5 + armorIndex, 0, 2, mc.thePlayer)
                    mc.playerController.windowClick((inventory as GuiContainer).inventorySlots.windowId, slotId, 0, 2, mc.thePlayer)
                }
            }
            FakeInventoryActionManager.addAction(doArmorSwap)
            return@swapSlots armorIndex
        }

        // close the inventory again
        return swappedIndex
    }
}