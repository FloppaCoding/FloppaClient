package floppaclient.floppamap.core

/**
 * This class is meant to offer a nice way to store dungeon room data, that can but does not have to be backed up by
 * [RoomConfigData] from the dungeon scan.
 *  @author Aton
 */
class RoomData(
    name: String = "Unknown",
    type: RoomType = RoomType.UNKNOWN,
) {
    constructor(configData: RoomConfigData) : this() {
        this.configData= configData
    }

    var hasMimic = false

    var configData: RoomConfigData? = null

    // First the values from config.
    var name: String = name
        get() = configData?.name ?: field
    var type: RoomType = type
        get() = configData?.type ?: field
    var maxSecrets: Int? = null
        get() = configData?.secrets ?: field
    var size: Int? = null
        get() = configData?.size ?: field
    val cores: List<Int>?
        get() = configData?.cores
    val crypts: Int?
        get() = configData?.crypts
    val trappedChests: Int?
        get() = configData?.trappedChests

    // room specific variables.
    var currentSecrets = 0

}
