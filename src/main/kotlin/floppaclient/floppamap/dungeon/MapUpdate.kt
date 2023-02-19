package floppaclient.floppamap.dungeon

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.DungeonRoomStateChangeEvent
import floppaclient.floppamap.core.*
import floppaclient.floppamap.utils.MapUtils
import floppaclient.module.impl.render.DungeonMap
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.util.StringUtils
import net.minecraftforge.common.MinecraftForge

/**
 * This object provides a collection of methods to update the dungeon information from the map item in the
 * hotbar and tab list.
 *
 * Based on [FunnyMap by Harry282](https://github.com/Harry282/FunnyMap/blob/master/src/main/kotlin/funnymap/features/dungeon/MapUpdate.kt)
 * <p>
 *     Colors in the map item:
85:  grey        - Unknown / unexplored room with questionmark in center. (color of the room not the questionmark)
63:  brown;      - Normal room.
30:  green       - Entracne room. and green checks.
18:  red         - Blood room. and failed puzzle X and blood door.
74:  yellow      -  Mini room.
66:  purple      - Puzzle room.
82:  pink        - Fairy room.
0:   blank       - blank fully undiscovered.
119: black       - question marks and wither doors.
34:  white       - used for checkmarks
62:  orange      - trap
 * <p>
 *
 * @author Aton
 */
object MapUpdate {

    /**
     * Used to only update puzzle names when a new one was revealed.
     */
    private var unmappedPuzz = false

    fun calibrate() {
        MapUtils.roomSize = MapUtils.getRoomSizeFromMap() ?: return
        MapUtils.startCorner = MapUtils.getStartCornerFromMap() ?: return

        MapUtils.coordMultiplier = (MapUtils.roomSize + 4.0) / Dungeon.roomSize

        MapUtils.calibrated = true
    }

    fun preloadHeads() {
        val tabEntries = Dungeon.getDungeonTabList() ?: return
        for (i in listOf(5, 9, 13, 17, 1)) {
            // Accessing the skin locations to load in skin
            tabEntries[i].first.locationSkin
        }
    }

    /**
     * Adds missing Players to the [Dungeon.dungeonTeammates] list and updates the information.
     * Updates the dead status for the players.
     * Dispatches [DungeonPlayer.updatePlayerAndRoom] for the teammates to update themselves.
     */
    fun updatePlayers(tabEntries: List<Pair<NetworkPlayerInfo, String>>) {
        var iconNum = 0
        // Update teammate list and dead status
        for (i in listOf(5, 9, 13, 17, 1)) {
            val tabText = StringUtils.stripControlCodes(tabEntries[i].second).trim()
            val name = tabText.split(" ").getOrNull(1) ?: ""
            if (name == "") continue
            // if the player is not in the list add it
            var teammate = Dungeon.dungeonTeammates.find { it.name == name }
            if ((teammate == null) || teammate.fakeEntity) {
                val potPlayer = mc.theWorld.playerEntities.find { it.name == name }
                val fake = potPlayer == null
                (potPlayer ?: EntityOtherPlayerMP(mc.theWorld, tabEntries[i].first.gameProfile))
                    .let {
                        if (teammate == null){
                            Dungeon.dungeonTeammates.add(DungeonPlayer(it, name, fake))
                        }else{
                            teammate!!.player = it
                            teammate!!.name = name
                            teammate!!.fakeEntity = fake
                        }
                    }
            }

            teammate = Dungeon.dungeonTeammates.find { it.name == name } ?: continue
            teammate.dead = tabText.contains("(DEAD)")
            if (!teammate.dead) {
                teammate.icon = "icon-${iconNum}"
                iconNum++
            } else {
                teammate.icon = ""
            }
        }

        // Update the teammates position as well as the secrets for the room they are in.
        val decor = MapUtils.getMapData()?.mapDecorations
        Dungeon.dungeonTeammates.forEach { dungeonPlayer ->
            dungeonPlayer.updatePlayerAndRoom(decor)
        }
    }

