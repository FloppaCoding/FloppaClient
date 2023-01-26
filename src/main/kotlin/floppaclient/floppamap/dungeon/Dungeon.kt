package floppaclient.floppamap.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.DungeonEndEvent
import floppaclient.events.RoomChangeEvent
import floppaclient.floppamap.core.*
import floppaclient.floppamap.extras.ExtrasScan
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.floppamap.utils.MapUtils
import floppaclient.module.impl.render.DungeonMap
import floppaclient.utils.TabListUtils
import floppaclient.utils.Utils.currentFloor
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.abs

/**
 * In this class everything Dungeon map related is dispatched.
 * It is also used to post various dungeon related events to be used within the Modules.
 *
 * Based on [FunnyMap by Harry282](https://github.com/Harry282/FunnyMap).
 * @author Aton
 */
object Dungeon {
    const val roomSize = 32
    const val startX = -185
    const val startZ = -185

    private var lastScanTime: Long = 0
    var fullyScanned = false
    var fullyScannedRotation = false

    var hasRunStarted = false
    var inBoss = false
    // 6 x 6 room grid, 11 x 11 with connections
    /**
     * This field stores all Tiles of the current dungeon in a 11x11 Array.
     * To access a specific Tile [getDungeonTile] and [setDungeonTile] are generally the better option.
     * <p>
     * The fields in this array correspond to a 11x11 Grid over the Dungeon.
     * This grid consists of  6 rows / columns for the Rooms and in between another 5 for connectors (Doors / separators
     * for rooms consisting of more than one Tile). All the Rooms lay on an even row and column.
     * The array index for a given column (corresponds to the tiles x coordinate) and
     * row (corresponds to the tiles z coordinate) is 'column*11 + row'. This is the same sort order as used by Hypixel
     * for Puzzle names on the tab list.
     * </p>
     */
    private val dungeonList = Array<Tile?>(121) { null }

    var mimicFound = false

    /**
     * Contains all the teammates in the current dungeon.
     * Also contains the Player.
     */
    val dungeonTeammates = mutableListOf<DungeonPlayer>()

    // Used for chat info
    val puzzles = mutableListOf<String>()
    var trapType = ""
    var witherDoors = 0
    var secretCount = 0
    var cryptCount = 0

    /**
     * Contains the current room and its rotation. Updated every tick.
     * The rotation is based on the clipConfig.
     * For extras Block rotations refer to Extras.currentExtrasRoom.
     */
    var currentRoomPair: Pair<Room, Int>? = null

