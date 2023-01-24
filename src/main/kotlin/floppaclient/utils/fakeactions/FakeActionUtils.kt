package floppaclient.utils.fakeactions

import floppaclient.FloppaClient.Companion.mc
import floppaclient.mixins.packet.C02Accessor
import floppaclient.module.impl.player.AutoEther
import floppaclient.utils.ChatUtils
import floppaclient.utils.GeometryUtils
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
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import java.util.*
import kotlin.concurrent.schedule

/**
 * Collection of functions for performing fake player interactions.
 *
 * @author Aton
 */
object FakeActionUtils {


    /**
     * Stages an etherwarp fake action to the specified block pos.
     * Returns false as well as sends a chat message if etherwarp not possible.
     * Checks the center of all 6 sides of the block for visibility.
     * @param targetPos the Target
     * @param message if true sends a chat message on fail.
     * @param fakeTp will set the position client side to the etherwarp position after sending the click packet.
     */
    fun etherwarpTo(targetPos: BlockPos, message: Boolean = false, fakeTp: Boolean = false): Boolean {
        val aotvSlot = Utils.findItem("Aspect of the Void") ?: run {
            if (message) ChatUtils.modMessage("No AOTV found in your hotbar!")
            return false
        }


        val distance = mc.thePlayer.getDistanceSq(targetPos)
        val dist = 61.0

        if (distance > (dist + 2) * (dist + 2)) {
            if (message) ChatUtils.modMessage("Target is to far away")
            return false
        }

        /** Account for shifted eye height. The default eye height is 1.62, shifted is -0.08 less */
        val sneakOffs = if (mc.thePlayer.isSneaking) {
            0.0
        } else {
            -0.08
        }

        // check whether the block can be seen or is to far away
        val targets = listOf(
            Vec3(targetPos).add(Vec3(0.5, 1.0, 0.5)),
            Vec3(targetPos).add(Vec3(0.0, 0.5, 0.5)),
            Vec3(targetPos).add(Vec3(0.5, 0.5, 0.0)),
            Vec3(targetPos).add(Vec3(1.0, 0.5, 0.5)),
            Vec3(targetPos).add(Vec3(0.5, 0.5, 1.0)),
            Vec3(targetPos).add(Vec3(0.5, 0.0, 0.5)),
        )

        var target: Vec3? = null
        for (targetVec in targets) {
            val eyeVec = mc.thePlayer.getPositionEyes(1f).addVector(0.0, sneakOffs, 0.0)

            val dirVec = targetVec.subtract(eyeVec).normalize()

            val vec32 = eyeVec.addVector(dirVec.xCoord * dist, dirVec.yCoord * dist, dirVec.zCoord * dist)
            val obj = mc.theWorld.rayTraceBlocks(eyeVec, vec32, true, false, true) ?: run {
                if (message) ChatUtils.modMessage("Target can not be found.")
                return false
            }
            if (obj.blockPos == targetPos) {
                target = targetVec
                break
            }
        }

        if (target == null) {
            if (message) ChatUtils.modMessage("Target can not be seen!")
            return false
        }

        if (FakeActionManager.doAction) {
            if (message) ChatUtils.modMessage("Conflicting fake action already staged.")
            return false
        }

        val direction = GeometryUtils.getDirection(
            mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight - 0.08, mc.thePlayer.posZ,
            target.xCoord, target.yCoord, target.zCoord
        )

        FakeActionManager.stageRightClickSlot(direction[1].toFloat(), direction[2].toFloat(), aotvSlot, true).apply {
            if (fakeTp){
                val newX = targetPos.x + 0.5
                val newY = targetPos.y + 1.05
                val newZ = targetPos.z + 0.5
                this.extraActionFun = {
                    mc.thePlayer.setPosition(newX, newY, newZ)
                    Timer().schedule(10) {
                        AutoEther.fakeTPResponse(newX, newY, newZ, direction[1].toFloat() % 360f, direction[2].toFloat() % 360f)
                    }
                }
            }
        }
        return true
    }


