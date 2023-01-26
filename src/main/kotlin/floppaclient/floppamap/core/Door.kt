package floppaclient.floppamap.core

import floppaclient.module.impl.render.MapRooms
import java.awt.Color

class Door(x: Int, z: Int, val type: DoorType) : Tile(x, z) {
    var opened = false

    override val color: Color
        get() = when (this.type) {
            DoorType.BLOOD -> MapRooms.colorBloodDoor.value
            DoorType.ENTRANCE -> MapRooms.colorEntranceDoor.value
            DoorType.WITHER -> if (opened)  MapRooms.colorOpenWitherDoor.value else MapRooms.colorWitherDoor.value
            else -> MapRooms.colorRoomDoor.value
        }
}
