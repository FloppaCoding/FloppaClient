package floppaclient.funnymap.features.dungeon

import floppaclient.FloppaClient.Companion.mc
import floppaclient.funnymap.core.*
import floppaclient.funnymap.utils.MapUtils
import floppaclient.funnymap.utils.MapUtils.mapX
import floppaclient.funnymap.utils.MapUtils.mapZ
import floppaclient.funnymap.utils.MapUtils.yaw
import floppaclient.utils.Utils
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.StringUtils

object MapUpdate {
    fun calibrate() {
        MapUtils.startCorner = when {
            Utils.currentFloor == 1 -> Pair(22, 11)
            Utils.currentFloor.equalsOneOf(2, 3) -> Pair(11, 11)
            Utils.currentFloor == 4 && Dungeon.rooms.size > 25 -> Pair(5, 16)
            Dungeon.rooms.size == 30 -> Pair(16, 5)
            Dungeon.rooms.size == 25 -> Pair(11, 11)
            else -> Pair(5, 5)
        }

        MapUtils.roomSize = if (Utils.currentFloor in 1..3 || Dungeon.rooms.size == 24) 18 else 16

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


    // Changing this method to not only update the players, but also get missing players
    fun updatePlayers(tabEntries: List<Pair<NetworkPlayerInfo, String>>) {
        var iconNum = 0
        for (i in listOf(5, 9, 13, 17, 1)) {
            val tabText = StringUtils.stripControlCodes(tabEntries[i].second).trim()
            val name = tabText.split(" ").getOrNull(1) ?: ""
            if (name == "") continue
            // if the player is not in the list add it
            var player = Dungeon.dungeonTeammates.find { it.name == name }
            if ((player == null) || player.fakeEntity) {
                Dungeon.dungeonTeammates.remove(player)
                val potPlayer = mc.theWorld.playerEntities.find { it.name == name }
                val fake = potPlayer == null
                (potPlayer ?: EntityOtherPlayerMP(mc.theWorld, tabEntries[i].first.gameProfile))
                    .let {
                        Dungeon.dungeonTeammates.add(DungeonPlayer(it, name).apply {
                            this.fakeEntity = fake
                        })
                    }
            }

            player = Dungeon.dungeonTeammates.find { it.name == name } ?: continue
            player.dead = tabText.contains("(DEAD)")
            if (!player.dead) {
                player.icon = "icon-${iconNum}"
                iconNum++
            } else {
                player.icon = ""
            }
        }

        // Changes here to make player positions and head rotations work before dungeon start
        val decor = MapUtils.getMapData()?.mapDecorations
        Dungeon.dungeonTeammates.forEach { dungeonPlayer ->
            if (dungeonPlayer.player == mc.thePlayer) {
                dungeonPlayer.yaw = dungeonPlayer.player.rotationYawHead
            } else {
                val player = mc.theWorld.playerEntities.find { it.name == dungeonPlayer.name }
                // when the player is in render distance, use that data instead of the map item
                if (player != null) {
                    // check whether the player is in the map; probably not needed
                    if ( player.posX > -200 && player.posX < -10 && player.posZ > -200 && player.posZ < -10) {
                        dungeonPlayer.mapX = (player.posX - Dungeon.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first - 2
                        dungeonPlayer.mapZ = (player.posZ - Dungeon.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second - 2
                        dungeonPlayer.yaw = player.rotationYawHead
                    }
                }else {
                    //if no date from the map item is present go to the next player
                    if (decor == null) return@forEach
                    decor.entries.find { (icon, _) -> icon == dungeonPlayer.icon }?.let { (_, vec4b) ->
                        dungeonPlayer.mapX = vec4b.mapX.toDouble()
                        dungeonPlayer.mapZ = vec4b.mapZ.toDouble()
                        dungeonPlayer.yaw = vec4b.yaw
                    }
                }
            }
        }
    }

    fun updateRooms() {
        val mapColors = MapUtils.getMapData()?.colors ?: return

        val startX = MapUtils.startCorner.first + (MapUtils.roomSize shr 1)
        val startZ = MapUtils.startCorner.second + (MapUtils.roomSize shr 1)
        val increment = (MapUtils.roomSize shr 1) + 2

        for (x in 0..10) {
            for (z in 0..10) {

                val mapX = startX + x * increment
                val mapZ = startZ + z * increment

                if (mapX >= 128 || mapZ >= 128) continue

                val room = Dungeon.dungeonList[z * 11 + x]

                room.state = when (mapColors[(mapZ shl 7) + mapX].toInt()) {
                    0, 85, 119 -> RoomState.UNDISCOVERED
                    18 -> if (room is Room) when (room.data.type) {
                        RoomType.BLOOD -> RoomState.DISCOVERED
                        RoomType.PUZZLE -> RoomState.FAILED
                        else -> room.state
                    } else RoomState.DISCOVERED
                    30 -> if (room is Room) when (room.data.type) {
                        RoomType.ENTRANCE -> RoomState.DISCOVERED
                        else -> RoomState.GREEN
                    } else room.state
                    34 -> RoomState.CLEARED
                    else -> RoomState.DISCOVERED
                }
            }
        }
    }

    fun updateDoors() {
        for ((door, pos) in Dungeon.doors) {
            if (!door.opened && mc.theWorld.getChunkFromChunkCoords(door.x shr 4, door.z shr 4).isLoaded) {
                if (mc.theWorld.getBlockState(BlockPos(door.x, 69, door.z)).block == Blocks.air) {
                    val room = Dungeon.dungeonList[pos.first + pos.second * 11]
                    if (room is Door && room.type == DoorType.WITHER) {
                        room.opened = true
                        door.opened = true
                    }
                }
            }
        }
    }
}