    val currentRoom: Room?
        get() {
            return currentRoomPair?.first ?: getCurrentRoom()
        }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !inDungeons || mc.thePlayer == null) return
        // World based scan
        if (shouldScan()) {
            lastScanTime = System.currentTimeMillis()
            DungeonScan.scanDungeon()
        }
        if (shouldScanRotation()) {
            ExtrasScan.scanDungeon()
        }
        // Map item based scan
        if (DungeonMap.enabled) {
            MapUpdate.updateRooms()
        }
        val newRoom = getCurrentRoomPair()
        if (newRoom != currentRoomPair) {
            MinecraftForge.EVENT_BUS.post(RoomChangeEvent(newRoom, currentRoomPair))
            currentRoomPair = newRoom
        }
        if (!mimicFound && currentFloor.equalsOneOf(6, 7)) {
            MimicDetector.findMimic()
        }

        getDungeonTabList()?.let {
            MapUpdate.updatePlayers(it)
            RunInformation.updateRunInformation(it)
        }

        // added check to determine whether in boss based on coordinates. This is relevant when blood is being skipped.
        // this also makes the chat message based detection obsolete
        if (FloppaClient.tickRamp % 20 == 0) {
            when ( currentFloor ) {
                1 -> inBoss = mc.thePlayer.posX > -71 && mc.thePlayer.posZ > -39
                2,3,4 -> inBoss = mc.thePlayer.posX > -39 && mc.thePlayer.posZ > -39
                5,6 -> inBoss = mc.thePlayer.posX > -39 && mc.thePlayer.posZ > -7
                7 -> inBoss = mc.thePlayer.posX > -7 && mc.thePlayer.posZ > -7
            }
            if (hasRunStarted && !MapUtils.calibrated) MapUpdate.calibrate()
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onChat(event: ClientChatReceivedEvent) {
        if (!inDungeons) return
        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        if (event.type.toInt() != 2) { //type:: 0 : Standard Text Message; 1 : 'System' message, displayed as standard text.
            when {
                text.equalsOneOf(
                    "Dungeon starts in 4 seconds.", "Dungeon starts in 4 seconds. Get ready!"
                ) -> MapUpdate.preloadHeads()

                text == "[NPC] Mort: Here, I found this map when I first entered the dungeon." -> {
                    MapUpdate.calibrate()
                    hasRunStarted = true
                }
                entryMessages.any { it == text } -> inBoss = true
                text == "                             > EXTRA STATS <" -> {
                    MinecraftForge.EVENT_BUS.post(DungeonEndEvent())
                }
                text.contains("☠") -> {
                    val matcher = deathPattern.find(text)
                    val deadName = matcher?.groups?.get("name")?.value
                    dungeonTeammates.find {
                        if (deadName.equals("you", true)) it.name == mc.thePlayer.name else it.name == deadName
                    }?.apply { deaths++ }
                }
            }
        }else if (event.type.toInt() == 2) { //Action bar
            val matcher = secretsPattern.find(text)
            if (matcher != null) {
                /*
                This part is supposed to take care of setting the correct secrets for the room.
                Dont do this for now.
                It will mess up the api based calculation.
                Should be added at some point, but is a bit tricky.

                currentRoom?.data?.currentSecrets = matcher.groupValues[1].toInt()
                */

                currentRoom?.data?.maxSecrets = matcher.groupValues[2].toInt()
            }
        }
    }

    /**
     * Update visited rooms when room is changed.
     * This event is posted whenever you, the Player, change the Tile you are in.
     */
    @SubscribeEvent
    fun onRoomChange(event: RoomChangeEvent) {
        if (event.newRoomPair == null) return
        if (event.newRoomPair.first.data.type == RoomType.BOSS || event.newRoomPair.first.data.type == RoomType.REGION) return
        // Update all of the connected rooms and separators to visited.
        dungeonList.forEach {
            if ((it is Room) && it.data.type != RoomType.UNKNOWN && it.data === event.newRoomPair.first.data){
                it.visited = true
            }
        }

        // Reveal door connecting the two rooms.
        if (event.oldRoomPair == null) return
        val doorRow = if (abs(event.newRoomPair.first.row - event.oldRoomPair.first.row) <= 2)
            (event.newRoomPair.first.row + event.oldRoomPair.first.row) shr 1
        else return
        val doorColumn = if (abs(event.newRoomPair.first.column - event.oldRoomPair.first.column) <= 2)
            (event.newRoomPair.first.column + event.oldRoomPair.first.column) shr 1
        else return
        if (doorRow < 0 || doorRow > 11 || doorColumn < 0 || doorColumn < 11) return
        getDungeonTile<Door>(doorColumn, doorRow)?.let { door ->
            door.visited = true
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        reset()
        MapUtils.calibrated = false
        hasRunStarted = false
        inBoss = false
        fullyScanned = false
        fullyScannedRotation = false
    }

    private fun shouldScan() =
        DungeonMap.autoScan.enabled && !fullyScanned && !inBoss && currentFloor != null

    private fun shouldScanRotation() =
        DungeonMap.autoScan.enabled && !fullyScannedRotation && !inBoss && currentFloor != null

    fun getDungeonTabList(): List<Pair<NetworkPlayerInfo, String>>? {
        val tabEntries = TabListUtils.tabList
        if (tabEntries.size < 18 || !tabEntries[0].second.contains("§r§b§lParty §r§f(")) {
            return null
        }
        return tabEntries
    }

    /**
     * Returns the room the player is in, as well as its rotation, or null when the room can not be recognized.
     * Includes Boss room.
     * Is used to update the Dungeon.currentRoomPair every tick.
     */
    @JvmName("getCurrentRoomPairWithRotation")
    private fun getCurrentRoomPair(): Pair<Room, Int>? {
        val room = getCurrentRoom()
        if (room !is Room) return null
        return if(room.data.type == RoomType.BOSS) {
            Pair(room, 0)
        }else {
            ExtrasScan.rooms.entries.find { it.key.data.name == room.data.name }?.toPair()
        }
    }

    /**
     * Returns the room the player is currently in.
     * Includes boss room.
     */
    @JvmName("getCurrentRoomFromCoordinates")
    fun getCurrentRoom(): Room? {
        val room = if (inBoss) {
            val floor = currentFloor
            if (floor != null) {
                RoomUtils.instanceBossRoom(floor)
            }else {
                null
            }
        }else {
            val x = ((mc.thePlayer.posX - startX + 15).toInt() shr 5)
            val z = ((mc.thePlayer.posZ - startZ + 15).toInt() shr 5)
            getDungeonTile(x*2, z*2)
        }
        if (room !is Room) return null
        return room
    }

    /**
     * Gets the corresponding Tile from [dungeonList] but first performs a check whether the indices are in range.
     */
    @JvmName("getDungeonTileDefault")
    fun getDungeonTile(column: Int, row: Int) : Tile?{
        return dungeonList[column*11 + row]
    }

    /**
     * Gets the corresponding Tile from [dungeonList] but first performs a check whether the indices are in range.
     * It is attempted to cast the Tile to [T], if not possible returns null.
     */
    inline fun <reified T : Tile> getDungeonTile(column: Int, row: Int) : T?{
        if (row !in 0..10 || column !in 0..10) return null
        return (getDungeonTile(column, row) as? T)
    }

    /**
     * Sets the corresponding value from [dungeonList] but first performs a check whether the indices are in range.
     */
    fun setDungeonTile(column: Int, row: Int, tile: Tile?): Boolean{
        if (row !in 0..10 || column !in 0..10) return false
        dungeonList[column*11 + row] = tile
        return true
    }

    /**
     * Returns the [dungeonList] as an immutable List.
     */
    fun getDungeonTileList(): List<Tile?>{
        return dungeonList.asList()
    }

    /**
     * Returns the [dungeonList] filtered for the supplied Tile Type.
     */
    inline fun <reified T : Tile> getDungeonTileList(): List<T>{
        return getDungeonTileList().filterIsInstance<T>()
    }

    /**
     * Rests most of the dungeon properties. Other properties which are not reset here are reset in onWorlLoad.
     * @see onWorldLoad
     */
    fun reset() {
        ExtrasScan.rooms.clear()

        currentRoomPair = null

        dungeonTeammates.clear()

        dungeonList.fill(null)
        mimicFound = false

        puzzles.clear()
        trapType = ""
        witherDoors = 0
        secretCount = 0
        cryptCount = 0
    }

    private val deathPattern = Regex("^ ☠ (?<name>\\w+) .+ and became a ghost")
    private val secretsPattern = Regex("([0-9]+)/([0-9]+) Secrets")

    private val entryMessages = listOf(
        "[BOSS] Bonzo: Gratz for making it this far, but I’m basically unbeatable.",
        "[BOSS] Scarf: This is where the journey ends for you, Adventurers.",
        "[BOSS] The Professor: I was burdened with terrible news recently...",
        "[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!",
        "[BOSS] Livid: Welcome, you arrive right on time. I am Livid, the Master of Shadows.",
        "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!",
        "[BOSS] Maxor: WELL WELL WELL LOOK WHO’S HERE!"
    )
}
