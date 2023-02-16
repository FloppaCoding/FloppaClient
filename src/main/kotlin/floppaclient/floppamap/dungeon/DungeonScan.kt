package floppaclient.floppamap.dungeon

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.core.*
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.module.impl.render.DungeonMap
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos

object DungeonScan {

    /**
     * Scans the dungeon from the loaded chunks in the world and updates [Dungeon.dungeonList] based on that.
     * When all chunks are loaded [Dungeon.fullyScanned] will be set to true afterwards.
     */
    fun scanDungeon() {
        var allLoaded = true
        var updateConnection = false

        scan@ for (column in 0..10) {
            for (row in 0..10) {
                val xPos = Dungeon.startX + column * (Dungeon.roomSize shr 1)
                val zPos = Dungeon.startZ + row * (Dungeon.roomSize shr 1)

                if (!mc.theWorld.getChunkFromChunkCoords(xPos shr 4, zPos shr 4).isLoaded) {
                    allLoaded = false
                    continue
                }

                if (Dungeon.getDungeonTile(column, row)?.scanned == true) continue
                getRoomFromWorld(xPos, zPos, column, row)?.let { newTile ->
                    val oldTile = Dungeon.getDungeonTile(column, row)
                    // When the tile is already scanned from the map item make sure to not overwrite it.
                    // Instead just update the values.
                    if (oldTile != null) {
                        /*
                         NOTE: The following check does not account for the case when newTile and oldTile
                          are of different type. This should not happen and when it does the newTile is likely faulty.
                         */
                        if (oldTile is Room && newTile is Room) { // Rooms
                            oldTile.data.configData = newTile.data.configData
                            oldTile.core = newTile.core
                        } else { // Doors
                            oldTile.scanned = true
                        }
                    } else {
                        Dungeon.setDungeonTile(column, row, newTile)
                        if (newTile is Room && newTile.data.type == RoomType.NORMAL) updateConnection = true
                    }
                }
            }
        }

        if (updateConnection) {
            MapUpdate.synchConnectedRooms()
        }

        if (allLoaded) {
            Dungeon.fullyScanned = true
            Dungeon.totalSecrets = Dungeon.getDungeonTileList<Room>().filter { it.isUnique }.sumOf { it.data.maxSecrets ?: 0 }
            Dungeon.cryptCount = Dungeon.getDungeonTileList<Room>().filter { it.isUnique }.sumOf { it.data.crypts ?: 0 }
            Dungeon.trapType = Dungeon.getDungeonTileList<Room>().find { it.data.type === RoomType.TRAP }?.data?.name ?: ""
            Dungeon.witherDoors = Dungeon.getDungeonTileList<Door>().filter { it.type === DoorType.WITHER }.size + 1
            Dungeon.puzzles.addAll(Dungeon.getDungeonTileList<Room>().filter { it.data.type === RoomType.PUZZLE }.map{it.data.name})

            if (DungeonMap.scanChatInfo.enabled && !DungeonMap.legitMode.enabled) {
                modMessage(
                    "&aScan Finished!\n&aPuzzles (&c${Dungeon.puzzles.size}&a):${
                        Dungeon.puzzles.joinToString("\n&b- &d", "\n&b- &d", "\n")
                    }&6Trap: &a${Dungeon.trapType}\n&8Wither Doors: &7${Dungeon.witherDoors}\n&7Total Secrets: &b${Dungeon.totalSecrets}" +
                            "\n&7Total Crypts: &b${Dungeon.cryptCount}"
                )
            }
        }
    }

    /**
     * Creates a dungeon Tile instance from the World.
     * This is achieved by scanning the blocks in the column specified by [x] and [z].
     * Returns null when the column is air.
     */
    private fun getRoomFromWorld(x: Int, z: Int, column: Int, row: Int): Tile? {
        if (isColumnAir(x, z)) return null
        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            rowEven && columnEven -> { // Room
                val core = getCore(x, z)
                RoomUtils.getRoomConfigData(core)?.let { configData ->
                    val data = RoomData(configData)

                    Room(x, z, data).apply { this.core = core }
                }
            }
            !rowEven && !columnEven -> {  // possible separator (only for 2x2)
                Dungeon.getDungeonTile(column - 1, row - 1)?.let {
                    if (it is Room) {
                        Room(x, z, it.data).apply { isSeparator = true }
                    } else null
                }
            }
            isDoor(x, z) -> { // Door
                val bState = mc.theWorld.getBlockState(BlockPos(x, 69, z))
                val doorType = when {
                    bState.block == Blocks.coal_block -> DoorType.WITHER
                    bState.block == Blocks.monster_egg -> DoorType.ENTRANCE
                    bState.block == Blocks.stained_hardened_clay && Blocks.stained_hardened_clay.getMetaFromState(
                        bState
                    ) == 14 -> DoorType.BLOOD
                    else -> DoorType.NORMAL
                }
                Door(x, z, doorType)
            }
            else -> { // Possible separator
                (if (rowEven) Dungeon.getDungeonTile(column - 1, row)
                else Dungeon.getDungeonTile(column, row - 1))?.let {
                    if (it is Room) {
                        if (it.data.type == RoomType.ENTRANCE) {
                            Door(x, z, DoorType.ENTRANCE)
                        } else {
                            Room(x, z, it.data).apply { isSeparator = true }
                        }
                    } else null
                }
            }
        }
    }

    fun getRoomCentre(posX: Int, posZ: Int): Pair<Int, Int> {
        val roomX = (posX - Dungeon.startX) shr 5
        val roomZ = (posZ - Dungeon.startZ) shr 5
        var x = 32 * roomX + Dungeon.startX
        if (x !in posX - 16..posX + 16) x += 32
        var z = 32 * roomZ + Dungeon.startZ
        if (z !in posZ - 16..posZ + 16) z += 32
        return Pair(x, z)
    }

    private fun isColumnAir(x: Int, z: Int): Boolean {
        for (y in 12..140) {
            if (mc.theWorld.getBlockState(BlockPos(x, y, z)).block != Blocks.air) {
                return false
            }
        }
        return true
    }

    private fun isDoor(x: Int, z: Int): Boolean {
        val xPlus4 = isColumnAir(x + 4, z)
        val xMinus4 = isColumnAir(x - 4, z)
        val zPlus4 = isColumnAir(x, z + 4)
        val zMinus4 = isColumnAir(x, z - 4)
        return xPlus4 && xMinus4 && !zPlus4 && !zMinus4 || !xPlus4 && !xMinus4 && zPlus4 && zMinus4
    }

    fun getCore(x: Int, z: Int): Int {
        val blocks = arrayListOf<Int>()
        for (y in 140 downTo 12) {
            val id = Block.getIdFromBlock(mc.theWorld.getBlockState(BlockPos(x, y, z)).block)
            if (!id.equalsOneOf(5, 54)) {
                blocks.add(id)
            }
        }
        return blocks.joinToString("").hashCode()
    }
}
