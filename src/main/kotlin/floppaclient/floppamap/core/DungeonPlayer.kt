package floppaclient.floppamap.core

import floppaclient.FloppaClient.Companion.mc
import floppaclient.FloppaClient.Companion.scope
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.utils.MapUtils
import floppaclient.floppamap.utils.MapUtils.mapX
import floppaclient.floppamap.utils.MapUtils.mapZ
import floppaclient.floppamap.utils.MapUtils.yaw
import floppaclient.module.impl.dungeon.PartyTracker
import floppaclient.module.impl.render.DungeonMap
import floppaclient.utils.HypixelApiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec4b

/**
 * Class to store information about dungeon teammates.
 * The Player will also be handled in this way as a teammate.
 *
 * This class also contains methods to update and track teammate data.
 * [updatePlayerAndRoom] handles updating the found secrets on the map.
 *
 * @author Aton
 */
class DungeonPlayer(var player: EntityPlayer, var name: String,
                    /**
                     * True when the field player is not the correct entity corresponding to this Player.
                     */
                    var fakeEntity: Boolean = false
) {
    var mapX = 0.0
    var mapZ = 0.0
    var yaw = 0f
    var icon = ""
    var dead = false
    var deaths = 0
    var secretsAtRunStart: Int? = null

    /**
     * Stores the index of the room the player is currently in within the [Dungeon.dungeonList].
     * This index will still point to the correct tile even when the tile is overwritten by the scan.
     * Get the corresponding Room from [currentRoom].
     *
     * There is no check what kind of tile this is.
     * Will also hold an index when no room is loaded in for the tile yet.
     *
     * This index is calculated as column * 11 + row.
     */
    private var currentRoomIndex: Int? = null

    /**
     * Gets the room this DungeonPlayer is currently in.
     * Does not include boss room.
     * This method is meant to be used to track the position of the dungeon teammates and not the Player.
     *
     * Not to be confused with [Dungeon.currentRoom].
     */
    val currentRoom: Room?
        get() = (currentRoomIndex?.let{Dungeon.getDungeonTileList()[it]} as? Room)

    /**
     * Maps the index of the tile in [Dungeon.dungeonList] to the count of ticks the player spent in that Tile.
     * The key -1 is used for time spent dead in clear.
     * @see currentRoomIndex
     */
    val visitedTileTimes: MutableMap<Int, Int> = mutableMapOf()

    private var lastSecretCheck: SecretCheck = SecretCheck(null, null, null, 0L)
    private var pending: Boolean = false

    init {
        if (!fakeEntity && (PartyTracker.enabled || DungeonMap.trackSecrets.enabled)) {
            scope.launch(Dispatchers.IO) { secretsAtRunStart = fetchTotalSecretsFromApi() }
        }
    }

    /**
     * Updates the teammates position and the secrets in the room they are in.
     */
    fun updatePlayerAndRoom(decor: Map<String, Vec4b>?) {
        // Update the position in the world
        val player = mc.theWorld.playerEntities.find { it.name == this.name }
        // when the player is in render distance, use that data instead of the map item
        if (player != null) {
            // check whether the player is in the map; probably not needed
            if ( player.posX > -200 && player.posX < -10 && player.posZ > -200 && player.posZ < -10) {
                this.mapX = (player.posX - Dungeon.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first - 2
                this.mapZ = (player.posZ - Dungeon.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second - 2
                this.yaw = player.rotationYawHead
            }
        }else {
            //if no data from the map item is present go to the next player
            if (decor != null) {
                decor.entries.find { (icon, _) -> icon == this.icon }?.let { (_, vec4b) ->
                    this.mapX = vec4b.mapX.toDouble()
                    this.mapZ = vec4b.mapZ.toDouble()
                    this.yaw = vec4b.yaw
                }
            }
        }

        // Update the current room and info about it.
        val newIndex = getCurrentRoomIndex()
        val oldRoom = currentRoom
        val shouldUpdateSecrets = DungeonMap.trackSecrets.enabled &&  Dungeon.hasRunStarted &&  !pending &&
                (System.currentTimeMillis() > lastSecretCheck.timeMS + 5000
                        || ( newIndex != currentRoomIndex && oldRoom?.data?.name != (newIndex?.let{Dungeon.getDungeonTileList()[it]} as? Room)?.data?.name ))
        currentRoomIndex = newIndex
        updateVisitedTileTimes()
        if (shouldUpdateSecrets ) {
            updateRoomSecrets(oldRoom)
        }
    }

    /**
     * Updates the secrets within [oldRoom] from the total collected secrets if possible.
     * @param oldRoom the room the player was in previously.
     */
    private fun updateRoomSecrets(oldRoom: Room?) {
        scope.launch(Dispatchers.IO) {
            if (oldRoom == null && currentRoom == null) return@launch
            val oldSecretCheck = lastSecretCheck
            pending = true
            val newSecrets = fetchTotalSecretsFromApi(oldRoom)
            pending = false
            if (oldRoom != null && oldRoom.data.name == oldSecretCheck.newRoom?.data?.name && newSecrets != null && oldSecretCheck.secrets != null) {
                val difference = newSecrets - oldSecretCheck.secrets
                oldRoom.data.currentSecrets += difference
            }
        }
    }

    /**
     * Increments the tick count this player spent in the current Tile in [visitedTileTimes].
     * Dead time is counted with index -1.
     */
    private fun updateVisitedTileTimes() {
        val index = if (dead) -1 else currentRoomIndex ?: return
        visitedTileTimes[index] = (visitedTileTimes[index] ?: 0) + 1
    }

    /**
     * Fetches the total collected secrets for the player from the API.
     * Run this with the IO dispatcher [Dispatchers.IO].
     *
     * Only use this method within a coroutine to not freeze the main thread.
     * @param oldRoom when this method is called on room change this will be the room the player was in previously.
     * @return The total secrets collected by this player, or null if no information could be retrieved.
     */
    suspend fun fetchTotalSecretsFromApi(oldRoom: Room? = null): Int? {
        val time = System.currentTimeMillis()
        val secrets = HypixelApiUtils.getSecrets(player.uniqueID.toString())
        lastSecretCheck = SecretCheck(secrets, currentRoom, oldRoom, time)
        return secrets
    }

    /**
     * Return the index of the room the player is currently in within the [Dungeon.dungeonList].
     * This index will still point to the correct tile even when the tile is overwritten by the scan.
     *
     * There is no check what kind of tile this is.
     * Will also return an index when no room is loaded in for the tile yet.
     */
    @JvmName("getCurrentRoomIndexFromCoordinates")
    private fun getCurrentRoomIndex(): Int? {
        if (Dungeon.inBoss) return null
        // Note the shr 5 ( / 32 ) instead of the usual shr 4 here. This ensures that only rooms can be pointed to.
        // But also means that the x and z values here are half of the column and row.
        val x = (((mapX + 2 - MapUtils.startCorner.first) / MapUtils.coordMultiplier ).toInt() shr 5)
        val z = (((mapZ + 2 - MapUtils.startCorner.second) / MapUtils.coordMultiplier).toInt() shr 5)
        if (x<0 || x > 5 || z < 0 || z > 5) return null
        return x * 22 + z * 2
    }

    /**
     * Class to store the data from the last time the secret count was checked for this player from the api.
     */
    private data class SecretCheck(
        val secrets: Int?,
        /** The room the player is currently in.*/
        val newRoom: Room?,
        /** When changing room, this is the room the player was in last. Otherwise null. */
        val oldRoom: Room?,
        val timeMS: Long
    )
}
