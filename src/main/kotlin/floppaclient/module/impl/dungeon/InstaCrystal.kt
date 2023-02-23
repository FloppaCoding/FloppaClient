package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.TeleportEventPre
import floppaclient.utils.ClipTools
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSelectorSetting
import floppaclient.utils.ChatUtils.modMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.math.floor

object InstaCrystal : Module (
    "Insta Crystal",
    category = Category.DUNGEON,
    description = "Instantly clips to the selected crystal on f7 boss entry."
){
    /**
     * Position on boss entry should be 73.5,221.0,14.5
     * Left crystal target block: 80,237,49
     * Right crystal target block: 66,237,49
     */

    private val targetCrystal = StringSelectorSetting("Target","right", arrayListOf("right", "left"))
    private val delay = NumberSetting("Delay",150.0, 0.0, 300.0,10.0)
    private val showInfo = BooleanSetting("Chat message", true)

    private var tpTicks = 0

    init {
        this.addSettings(
            targetCrystal,
            delay,
            showInfo
        )
    }

//    /**
//     * Detects the f7 Boss message
//     */
//    @SubscribeEvent(priority = EventPriority.HIGHEST)
//    fun onChatPacket(event: ReceivePacketEvent) {
//        if (event.packet !is S02PacketChat || event.packet.type.toInt() == 2 || !inDungeons) return
//        when (StringUtils.stripControlCodes(event.packet.chatComponent.unformattedText)) {
//            "[BOSS] Maxor: WELL WELL WELL LOOK WHO’S HERE!" -> {
//                doCrystalWarp()
//            }
//        }
//    }

    /**
     * Registers teleport by recieved packet.
     * Note: each teleport in skyblock is sent twice.
     */
    @SubscribeEvent(receiveCanceled = true)
    fun onTeleport(event: TeleportEventPre) {
        if (!inDungeons) return
        if (tpTicks > 0) return
        // Check for correct location
        if (floor(event.packet.x) == 73.0
            && floor(event.packet.y) == 221.0
            && floor(event.packet.z) == 14.0
        ) {
            if (tpTicks <= 0) {
                tpTicks = 10
                Timer().schedule( delay.value.toLong()) {
                    crystalClip()
                }

            }

        }
    }

    /**
     * Count down tp cooldown ticks
     */
    @SubscribeEvent
    fun onTick(event: ClientTickEvent){
        if (event.phase != TickEvent.Phase.START) return
        if (tpTicks> 0) tpTicks--
    }

    private fun crystalClip() {
        if (showInfo.enabled) modMessage("Attempting clip to ${targetCrystal.selected} crystal")
        val route = if (targetCrystal.selected == "right") {
            mutableListOf(64.5,241.0,49.0)
        } else {
            mutableListOf(82.5,241.0,49.0)
        }
        val start = mc.thePlayer.positionVector.run {
            mutableListOf(this.xCoord, this.yCoord, this.zCoord)
        }
        ClipTools.executeClipRoute(route, relative = false, startPos = start)
    }
}