    /**
     * Updates the dungeon info from the hotbar map item.
     * Dispatches the map item based dungeon scan if that is enabled.
     * Updates the room states and door states based on check marks and door color.
     */
    fun updateRooms() {
        if (!MapUtils.calibrated) return
        val mapColors = MapUtils.getMapData()?.colors ?: return
        if (mapColors[0].toInt() != 0) return
        /** Used to signal that connected rooms should be updated. */
        var shouldConnectRooms = false

        val startX = MapUtils.startCorner.first
        val startZ = MapUtils.startCorner.second
        val centerOffset = (MapUtils.roomSize shr 1)
        val increment = (MapUtils.roomSize shr 1) + 2

        val scanRooms = shouldScanMapItem()

        for (column in 0..10) {
            for (row in 0..10) {
                var tile = Dungeon.getDungeonTile(column, row)

                //If room unknown try to get it from the map item.
                if (scanRooms && (tile == null || (tile.state == RoomState.QUESTION_MARK && !tile.scanned))) {
                    getRoomFromMap(column, row, mapColors)?.let { newTile ->
                        Dungeon.setDungeonTile(column, row, newTile)

                        if (newTile is Room && newTile.data.type == RoomType.NORMAL) shouldConnectRooms = true

                        // Update the room size.
                        if ((newTile as? Room)?.isSeparator == false && (newTile as? Room)?.data?.type == RoomType.NORMAL) {
                            val size = Dungeon.getDungeonTileList<Room>().filter { temporaryTile ->
                                !temporaryTile.isSeparator && temporaryTile.data === newTile.data
                            }.size
                            newTile.data.size = size
                        }
                        // Set a flag when a puzzle was added to get that puzzles name from the tab list.
                        if ((newTile as? Room)?.data?.type == RoomType.PUZZLE)
                            unmappedPuzz = true
                    }
                }

                // Scan the room centers on the map for check marks.
                tile = Dungeon.getDungeonTile(column, row)
                if (tile != null) {
                    val centerX = startX + column * increment + centerOffset
                    val centerZ = startZ + row * increment + centerOffset
                    if (centerX >= 128 || centerZ >= 128) continue
                    val newState = when (mapColors[(centerZ shl 7) + centerX].toInt()) {
                        0 -> RoomState.UNDISCOVERED
                        85 -> if (tile is Door)
                            RoomState.DISCOVERED
                        else
                            RoomState.UNDISCOVERED // should not happen
                        119 -> if (tile is Room)
                            RoomState.QUESTION_MARK
                        else
                            RoomState.DISCOVERED // wither door
                        18 -> if (tile is Room) when (tile.data.type) {
                            RoomType.BLOOD -> RoomState.DISCOVERED
                            RoomType.PUZZLE -> RoomState.FAILED
                            else -> tile.state
                        } else RoomState.DISCOVERED
                        30 -> if (tile is Room) when (tile.data.type) {
                            RoomType.ENTRANCE -> RoomState.DISCOVERED
                            else -> RoomState.GREEN
                        } else tile.state
                        34 -> RoomState.CLEARED
                        else -> {
                            if (tile is Door)
                                tile.opened = true
                            RoomState.DISCOVERED
                        }
                    }
                    if (newState != tile.state) {
                        MinecraftForge.EVENT_BUS.post(DungeonRoomStateChangeEvent(tile, newState))
                        tile.state = newState
                    }
                }
            }
        }

        if (shouldConnectRooms) {
            synchConnectedRooms()
        }

        if (unmappedPuzz)
            updatePuzzleNames()
    }

    /**
     * Makes sure that all rooms within [Dungeon.dungeonList] which are connected have the same data.
     * Also updates the [isUnique][Room.isUnique] state.
     */
    fun synchConnectedRooms() {
        /** Buffer room data which was combined, to prevent any loss */
        val bufferedData: MutableMap<RoomData, MutableSet<RoomData>> = mutableMapOf()
        Dungeon.getDungeonTileList().withIndex().forEach { (index, tile) ->
            if (tile !is Room) return@forEach
            if (tile.data.type != RoomType.NORMAL) return@forEach
            if (tile.isSeparator) return@forEach
            val column = index / 11
            val row = index % 11

            // If the tile is a room check neighboring tiles for data in the order left, top
            val leftConnector = Dungeon.getDungeonTile(column-1, row) as? Room
            val topConnector = Dungeon.getDungeonTile(column, row-1) as? Room
            var finalData: RoomData? = null
            val bufferedDataTemporary: MutableSet<RoomData> = mutableSetOf()
            var gotDataFromLeft = false

            // link the tile and connector to the correct data
            // this code could be compacted into a loop to reduce redundancy, but this form has better readability
            if (leftConnector?.isSeparator == true) {
                val leftRoom = Dungeon.getDungeonTile(column-2, row) as? Room
                if (leftRoom != null) {
                    gotDataFromLeft = true
                    finalData = leftRoom.data
                    bufferedDataTemporary.add(tile.data)
                    leftConnector.data = leftRoom.data
                    tile.data = leftRoom.data
                }
            }
            if (topConnector?.isSeparator == true) {
                val topRoom = Dungeon.getDungeonTile(column, row-2) as? Room
                if (topRoom != null) {
                    if (gotDataFromLeft) {
                        bufferedDataTemporary.add(topRoom.data)
                        topRoom.data = tile.data
                        topConnector.data = tile.data
                    }else {
                        finalData = topRoom.data
                        bufferedDataTemporary.add(tile.data)
                        topConnector.data = topRoom.data
                        tile.data = topRoom.data
                    }
                }
            }

            if (finalData != null) {
                bufferedData.getOrPut(finalData) { mutableSetOf() }.addAll(bufferedDataTemporary)
            }
        }

        // merge all the buffered Data together.
        bufferedData.forEach { (finalData, bufferedSet) ->
            //First make sure that no data is being merged with itself.
            bufferedSet.remove(finalData)
            bufferedSet.forEach {
                if (finalData.maxSecrets == null) finalData.maxSecrets = it.maxSecrets
                finalData.currentSecrets += it.currentSecrets
            }
        }

        // Update the isUnique state. This works by checking whether a tile is the first in the list.
        // This assumes that groupBy preserves the initial order of the rooms, which it should do.
        Dungeon.getDungeonTileList<Room>().filter { !it.isSeparator }.groupBy { it.data }.forEach{ (_, rooms) ->
            rooms.withIndex().forEach { (index, room) ->
                room.isUnique = index == 0
            }
        }
    }

