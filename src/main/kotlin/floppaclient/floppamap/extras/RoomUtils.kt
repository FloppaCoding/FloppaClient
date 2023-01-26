package floppaclient.floppamap.extras

import floppaclient.FloppaClient
import floppaclient.floppamap.core.*
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.util.BlockPos
// TODO doc comments and move to utils dir
object RoomUtils {
    /**
     * Rotates the given blockPos inside of a room with rotation to rotation 0.
     */
    fun getRotatedPos(blockPos: BlockPos, rotation: Int): BlockPos {
        return when {
            rotation.equalsOneOf(90, -270) -> BlockPos(-blockPos.z, blockPos.y, blockPos.x)
            rotation.equalsOneOf(180, -180) -> BlockPos(-blockPos.x, blockPos.y, -blockPos.z)
            rotation.equalsOneOf(270, -90) -> BlockPos(blockPos.z, blockPos.y, -blockPos.x)
            else -> blockPos
        }
    }

    /**
     * Rotates all blockPos in the given set inside of a room with rotation to rotation 0.
     */
    fun getRotatedPosSet(posSet: MutableSet<BlockPos>, rotation: Int): MutableSet<BlockPos> {
        val returnSet: MutableSet<BlockPos> = mutableSetOf()
        posSet.forEach {
            returnSet.add(getRotatedPos(it, rotation))
        }
        return returnSet
    }

    /**
     * Translates the given blockpos to the corresponding relative position in the room.
     */
    fun getRelativePos(blockPos: BlockPos, roomPair: Pair<Room, Int>): BlockPos {
        return getRotatedPos(blockPos.add(-roomPair.first.x, 0, -roomPair.first.z), -roomPair.second)
    }

    /**
     * Translates the given room relative coordinates to real coordinates.
     */
    fun getRealPos(blockPos: BlockPos, roomPair: Pair<Room, Int>): BlockPos {
        return getRotatedPos(blockPos, roomPair.second).add(roomPair.first.x,0,roomPair.first.z)
    }

    /**
     * Gets the extras data for the specified room (this can also be a region).
     * If none exits it will create a blank entry.
     * @see getRoomExtrasData
     * @return null when [room].[core][Room.core] is null.
     */
    fun getOrPutRoomExtrasData(room: Room): ExtrasData? {
        if (room.core == null) return null
        return if (room.data.type == RoomType.REGION)
            FloppaClient.extras.extraRegions.getOrPut(room.data.name) {
                ExtrasData(room.core!!)
            }
        else
            FloppaClient.extras.extraRooms.getOrPut(room.data.name) {
                ExtrasData(room.core!!)
            }
    }

    /**
     * Gets the extras data for the specified room (this can also be a region).
     *
     * If none exits it will return null.
     * @see getOrPutRoomExtrasData
     */
    fun getRoomExtrasData(room: Room): ExtrasData? {
        return if (room.data.type == RoomType.REGION)
            FloppaClient.extras.extraRegions[room.data.name]
        else
            FloppaClient.extras.extraRooms[room.data.name]
    }

    /**
     * Gets the auto action data for the specified room (this can also be a region).
     * If none exits it will create a blank entry.
     * @see getRoomAutoActionData
     * @return null when [room].[core][Room.core] is null.
     */
    fun getOrPutRoomAutoActionData(room: Room): AutoActionData? {
        if(room.core == null) return null
        return if (room.data.type == RoomType.REGION)
            FloppaClient.autoactions.autoActionRegions.getOrPut(room.data.name) {
                AutoActionData(room.core!!)
            }
        else
            FloppaClient.autoactions.autoActionRooms.getOrPut(room.data.name) {
                AutoActionData(room.core!!)
            }
    }

    /**
     * Gets the auto action data for the specified room (this can also be a region).
     *
     * If none exits it will return null.
     * @see getOrPutRoomAutoActionData
     */
    fun getRoomAutoActionData(room: Room): AutoActionData? {
        return if (room.data.type == RoomType.REGION)
            FloppaClient.autoactions.autoActionRegions[room.data.name]
        else
            FloppaClient.autoactions.autoActionRooms[room.data.name]
    }

    fun instanceBossRoom(floor: Int): Room {
        return Room(0,0, RoomData("Boss $floor", RoomType.BOSS))
    }

    fun instanceRegionRoom(region: String): Room {
        return Room(0,0, RoomData(region, RoomType.REGION))
    }
}