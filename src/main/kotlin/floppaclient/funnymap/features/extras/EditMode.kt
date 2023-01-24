package floppaclient.funnymap.features.extras

import floppaclient.FloppaClient.Companion.currentRegionPair
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.funnymap.features.dungeon.Dungeon.currentRoomPair
import floppaclient.funnymap.features.extras.RoomUtils.getOrPutRoomExtrasData
import floppaclient.funnymap.features.extras.RoomUtils.getRelativePos
import floppaclient.utils.ChatUtils
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object EditMode {

    var enabled = false
    var currentBlockID = 95

    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (!enabled || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
        val roomPair = currentRoomPair ?: currentRegionPair ?: return
        val relativeCoords = getRelativePos(mc.objectMouseOver.blockPos, roomPair)

        getOrPutRoomExtrasData(roomPair.first).run {
            var removedBlock = false
            // this for each will remove all entries at those coordinates, there should only be one
            this.preBlocks.forEach { (blockID, _) ->
                if (this.preBlocks[blockID]?.remove(relativeCoords) == true) {
                    removedBlock = true
                }
            }
            if (!removedBlock) {
                this.preBlocks.getOrPut(0) {
                    mutableSetOf()
                }.add(relativeCoords)
            }
            event.isCanceled = true
            mc.theWorld.setBlockToAir(mc.objectMouseOver.blockPos)
        }
    }

    @SubscribeEvent
    fun onMiddleClick(event: ClickEvent.MiddleClickEvent) {
        if (!enabled || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
        val state = mc.theWorld.getBlockState(mc.objectMouseOver.blockPos)
        if (state.block == Blocks.air) return
        currentBlockID = Block.getStateId(state)
        ChatUtils.modMessage("Set block to: ${state.block.localizedName}")
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (!enabled || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
        val blockPos = mc.objectMouseOver.blockPos.add(mc.objectMouseOver.sideHit.directionVec)
        val roomPair = currentRoomPair ?: currentRegionPair ?: return
        val relativeCoords =
            getRelativePos(blockPos, roomPair)

        getOrPutRoomExtrasData(roomPair.first).run {
            if (this.preBlocks[0]?.remove(relativeCoords) != true) {
                var blockstate = adjustBlockState(blockPos, currentBlockID)
                mc.theWorld.setBlockState(blockPos, blockstate)

                // if block has rotation data rotate for config.
                // There are 4 different ways rotations are saved.
                blockstate = Extras.getStateFromIDWithRotation(blockstate, -roomPair.second)

                this.preBlocks.getOrPut(Block.getStateId(blockstate)) {
                    mutableSetOf()
                }.add(relativeCoords)
                event.isCanceled = true
            }
        }
    }

    private fun adjustBlockState(blockPos: BlockPos, blockID: Int): IBlockState {
        val hitVec = mc.objectMouseOver.hitVec
        val f = (hitVec.xCoord - blockPos.x.toDouble()).toFloat()
        val f1 = (hitVec.yCoord - blockPos.y.toDouble()).toFloat()
        val f2 = (hitVec.zCoord - blockPos.z.toDouble()).toFloat()
        return try {
            val block = Block.getBlockById(blockID)
            val meta = block.getMetaFromState(Block.getStateById(blockID))
            block.onBlockPlaced(
                mc.theWorld,
                blockPos,
                mc.objectMouseOver.sideHit,
                f, f1, f2, meta, mc.thePlayer
            )
        }catch (e: IllegalArgumentException) {
            Block.getStateById(blockID)
        }
    }
}
