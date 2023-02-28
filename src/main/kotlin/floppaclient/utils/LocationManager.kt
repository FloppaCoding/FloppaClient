package floppaclient.utils

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.core.Room
import floppaclient.floppamap.utils.RoomUtils
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

object LocationManager {

    var onHypixel: Boolean = false
    var inSkyblock: Boolean = false
    var inDungeons = false
        get() = inSkyblock && field
    var currentRegionPair: Pair<Room, Int>? = null

    /**
     * Keeps track of elapsed ticks, gets reset at 20
     */
    private var tickRamp = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        tickRamp++

        if (tickRamp % 20 == 0) {
            if (mc.thePlayer != null) {

                if (!inSkyblock) {
                    inSkyblock = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)
                        ?.let { ScoreboardUtils.cleanSB(it.displayName).contains("SKYBLOCK") } ?: false
                }

                // If alr known that in dungeons don't update the value. It does get reset to false on world change.
                if (!inDungeons) {
                    inDungeons = inSkyblock && ScoreboardUtils.sidebarLines.any {
                        ScoreboardUtils.cleanSB(it).run {
                            (contains("The Catacombs") && !contains("Queue")) || contains("Dungeon Cleared:")
                        }
                    }
                }
            }
            tickRamp = 0
        }
        val newRegion = getArea()
        if (currentRegionPair?.first?.data?.name != newRegion){
            currentRegionPair = newRegion?.let { Pair( RoomUtils.instanceRegionRoom(it) , 0) }
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        onHypixel = false
        inSkyblock = false
        inDungeons = false
    }

    @SubscribeEvent
    fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent.Unload) {
        inDungeons = false
        inSkyblock = false
        currentRegionPair = null
        tickRamp = 18
    }

    /**
     * Taken from [SBC](https://github.com/Harry282/Skyblock-Client/blob/main/src/main/kotlin/skyblockclient/utils/LocationUtils.kt)
     */
    @SubscribeEvent
    fun onConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        onHypixel = mc.runCatching {
            !event.isLocal && ((thePlayer?.clientBrand?.lowercase()?.contains("hypixel")
                ?: currentServerData?.serverIP?.lowercase()?.contains("hypixel")) == true)
        }.getOrDefault(false)
    }


    /**
     * Returns the current area from the tab list info.
     * If no info can be found return null.
     */
    private fun getArea(): String? {
        if (!inSkyblock) return null
        val nethandlerplayclient: NetHandlerPlayClient = mc.thePlayer?.sendQueue ?: return null
        val list = nethandlerplayclient.playerInfoMap ?: return null
        var area: String? = null
        var extraInfo: String? = null
        for (entry in list) {
            //  "Area: Hub"
            val areaText = entry?.displayName?.unformattedText ?: continue
            if (areaText.startsWith("Area: ")) {
                area = areaText.substringAfter("Area: ")
                if (!area.contains("Private Island")) break
            }
            if (areaText.contains("Owner:")){
                extraInfo = areaText.substringAfter("Owner:")
            }

        }
        return if (area == null)
            null
        else
            area + (extraInfo ?: "")
    }
}