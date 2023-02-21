package floppaclient.floppamap.utils

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.dungeon.RunInformation
import net.minecraft.item.ItemMap
import net.minecraft.util.Vec4b
import net.minecraft.world.storage.MapData

object MapUtils {

    var startCorner = Pair(5, 5)

    /**
     * Rooms have size 18x18 pixels on the vanilla map in floors 1..3, and 16x16 obove that.
     */
    var roomSize = 16
    var calibrated = false
    var coordMultiplier = 20.0/32.0

    private val colorString: String?
        get() {
            val mapData = getMapData() ?: return null
            return  mapData.colors.map{it.toInt().toChar()}.joinToString("")
        }

    private val regex16 = Regex("${30.toChar()}{16}")
    private val regex18 = Regex("${30.toChar()}{18}")

    fun getMapData(): MapData? {
        val map = mc.thePlayer?.inventory?.getStackInSlot(8) ?: return null
        if (map.item !is ItemMap || !map.displayName.contains("Magical Map")) return null
        return (map.item as ItemMap).getMapData(map, mc.theWorld)
    }


    /**
     * Returns the room size of dungeon rooms in pixels. Based on the size of the start room in the map item.
     */
    fun getRoomSizeFromMap(): Int? {
        val mapColorString = colorString ?: return null

        return if(regex18.find(mapColorString) != null) 18
        else if(regex16.find(mapColorString) != null) 16
        else null
    }

    /**
     * Returns the Start corner of the dungeon. This requires the rooms size to be set correctly first.
     */
    fun getStartCornerFromMap(): Pair<Int, Int>? {

        if (RunInformation.currentFloor?.floorNumber == 1) return Pair(22, 11)

        val mapColorString = colorString ?: return null

        val greenCorner = regex18.find(mapColorString)?.range?.first
            ?: regex16.find(mapColorString)?.range?.first
            ?: return null

        // make sure to update that value before using this function
        val increment = roomSize + 4
        val greenX = greenCorner.mod( 128)
        val greenZ = greenCorner shr 7 // Division by 128

        val startX = greenX.mod(increment)
        val startZ = greenZ.mod(increment)
        return Pair(startX, startZ)
    }

    val Vec4b.mapX
        get() = (this.func_176112_b() + 128) shr 1

    val Vec4b.mapZ
        get() = (this.func_176113_c() + 128) shr 1

    val Vec4b.yaw
        get() = this.func_176111_d() * 22.5f
}
