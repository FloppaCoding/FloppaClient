package floppaclient.floppamap.dungeon

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.core.Room
import floppaclient.floppamap.core.RoomData
import floppaclient.module.impl.render.DungeonMap
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.BlockPos

/**
 * Code for finding the room which contains the Mimic.
 *
 * The mimic is detected by counting the trapped chests that fall within a room and comparing that number against the expected number.
 * If there are more chests present, then one has to be the Mimic.
 *
 * Based on [FunnyMap by Harry282](https://github.com/Harry282/FunnyMap/blob/master/src/main/kotlin/funnymap/features/dungeon/MimicDetector.kt).
 * @author Aton
 */
object MimicDetector {
    /**
     * Scan the Dungeon for the mimic trapped chest.
     *
     * When it is found [Dungeon.mimicFound] is set to true and the [hasMimic][RoomData.hasMimic] field for the
     * corresponding room is also set to true.
     */
    fun findMimic() {
        val mimicRoomData = findMimicRoom()
        if (mimicRoomData != null) {
            mimicRoomData.hasMimic = true
            if(DungeonMap.mimicInfo.enabled && !DungeonMap.legitMode.enabled) modMessage("Mimic found in ${mimicRoomData.name}")
            Dungeon.mimicFound = true
        }
    }

    /**
     * Counts the trapped chests in all rooms and compares that number against the expected value.
     * If there are more trapped chests found than expected one of them has to be the mimic.
     * @return the room data for the room the mimic was found in or null if no mimic was found.
     */
    private fun findMimicRoom(): RoomData? {
        mc.theWorld.loadedTileEntityList.filter { it is TileEntityChest && it.chestType == 1 }
            .mapNotNull { getRoomFromPos(it.pos) }.groupingBy { it.data }.eachCount()
            .forEach { (roomData, trappedChestCount) ->
                if(roomData.trappedChests != null && roomData.trappedChests!! < trappedChestCount) {
                    return roomData
                }
            }
        return null
    }

    /**
     * Returns the Room which contains [pos]. returns null if the room has not been scanned yet or [pos] is outside the dungeon.
     */
    private fun getRoomFromPos(pos: BlockPos): Room? {
        // The following operation is a division of the coordinates by the chunk size of 16 rounded down to the next even number.
        // This bins all coordinates into a region of 32 blocks, which is the size of a room.
        val column = ((pos.x - Dungeon.startX + 15) shr 4) and 0b1110
        val row = ((pos.z - Dungeon.startZ + 15) shr 4) and 0b1110
        return Dungeon.getDungeonTile<Room>(column, row)
    }
}