    /**
     * Stages an etherwarp fake action to the specified block pos for the given start vec which is assumed to be the
     * players coordinates (at feet level).
     * Returns false as well as sends a chat message if etherwarp not possible.
     * Checks the center of all 6 sides of the block for visibility.
     * @param start the start position.
     * @param targetPos the Target
     * @param message if true sends a chat message on fail.
     * @param fakeTp will set the position client side to the etherwarp position after sending the click packet.
     */
    fun tryEtherwarp(
        start: Vec3,
        targetPos: BlockPos,
        message: Boolean = false,
        queueMode: Boolean = false,
        fakeTp: Boolean = false
    ): Boolean {
        val aotvSlot = Utils.findItem("Aspect of the Void") ?: run {
            if (message) ChatUtils.modMessage("No AOTV found in your hotbar!")
            return false
        }
        val eyeVec = start.addVector(0.0, mc.thePlayer.eyeHeight - 0.08, 0.0)


//        val distance = mc.thePlayer.getDistanceSq(targetPos)
        val distance = eyeVec.distanceTo(Vec3(targetPos))
        val dist = 61.0

        if (distance > (dist + 2)) {
            if (message) ChatUtils.modMessage("Target is to far away")
            return false
        }


        // check whether the block can be seen or is to far away
        val targets = listOf(
            Vec3(targetPos).add(Vec3(0.5, 1.0, 0.5)),
            Vec3(targetPos).add(Vec3(0.0, 0.5, 0.5)),
            Vec3(targetPos).add(Vec3(0.5, 0.5, 0.0)),
            Vec3(targetPos).add(Vec3(1.0, 0.5, 0.5)),
            Vec3(targetPos).add(Vec3(0.5, 0.5, 1.0)),
            Vec3(targetPos).add(Vec3(0.5, 0.0, 0.5)),
        )

        var target: Vec3? = null
        for (targetVec in targets) {

            val dirVec = targetVec.subtract(eyeVec).normalize()

            val vec32 = eyeVec.addVector(dirVec.xCoord * dist, dirVec.yCoord * dist, dirVec.zCoord * dist)
            val obj = mc.theWorld.rayTraceBlocks(eyeVec, vec32, true, false, true) ?: run {
                if (message) ChatUtils.modMessage("Target can not be found.")
                return false
            }
            if (obj.blockPos == targetPos) {
                target = targetVec
                break
            }
        }

        if (target == null) {
            if (message) ChatUtils.modMessage("Target can not be seen!")
            return false
        }

        val direction = GeometryUtils.getDirection(
            start.xCoord, start.yCoord + mc.thePlayer.eyeHeight - 0.08, start.zCoord,
            target.xCoord, target.yCoord, target.zCoord
        )

        val fakeAction = if (queueMode)
            FakeActionManager.queueRightClickSlot(direction[1].toFloat(), direction[2].toFloat(), aotvSlot, true)
        else {
            if (FakeActionManager.doAction) {
                if (message) ChatUtils.modMessage("Conflicting fake action already staged.")
                return false
            }
            FakeActionManager.stageRightClickSlot(direction[1].toFloat(), direction[2].toFloat(), aotvSlot, true)
        }
        fakeAction.apply {
            if (fakeTp){
                val newX = targetPos.x + 0.5
                val newY = targetPos.y + 1.05
                val newZ = targetPos.z + 0.5
                this.extraActionFun = {
                    mc.thePlayer.setPosition(newX, newY, newZ)
                    Timer().schedule(10) {
                        AutoEther.fakeTPResponse(newX, newY, newZ, direction[1].toFloat() % 360f, direction[2].toFloat() % 360f)
                    }
                }
            }
        }
        return true
    }

    /**
     * Stages an etherwarp fake action to the specified block pos for the given start vec which is assumed to be the
     * players coordinates (at feet level).
     * The top center of the block will be targeted.
     * It will not check whether the target can be seen.
     * @return false when no aotv found in hotbar
     */
    fun forceEtherwarp(
        start: Vec3,
        targetPos: BlockPos,
        message: Boolean = false,
        queueMode: Boolean = false,
        fakeTp: Boolean = false
    ): Boolean {
        val aotvSlot = Utils.findItem("Aspect of the Void") ?: run {
            if (message) ChatUtils.modMessage("No AOTV found in your hotbar!")
            return false
        }
        val target = Vec3(targetPos).add(Vec3(0.5, 1.0, 0.5))

        val direction = GeometryUtils.getDirection(
            start.xCoord, start.yCoord + mc.thePlayer.eyeHeight - 0.08, start.zCoord,
            target.xCoord, target.yCoord, target.zCoord
        )
        val fakeAction = if (queueMode)
            FakeActionManager.queueRightClickSlot(direction[1].toFloat(), direction[2].toFloat(), aotvSlot, true)
        else
            FakeActionManager.stageRightClickSlot(direction[1].toFloat(), direction[2].toFloat(), aotvSlot, true)
        fakeAction.apply {
            if (fakeTp){
                val newX = targetPos.x + 0.5
                val newY = targetPos.y + 1.05
                val newZ = targetPos.z + 0.5
                this.extraActionFun = {
                    mc.thePlayer.setPosition(newX, newY, newZ)
                    Timer().schedule(10) {
                        AutoEther.fakeTPResponse(newX, newY, newZ, direction[1].toFloat() % 360f, direction[2].toFloat() % 360f)
                    }
                }
            }
        }
        return true
    }

    /**
     * Interacts with the entity with the given id by sending the interaction package.
     */
    fun interactWithEntity(entityId: Int) {
        val packet = C02PacketUseEntity()
        @Suppress("KotlinConstantConditions")
        (packet as C02Accessor).setEntityId(entityId)
        (packet as C02Accessor).setAction(C02PacketUseEntity.Action.INTERACT)
        mc.netHandler.networkManager.sendPacket(packet as Packet<*>)
    }

    /**
     * Interacts with the given entity by sending the interaction package.
     */
    fun interactWithEntity(entity: Entity) {
        this.interactWithEntity(entity.entityId)
    }

    /**
     * Do not use for armorstands.
     */
    fun legitClickEntity(entity: Entity) {
        val vec3 = mc.thePlayer.getPositionEyes(1f)
        val vec32 = entity.positionVector

        val f1 = entity.collisionBorderSize
        val axisalignedbb = entity.entityBoundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble())
        val movingObject = axisalignedbb.calculateIntercept(vec3, vec32) ?: MovingObjectPosition(vec32, EnumFacing.EAST)

        val vec3new = Vec3(
            movingObject.hitVec.xCoord - entity.posX,
            movingObject.hitVec.yCoord - entity.posY,
            movingObject.hitVec.zCoord - entity.posZ
        )
        mc.netHandler.networkManager.sendPacket(C02PacketUseEntity(entity, vec3new))
        interactWithEntity(entity)
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