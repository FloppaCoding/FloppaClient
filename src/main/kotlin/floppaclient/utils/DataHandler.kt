package floppaclient.utils

import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.util.Vec3

object DataHandler {
    /**
     * Returns a mutbale list of 3 Integers representing the players position in the current room.
     * BlockPos or Vec3i would probably be the better data format, but list was chosen, because it is easier to serialize.
     */
    fun getKey(vec: Vec3, roomX: Int, roomZ: Int, rotation: Int): MutableList<Int> {
        getRelativeCoords(vec, roomX, roomZ, rotation).run {
            return mutableListOf(this.xCoord.toInt(), this.yCoord.toInt(), this.zCoord.toInt())
        }
    }

    /**
     * Gets the relative player position in a room
     */
    private fun getRelativeCoords(vec: Vec3, roomX: Int, roomZ: Int, rotation: Int): Vec3 {
        return getRotatedCoords(vec.subtract(roomX.toDouble(), 0.0, roomZ.toDouble()), -rotation)
    }

    /**
     * Returns the real rotations of the given vec in a room with given rotation.
     * To get the relative rotation inside a room use 360 - rotation.
     */
    private fun getRotatedCoords(vec: Vec3, rotation: Int): Vec3 {
        return when {
            rotation.equalsOneOf(90, -270) -> Vec3(-vec.zCoord, vec.yCoord, vec.xCoord)
            rotation.equalsOneOf(180, -180) -> Vec3(-vec.xCoord, vec.yCoord, -vec.zCoord)
            rotation.equalsOneOf(270, -90) -> Vec3(vec.zCoord, vec.yCoord, -vec.xCoord)
            else -> vec
        }
    }
}