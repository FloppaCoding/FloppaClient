package floppaclient.funnymap.core

import floppaclient.module.impl.render.MapRooms
import java.awt.Color

data class Room(override var x: Int, override var z: Int, var data: RoomData) : Tile(x, z) {

    var core = 0
    var hasMimic = false
    var isSeparator = false

    override val color: Color
        get() = when (data.type) {
            RoomType.BLOOD ->     MapRooms.colorBlood.value
            RoomType.CHAMPION ->  MapRooms.colorMiniboss.value
            RoomType.ENTRANCE ->  MapRooms.colorEntrance.value
            RoomType.FAIRY ->     MapRooms.colorFairy.value
            RoomType.PUZZLE ->    MapRooms.colorPuzzle.value
            RoomType.RARE ->      MapRooms.colorRare.value
            RoomType.TRAP ->      MapRooms.colorTrap.value
            else -> if (hasMimic) MapRooms.colorRoomMimic.value else MapRooms.colorRoom.value
        }
}
