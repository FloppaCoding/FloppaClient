package floppaclient.floppamap.extras

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.core.Room
import floppaclient.floppamap.core.RoomType
import floppaclient.floppamap.core.Tile
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.dungeon.RunInformation
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import kotlin.math.min

/**
 * Scans the rooms rotations.
 *
 * @author Aton
 */
object ExtrasScan {

    // TODO make the rotations part of the room data?

    val rooms = mutableMapOf<Room, Int>()

    fun scanDungeon() {
        var allLoaded = true
        Dungeon.getDungeonTileList<Room>().forEach { room ->
            if (RoomUtils.roomList.any { it.cores[0] == room.core }) {

                // scan the room rotation
                if (!rooms.containsKey(room)) {
                    val rotation = getAbsoluteRoomRotation(room)
                    if (rotation != null) {
                        rooms[room] = rotation
                    }else allLoaded = false
                }
            }
        }

        // Also add the information for the boss room
        if (rooms.none { it.key.data.type == RoomType.BOSS }) {
            val floor = RunInformation.currentFloor?.floorNumber ?: return
            val bossRoom = RoomUtils.instanceBossRoom(floor)
            rooms[bossRoom] = 0
        }

        if (Dungeon.fullyScanned && allLoaded) {
            Dungeon.fullyScannedRotation = true
            modMessage("Finished Rotation scan.")
        }
    }

    /**
     * Returns the rotation of the given room based on the structure on top of the room.
     * It looks for a lapis Block in the corner of the roof of the room. If none is found it looks for the blue clay structure.
     * Rotations:
     * When the block is in the south east corner (15,15) -> 0 rotation.
     * When the block is in the south west corner (-15,15) -> 90 rotation.
     * When the block is in the north west corner (-15,-15) -> 180 rotation.
     * When the block is in the north east corner (15,-15) -> 270 rotation.
     */
    private fun getAbsoluteRoomRotation(room: Tile): Int? {
        if(room !is Room) return null

        // At least one of the entrance rooms can be shifted by one block so the detection fails
        // Custom detection here for entrance room
        if (room.data.type == RoomType.ENTRANCE) {
            listOf(
                0 to -7,
                7 to 0,
                0 to 7,
                -7 to 0
            ).withIndex().forEach { (index, pair) ->
                if (mc.theWorld.getBlockState(BlockPos(room.x + pair.first, 70 , room.z + pair.second)).block == Blocks.air) {
                    return index * 90
                }
            }

        }


        val cores = RoomUtils.roomList.find { it.cores.contains( room.core ) }?.cores ?: return null
        val tiles = Dungeon.getDungeonTileList<Room>().filter { !it.isSeparator && cores.contains(it.core) }
        // Check if the room is fully scanned already
        if (room.data.size != tiles.size) return null
        val corners = listOf(
            15 to 15,
            -15 to 15,
            -15 to -15,
            15 to -15
        )
        val roofY = getRoomRoofY(room)

        // First scan for the lapis Block
        for (tile in tiles) {
            corners.withIndex().forEach { (index, pair) ->
                if (!mc.theWorld.getChunkFromChunkCoords((tile.x + pair.first) shr 4, (tile.z + pair.second) shr 4).isLoaded) return null
                if (mc.theWorld.getBlockState(BlockPos(tile.x + pair.first, roofY, tile.z + pair.second)).block == Blocks.lapis_block ) {
                    return index * 90
                }
            }
        }

        // If no lapis Block was found scan for the blue clay structure which overrides it.
        // The color does not have to be checked, since the corners are either blue clay, redstone block or lapis block
        for (tile in tiles) {
            corners.withIndex().forEach { (index, pair) ->
                val pos = BlockPos(tile.x + pair.first, roofY, tile.z + pair.second)
                if (mc.theWorld.getBlockState(pos).block == Blocks.stained_hardened_clay ) {
                    // Here an additional check is needed to confirm that it is indeed the corner of the room. for 1x4 rooms this can be reacked without it being the room corner.
                    if (  (mc.theWorld.getBlockState(pos.south()).block == Blocks.stained_hardened_clay
                        || mc.theWorld.getBlockState(pos.north()).block == Blocks.stained_hardened_clay)
                        && (mc.theWorld.getBlockState(pos.west()).block == Blocks.stained_hardened_clay
                         || mc.theWorld.getBlockState(pos.east()).block == Blocks.stained_hardened_clay)
                    ){
                        return index * 90
                    }
                }
            }
        }

        return null
    }

    private fun getRoomRoofY(room: Tile): Int {
        val chunk = mc.theWorld.getChunkFromChunkCoords(room.x shr 4, room.z shr 4)
        // Here two checks are needed because at least of one room (Gold) there is a block on the roof in the center.
        val height1 = chunk.getHeightValue(room.x and 15, room.z and 15)
        val heihgt2 = chunk.getHeightValue(room.x+1 and 15, room.z and 15)
        return min(height1, heihgt2) -1
    }
}
