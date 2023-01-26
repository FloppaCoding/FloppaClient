package floppaclient.floppamap.core

import floppaclient.floppamap.dungeon.Dungeon
import java.awt.Color

abstract class Tile(val x: Int, val z: Int) {
    var state = RoomState.UNDISCOVERED
    var visited = false
    var scanned = true
    abstract val color: Color

    /**
     * Row in the duneonList.
     */
    val row
        get() = (z - Dungeon.startZ) shr 4
    /**
     * Column in the dungeonList
     */
    val column
        get() = (x - Dungeon.startX) shr 4
}
