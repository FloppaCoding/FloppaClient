package floppaclient.module.impl.dev

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PacketSentEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.network.play.client.C00PacketKeepAlive
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DevModule : Module(
    "Dev Module",
    category = Category.MISC,
) {

    private val showAllSent = BooleanSetting("All sent", false)

    init {
        this.addSettings(
            showAllSent
        )
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (mc.thePlayer == null) return
        when(val packet = event.packet) {
            is C0EPacketClickWindow -> {
                val id = packet.windowId
                val slot = packet.slotId
                val action = packet.actionNumber
                val item = packet.clickedItem?.displayName
                val button = packet.usedButton
                val mode = packet.mode
                modMessage("§lC0E:§r window: $id; action: $action; slot: $slot; item: $item; button: $button; mode: $mode")
            }
            else -> if (showAllSent.enabled) {
                if (packet !is C03PacketPlayer && packet !is C0FPacketConfirmTransaction && packet !is C00PacketKeepAlive)
                    modMessage(event.packet.javaClass.simpleName)
            }
        }
    }
}