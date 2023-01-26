package floppaclient.floppamap.core

/**
 * Data for rooms retrieved from rooms.json.
 *
 * Not to be confused with [RoomData] which contains all data for the room.
 */
data class RoomConfigData(
    val name: String,
    val type: RoomType,
    val secrets: Int,
    val size: Int,
    val cores: List<Int>,
    val crypts: Int,
    val trappedChests: Int
)