    private fun shouldScanMapItem() =
        DungeonMap.mapItemScan.enabled && !Dungeon.fullyScanned && !Dungeon.inBoss && RunInformation.currentFloor != null

    /**
     * Updates the names of revealed puzzles from the tab list.
     */
    private fun updatePuzzleNames() {
        val puzzles = Dungeon.getDungeonTileList<Room>()
            .filter { room -> !room.isSeparator && room.data.type == RoomType.PUZZLE && room.state.revealed  }
            .sortedBy { room -> room.column*11 + room.row } // This is probably redundant since this is already the sort order of dungeonList
        if (RunInformation.puzzles.size == puzzles.size) {
            RunInformation.puzzles.withIndex().forEach { (index, puzzlePair) -> puzzles[index].data.name = puzzlePair.first }
            unmappedPuzz = false
        }
    }

    /**
     * Gets a dungeon tile from the map item.
     */
    private fun getRoomFromMap(column: Int, row: Int, mapColors: ByteArray): Tile? {

        val startX = MapUtils.startCorner.first
        val startZ = MapUtils.startCorner.second
        val increment = (MapUtils.roomSize shr 1) + 2
        val centerOffset = (MapUtils.roomSize shr 1)

        val cornerX = startX + column * increment
        val cornerZ = startZ + row * increment

        val centerX = cornerX + centerOffset
        val centerZ = cornerZ + centerOffset

        if (cornerX >= 128 || cornerZ >= 128) return null
        if (centerX >= 128 || centerZ >= 128) return null

        val xPos = Dungeon.startX + column * (Dungeon.roomSize shr 1)
        val zPos = Dungeon.startZ + row * (Dungeon.roomSize shr 1)

        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            rowEven && columnEven -> { // room
                val roomType = when (mapColors[(cornerZ shl 7) + cornerX].toInt()) {
                    0       -> return null
                    18      -> RoomType.BLOOD
                    30      -> RoomType.ENTRANCE
                    85      -> RoomType.UNKNOWN
                    63      -> RoomType.NORMAL
                    74      -> RoomType.CHAMPION
                    66      -> RoomType.PUZZLE
                    82      -> RoomType.FAIRY
                    62      -> RoomType.TRAP
                    else    -> RoomType.NORMAL
                }

                val data = RoomData("Unknown$column$row", roomType)
                Room(xPos, zPos, data)
            }
            !rowEven && !columnEven -> { // possible separator (only for 2x2)
                if(mapColors[(centerZ shl 7) + centerX].toInt() != 0){
                    Dungeon.getDungeonTile(column-1, row-1)?.let {
                        if (it is Room) {
                            Room(xPos, zPos, it.data).apply { isSeparator = true }
                        } else null
                    }
                }else null
            }
            else -> { // door or separator
                // Check the "side" of the connector to see whether it is a connector
                if (mapColors[( (if (rowEven) cornerZ else centerZ) shl 7) + (if (rowEven) centerX else cornerX)].toInt() != 0) { // separator
                    (if (rowEven) Dungeon.getDungeonTile(column - 1, row)
                    else Dungeon.getDungeonTile(column, row - 1))?.let {
                        if (it is Room) {
                            Room(xPos, zPos, it.data).apply { isSeparator = true }
                        } else null
                    }
                } else { // door or nothing
                    val doorType = when(mapColors[(centerZ shl 7) + centerX].toInt()) {
                        0 -> return null
                        119 -> DoorType.WITHER
                        30 -> DoorType.ENTRANCE
                        18 -> DoorType.BLOOD
                        else -> DoorType.NORMAL
                    }
                    Door(xPos, zPos, doorType)
                }
            }
        }?.apply { scanned = false }
    }
}
