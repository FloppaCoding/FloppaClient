package floppaclient.funnymap.core

import floppaclient.module.impl.render.MapRooms
import java.awt.Color

data class Door(override var x: Int, override var z: Int) : Tile(x, z) {

    var type = DoorType.NONE
    var opened = false

    override val color: Color
        get() = when (this.type) {
            DoorType.BLOOD -> MapRooms.colorBloodDoor.value
            DoorType.ENTRANCE -> MapRooms.colorEntranceDoor.value
            DoorType.WITHER -> if (opened)  MapRooms.colorOpenWitherDoor.value else MapRooms.colorWitherDoor.value
            else -> MapRooms.colorRoomDoor.value
        }
}
