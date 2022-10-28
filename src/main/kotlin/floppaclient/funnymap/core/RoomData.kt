package floppaclient.funnymap.core

data class RoomData(
    val name: String,
    val type: RoomType,
    val secrets: Int,
    val size: Int,
    val cores: List<Int>,
    val crypts: Int,
    val trappedChests: Int
)
