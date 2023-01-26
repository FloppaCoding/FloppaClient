package floppaclient.floppamap.core

import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.module.impl.render.MapRooms
import java.awt.Color

class Room(x: Int, z: Int, var data: RoomData) : Tile(x, z) {

    constructor(x: Int, z: Int, configData: RoomConfigData) : this(x, z, RoomData(configData = configData))

    /**
     * Core of this tile.
     */
    var core: Int? = null
    var isSeparator = false

    private val puzzleRegex = Regex("Higher|Blaze|Tic")

    /**
     * Marks this tile as "unique".
     * Unique is only relevant for rooms consisting of multiple tiles. All 1x1 rooms are unique by default.
     * For rooms with multiple tiles the first Tile in [Dungeon.dungeonList] will be the unique one.
     * This corresponds to the most west and north tile of a room. (west prioritized over north)
     * The unique tile is the one which has the checkmark on the map.
     */
    val isUnique: Boolean
        get() {
            return Dungeon.dungeonList.first { it is Room && !it.isSeparator && it.data === this.data } === this
        }

    val canHaveSecrets: Boolean
        get() {
            return if (this.state == RoomState.QUESTION_MARK) false
            else when(data.type) {
                RoomType.NORMAL, RoomType.RARE, RoomType.TRAP -> true
                RoomType.PUZZLE -> data.name.contains(puzzleRegex)
                else -> false
            }
        }

    override val color: Color
        get() = if (this.state == RoomState.QUESTION_MARK && !visited)
            MapRooms.colorUnexplored.value
        else when (data.type) {
            RoomType.UNKNOWN ->   MapRooms.colorUnexplored.value
            RoomType.BLOOD ->     MapRooms.colorBlood.value
            RoomType.CHAMPION ->  MapRooms.colorMiniboss.value
            RoomType.ENTRANCE ->  MapRooms.colorEntrance.value
            RoomType.FAIRY ->     MapRooms.colorFairy.value
            RoomType.PUZZLE ->    MapRooms.colorPuzzle.value
            RoomType.RARE ->      MapRooms.colorRare.value
            RoomType.TRAP ->      MapRooms.colorTrap.value
            else -> if (data.hasMimic) MapRooms.colorRoomMimic.value else MapRooms.colorRoom.value
        }
}
