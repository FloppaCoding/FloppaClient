package floppaclient.floppamap.extras

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.currentRegionPair
import floppaclient.floppamap.dungeon.Dungeon.currentRoomPair
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.floppamap.utils.RoomUtils.getRoomExtrasData
import floppaclient.module.impl.render.ExtraBlocks
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.block.Block
import net.minecraft.block.BlockHopper
import net.minecraft.block.BlockStem
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object Extras {

    private val FACING_HORIZONTAL: PropertyDirection = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)
    private val FACING_OMNI: PropertyDirection = PropertyDirection.create("facing")
    private val FACING_UP: PropertyDirection = BlockStem.FACING
    private val FACING_DOWN: PropertyDirection = BlockHopper.FACING

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !ExtraBlocks.enabled ) return
        // Alternate run of the extras data. No more looping through all rooms. This also works for the boss room when
        // the dungeon is not scanned.
        (currentRoomPair ?: currentRegionPair)?.run roomPair@{
            val room = this.first
            val rotation = this.second

            getRoomExtrasData(room)?.run {
                this.preBlocks.forEach { (blockID, posList) ->
                    val blockstate = getStateFromIDWithRotation(blockID, rotation)

                    posList.forEach {
                        FloppaClient.mc.theWorld.setBlockState(
                            RoomUtils.getRealPos(it, this@roomPair),
                            blockstate
                        )
                    }
                }
            }
        }
    }

    /**
     * Returns the blockstate for the given blockID. Also rotates the state corresponding to the given rotation if it
     * has rotation data.
     */
    private fun getStateFromIDWithRotation(blockID: Int, rotation: Int) : IBlockState {
        val blockstate = Block.getStateById(blockID)
        return getStateFromIDWithRotation(blockstate, rotation)
    }

    /**
     * Returns the blockstate for the given blockID. Also rotates the state corresponding to the given rotation if it
     * has rotation data.
     */
    fun getStateFromIDWithRotation(iblockstate: IBlockState, rotation: Int) : IBlockState {
        var blockstate = iblockstate
        // rotate if block has rotation data. this is really scuffed unfortunately
        if (blockstate.properties.containsKey(FACING_HORIZONTAL)){
            val facing = blockstate.getValue(FACING_HORIZONTAL)
            if (facing.axis.isHorizontal) {
                blockstate = blockstate.withProperty(FACING_HORIZONTAL, getRotatedFacing(facing, rotation))
            }
        }
        else if (blockstate.properties.containsKey(FACING_OMNI)){
            val facing = blockstate.getValue(FACING_OMNI)
            if (facing.axis.isHorizontal) {
                blockstate = blockstate.withProperty(FACING_OMNI, getRotatedFacing(facing, rotation))
            }
        }
        else if (blockstate.properties.containsKey(FACING_DOWN)){
            val facing = blockstate.getValue(FACING_DOWN)
            if (facing.axis.isHorizontal) {
                blockstate = blockstate.withProperty(FACING_DOWN, getRotatedFacing(facing, rotation))
            }
        }
        else if (blockstate.properties.containsKey(FACING_UP)){
            val facing = blockstate.getValue(FACING_UP)
            if (facing.axis.isHorizontal) {
                blockstate = blockstate.withProperty(FACING_UP, getRotatedFacing(facing, rotation))
            }
        }
        return blockstate
    }

    private fun getRotatedFacing(facing: EnumFacing, rotation: Int): EnumFacing {
        return when {
            rotation.equalsOneOf(90, -270) -> facing.rotateY()
            rotation.equalsOneOf(180, -180) -> facing.rotateY().rotateY()
            rotation.equalsOneOf(270, -90) -> facing.rotateY().rotateY().rotateY()
            else -> facing
        }
    }
}