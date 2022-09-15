package floppaclient.funnymap.features.dungeon

import floppaclient.FloppaClient.Companion.mc
import floppaclient.funnymap.core.Room
import floppaclient.module.impl.render.DungeonMap
import floppaclient.utils.Utils.modMessage
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.BlockPos

object MimicDetector {
    fun findMimic() {
        val mimicRoom = getMimicRoom()
        if (mimicRoom == "") return
        if(DungeonMap.mimicInfo.enabled) modMessage("Mimic found in $mimicRoom")
        Dungeon.dungeonList.forEach {
            if (it is Room && it.data.name == mimicRoom) {
                it.hasMimic = true
            }
        }
        Dungeon.mimicFound = true
    }

    private fun getMimicRoom(): String {
        mc.theWorld.loadedTileEntityList.filter { it is TileEntityChest && it.chestType == 1 }
            .mapNotNull { getRoomFromPos(it.pos) }.groupingBy { it.data.name }.eachCount()
            .forEach { (room, trappedChests) ->
                Dungeon.uniqueRooms.find { it.data.name == room && it.data.trappedChests < trappedChests }
                    ?.let { return it.data.name }
            }
        return ""
    }

    private fun getRoomFromPos(pos: BlockPos): Room? {
        val x = ((pos.x - Dungeon.startX + 15) shr 5)
        val z = ((pos.z - Dungeon.startZ + 15) shr 5)
        val room = Dungeon.dungeonList.getOrNull(x * 2 + z * 22)
        return if (room is Room) room else null
    }
}
