package floppaclient.funnymap.features.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.FloppaClient.Companion.scope
import floppaclient.events.RoomChangeEvent
import floppaclient.funnymap.core.*
import floppaclient.funnymap.features.extras.ExtrasScan
import floppaclient.funnymap.features.extras.RoomUtils
import floppaclient.funnymap.utils.MapUtils
import floppaclient.module.impl.render.DungeonMap
import floppaclient.utils.TabListUtils
import floppaclient.utils.Utils.currentFloor
import floppaclient.utils.Utils.equalsOneOf
import kotlinx.coroutines.launch
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object Dungeon {

    const val roomSize = 32
    const val startX = -185
    const val startZ = -185

    private var lastScanTime: Long = 0
    private var isScanning = false
    private var isScanningRotation = false
    var fullyScanned = false
    var fullyScannedRotation = false

    var hasRunStarted = false
    var inBoss = false
    // 6 x 6 room grid, 11 x 11 with connections
    val dungeonList = Array<Tile>(121) { Door(0, 0) }
    val uniqueRooms = mutableListOf<Room>()
    val rooms = mutableListOf<Room>()
    val doors = mutableMapOf<Door, Pair<Int, Int>>()
    var mimicFound = false

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

    private val entryMessages = listOf(
        "[BOSS] Bonzo: Gratz for making it this far, but I’m basically unbeatable.",
        "[BOSS] Scarf: This is where the journey ends for you, Adventurers.",
        "[BOSS] The Professor: I was burdened with terrible news recently...",
        "[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!",
        "[BOSS] Livid: Welcome, you arrive right on time. I am Livid, the Master of Shadows.",
        "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!",
        "[BOSS] Maxor: WELL WELL WELL LOOK WHO’S HERE!"
    )

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !inDungeons) return
        if (shouldScan()) {
            lastScanTime = System.currentTimeMillis()
            isScanning = true
            DungeonScan.scanDungeon()
            isScanning = false
        }
        if (shouldScanRotation()) {
            isScanningRotation = true
            ExtrasScan.scanDungeon()
            isScanningRotation = false
        }
        val newRoom = getCurrentRoomPair()
        if (newRoom != currentRoomPair) {
            MinecraftForge.EVENT_BUS.post(RoomChangeEvent(newRoom, currentRoomPair))
            currentRoomPair = newRoom
        }

        // removed the full scanned check
        scope.launch {
            if (!mimicFound && currentFloor.equalsOneOf(6, 7)) {
                MimicDetector.findMimic()
            }
            MapUpdate.updateRooms()
            MapUpdate.updateDoors()

            getDungeonTabList()?.let {
                MapUpdate.updatePlayers(it)
                RunInformation.updateRunInformation(it)
            }
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
        if (!inDungeons || event.type.toInt() == 2) return
        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        when {
            text.equalsOneOf(
                "Dungeon starts in 4 seconds.", "Dungeon starts in 4 seconds. Get ready!"
            ) -> MapUpdate.preloadHeads()

            text == "[NPC] Mort: Here, I found this map when I first entered the dungeon." -> {
                MapUpdate.calibrate()
                hasRunStarted = true
            }
            entryMessages.any { it == text } -> inBoss = true
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
        DungeonMap.autoScan.enabled && !isScanning && !fullyScanned && !inBoss && currentFloor != null

    private fun shouldScanRotation() =
        DungeonMap.autoScan.enabled && !isScanningRotation && !fullyScannedRotation && !inBoss && currentFloor != null

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
    @JvmName("getCurrentRoomAgain")
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
            dungeonList.getOrNull(x * 2 + z * 22)
        }
        if (room !is Room) return null
        return room
    }

    /**
     * Rests most of the dungeon properties. Other properties which are not reset here are reset in onWorlLoad.
     * @see onWorldLoad
     */
    fun reset() {
        ExtrasScan.rooms.clear()

        currentRoomPair = null

        dungeonTeammates.clear()

        dungeonList.fill(Door(0, 0))
        uniqueRooms.clear()
        rooms.clear()
        doors.clear()
        mimicFound = false

        puzzles.clear()
        trapType = ""
        witherDoors = 0
        secretCount = 0
        cryptCount = 0
    }
}
