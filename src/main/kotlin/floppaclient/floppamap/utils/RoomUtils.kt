package floppaclient.floppamap.utils

import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import floppaclient.FloppaClient
import floppaclient.floppamap.core.*
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation

/**
 * A collection of methods for dungeon room specific information.
 *
 * These include coordinate transformations and obtaining the correct data from config files.
 *
 * @author Aton
 */
object RoomUtils {
    val roomList: Set<RoomConfigData> = try {
        Gson().fromJson(
            FloppaClient.mc.resourceManager.getResource(ResourceLocation(FloppaClient.RESOURCE_DOMAIN, "floppamap/rooms.json"))
                .inputStream.bufferedReader(),
            object : TypeToken<Set<RoomConfigData>>() {}.type
        )
    } catch (e: JsonSyntaxException) {
        println("Error parsing FloppaMap room data.")
        setOf()
    } catch (e: JsonIOException) {
        println("Error reading FloppaMap room data.")
        setOf()
    }

    /**
     * Rotates the given [blockPos] for a room with [rotation] to rotation 0.
     *
     * [rotation] is the rotation of the room in degrees. It has to be a multiple of 90 within the range of -360 to +360.
     * For any other value [blockPos] is returned.
     *
     * If you have coordinates in the rooms coordinate system this function will rotate them into real world coordinates.
     * The coordinates will still be offset by the position of the rooms center afterwards.
     *
     * To rotate coordinates from absolute world coordinates into the coordinate system of the room use this method with
     * minus the rotation of the room as argument.
     *
     * @see getRealPos
     * @see getRelativePos
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
     * Rotates the coordinates within the given [posSet] for a room with [rotation] to rotation 0.
     *
     * @see getRotatedPos
     */
    fun getRotatedPosSet(posSet: MutableSet<BlockPos>, rotation: Int): MutableSet<BlockPos> {
        val returnSet: MutableSet<BlockPos> = mutableSetOf()
        posSet.forEach {
            returnSet.add(getRotatedPos(it, rotation))
        }
        return returnSet
    }

    /**
     * Transforms the given [blockPos] in absolute world coordinates to the corresponding room relative position for the
     * given room with given rotation.
     *
     * @see getRealPos
     */
    fun getRelativePos(blockPos: BlockPos, roomPair: Pair<Room, Int>): BlockPos {
        return getRotatedPos(blockPos.add(-roomPair.first.x, 0, -roomPair.first.z), -roomPair.second)
    }

    /**
     * Transforms the given room relative coordinates to absolute coordinates.
     *
     * @see getRelativePos
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
        return if (room.data.type == RoomType.REGION)
            FloppaClient.extras.extraRegions.getOrPut(room.data.name) {
                ExtrasData(room.core ?: 0)
            }
        else {
            if (room.core == null) return null
            FloppaClient.extras.extraRooms.getOrPut(room.data.name) {
                ExtrasData(room.core!!)
        }
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
        return if (room.data.type == RoomType.REGION)
            FloppaClient.autoactions.autoActionRegions.getOrPut(room.data.name) {
                AutoActionData(room.core ?: 0)
            }
        else {
            if (room.core == null) return null
            FloppaClient.autoactions.autoActionRooms.getOrPut(room.data.name) {
                AutoActionData(room.core!!)
            }
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

    /**
     * Fetches information about the Tile with the given [core] from the rooms resource rooms.json.
     */
    fun getRoomConfigData(core: Int): RoomConfigData? {
        return roomList.find { core in it.cores }
    }

    fun instanceBossRoom(floor: Int): Room {
        return Room(0,0, RoomData("Boss $floor", RoomType.BOSS))
    }

    fun instanceRegionRoom(region: String): Room {
        return Room(0,0, RoomData(region, RoomType.REGION))
    }
}