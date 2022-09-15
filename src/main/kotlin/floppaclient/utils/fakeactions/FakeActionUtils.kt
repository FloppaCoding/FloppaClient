package floppaclient.utils.fakeactions

import floppaclient.FloppaClient.Companion.mc
import floppaclient.mixins.packet.C02Accessor
import floppaclient.utils.Utils
import net.minecraft.entity.Entity
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
     * Returns true when the block is clicked, and false when it is out of range and the click is aborted.
     */
    fun clickBlockWithItem(blockPos: BlockPos, slot: Int? = null, name: String = "", range: Int = 10): Boolean {
        val previous = mc.thePlayer.inventory.currentItem
        val itemSlot = when (name) {
            "" -> slot
            else -> {
                Utils.findItem(name)
            }
        } ?: previous

        mc.thePlayer.inventory.currentItem = itemSlot
        val inRange = clickBlock(blockPos, range)
        mc.thePlayer.inventory.currentItem = previous
        return inRange
    }

    /**
     * Interacts with the block at the gives blockpos with the currently held item.
     * performs a check whether that block is in the specified range first.
     * Returns true when the block is clicked, and false when it is out of range and the click is aborted.
     */
    fun clickBlock(blockPos: BlockPos, range: Int = 10): Boolean {
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
    fun useItem(itemSlot: Int, swapBack: Boolean = true) {
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

    /**
     * Attempts to swap to and the item with the specified name.
     * Returns true if successful.
     */
    fun useItem(name: String, swapBack: Boolean = true): Boolean {
        val itemSlot = Utils.findItem(name) ?: return false
        this.useItem(itemSlot, swapBack)
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
}