package floppaclient.utils.fakeactions

import floppaclient.FloppaClient.Companion.mc
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation

object FakeActionManager {

    private var originalYaw = 0f
    private var originalPitch = 0f
    private var originalSneakState = false

    /**
     * Indicates whether a fake action is already staged for this tick.
     * Use this and Subscribe event priority to avoid conflicts with different fake actions.
     */
    var doAction = false
        private set
        get() {
            return actionQueue.size > 0
        }

    var actionQueue: MutableList<FakeAction> = mutableListOf()

    /**
     * Stages a right click action with the specified slot and the given rotation for this tick.
     */
    fun stageRightClickSlot(
        yaw: Float,
        pitch: Float,
        itemSlot: Int,
        forceSneak: Boolean = false
    ): FakeAction {
        val newAction = FakeAction(
            yaw,
            pitch,
            true,
            itemSlot,
            forceSneak,
            false,
        )
        this.actionQueue = mutableListOf(newAction)
        return newAction
    }

    /**
     * Stages a right click action with the specified slot and the given rotation for this tick.
     */
    fun stageRightClickSlot(
        yaw: Double,
        pitch: Double,
        itemSlot: Int,
        forceSneak: Boolean = false
    ): FakeAction {
        return this.stageRightClickSlot(yaw.toFloat(), pitch.toFloat(), itemSlot, forceSneak)
    }

    /**
     * Stages a left click action with the specified slot and the given rotation for this tick.
     */
    fun stageLeftClickSlot(
        yaw: Float,
        pitch: Float,
        itemSlot: Int,
        swingItem: Boolean = true
    ): FakeAction {
        val newAction = FakeAction(
            yaw,
            pitch,
            false,
            itemSlot,
            false,
            swingItem,
        )
        this.actionQueue = mutableListOf(newAction)
        return newAction
    }

    /**
     * Stages a left click action with the specified slot and the given rotation for this tick.
     */
    fun stageLeftClickSlot(
        yaw: Double,
        pitch: Double,
        itemSlot: Int,
        swingItem: Boolean = true
    ): FakeAction {
        return this.stageLeftClickSlot(yaw.toFloat(), pitch.toFloat(), itemSlot, swingItem)
    }

    /**
     * Queues a right click action with the specified slot and the given rotation for the next availiable tick.
     */
    fun queueRightClickSlot(
        yaw: Float,
        pitch: Float,
        itemSlot: Int,
        forceSneak: Boolean = false
    ): FakeAction {
        val newAction = FakeAction(
            yaw,
            pitch,
            true,
            itemSlot,
            forceSneak,
            false,
        )
        this.actionQueue.add(newAction)
        return newAction
    }

    /**
     * Queues a left click action with the specified slot and the given rotation for the next availiable tick.
     */
    fun queueLeftClickSlot(
        yaw: Float,
        pitch: Float,
        itemSlot: Int,
        swingItem: Boolean = true
    ): FakeAction {
        val newAction = FakeAction(
            yaw,
            pitch,
            false,
            itemSlot,
            false,
            swingItem,
        )
        this.actionQueue.add(newAction)
        return newAction
    }

    /**
     * Gets run by the EntityPlayerSP mixin to perform the fake action.
     */
    fun interact() {
        actionQueue.firstOrNull()?.let {
            val previous = mc.thePlayer.inventory.currentItem
            mc.thePlayer.inventory.currentItem = it.itemSlot
            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(it.itemSlot))
            if (it.rightClick) {
                mc.thePlayer.sendQueue.addToSendQueue(
                    C08PacketPlayerBlockPlacement(
                        mc.thePlayer.inventory.getStackInSlot(
                            it.itemSlot
                        )
                    )
                )
            } else {
                if (it.swingItem) {
                    mc.thePlayer.swingItem()
                } else {
                    mc.thePlayer.sendQueue.addToSendQueue(C0APacketAnimation())
                }

            }
            mc.thePlayer.inventory.currentItem = previous
            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(previous))
            it.extraAction()
        }
    }

    fun fakeRotate() {
        storeState()
        actionQueue.firstOrNull()?.let {
            if (it.forceSneak) {
                mc.thePlayer.movementInput.sneak = true
            }
            mc.thePlayer.rotationYaw = it.fakeYaw
            mc.thePlayer.rotationPitch = it.fakePitch
        }
    }

    fun rotateBack() {
        actionQueue.firstOrNull()?.let {
            if (it.forceSneak) {
                mc.thePlayer.isSneaking = originalSneakState
            }
        }
        mc.thePlayer.rotationYaw = originalYaw
        mc.thePlayer.rotationPitch = originalPitch
    }

    private fun storeState() {
        originalSneakState = mc.thePlayer.isSneaking
        originalYaw = mc.thePlayer.rotationYaw
        originalPitch = mc.thePlayer.rotationPitch
    }

    /**
     * Removes the completed action from the queue
     */
    fun reset() {
        actionQueue.removeAt(0)
    }
}