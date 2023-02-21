package floppaclient.module.impl.dev

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PacketSentEvent
import floppaclient.events.ReceivePacketEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.network.login.server.S02PacketLoginSuccess
import net.minecraft.network.play.client.C00PacketKeepAlive
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.*
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DevModule : Module(
    "Dev Module",
    category = Category.MISC,
) {

    private val showAllOtherSent = BooleanSetting("All other sent", false)
    private val showC0E = BooleanSetting("C0E", false, description = "C0EPacketClickWindow")
    private val showC03 = BooleanSetting("C03", false, description = "C03PacketPlayer")

    private val showAllOtherReceived = BooleanSetting("All other received", false)
    private val showS1B = BooleanSetting("S1B", false, description = "S1BPacketEntityAttach")
    private val showS1C = BooleanSetting("S1C", false, description = "S1CPacketEntityMetadata")
    private val showS2A = BooleanSetting("S2A", false, description = "S2APacketParticles")
    private val showS02 = BooleanSetting("S02", false, description = "S02PacketChat; S02PacketLoginSuccess")
    private val showS03 = BooleanSetting("S03", false, description = "S03PacketTimeUpdate")
    private val showS08 = BooleanSetting("S08", false, description = "S08PacketPlayerPosLook")
    private val showS12 = BooleanSetting("S12", false, description = "S12PacketEntityVelocity")
    private val showS13 = BooleanSetting("S13", false, description = "S13PacketDestroyEntities")
    private val showS14 = BooleanSetting("S14", false, description = "S14PacketEntity")
    private val showS18 = BooleanSetting("S18", false, description = "S18PacketEntityTeleport")
    private val showS19 = BooleanSetting("S19", false, description = "S19PacketEntityHeadLook; S19PacketEntityStatus")
    private val showS23 = BooleanSetting("S23", false, description = "S23PacketBlockChange")
    private val showS29 = BooleanSetting("S29", false, description = "S29PacketSoundEffect")
    private val showS32 = BooleanSetting("S32", false, description = "S32PacketConfirmTransaction")
    private val showS45 = BooleanSetting("S45", false, description = "S45PacketTitle")

    init {
        this.addSettings(
            showAllOtherSent,
            showC0E,
            showC03,
            showAllOtherReceived,
            showS1B,
            showS1C,
            showS2A,
            showS03,
            showS08,
            showS12,
            showS13,
            showS14,
            showS18,
            showS19,
            showS23,
            showS29,
            showS32,
            showS45,
        )
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPacketSent(event: PacketSentEvent) {
        if (mc.thePlayer == null) return
        when(val packet = event.packet) {
            is C0EPacketClickWindow -> if (showC0E.enabled) {
                val id = packet.windowId
                val slot = packet.slotId
                val action = packet.actionNumber
                val item = packet.clickedItem?.displayName
                val button = packet.usedButton
                val mode = packet.mode
                modMessage("§lC0E:§r window: $id; action: $action; slot: $slot; item: $item; button: $button; mode: $mode")
            }
            is C03PacketPlayer -> if (showC03.enabled) {
                val type = packet.javaClass.simpleName
                val x = packet.positionX
                val y = packet.positionY
                val z = packet.positionZ
                val yaw = packet.yaw
                val pitch = packet.pitch
                val onGround = packet.isOnGround
                val moving = packet.isMoving
                val rotating = packet.rotating
                modMessage("§lC03:§r type=$type; $x / $y / $z ($yaw | $pitch); onGround=$onGround; moving=$moving; rotating=$rotating")
            }
            else -> if (showAllOtherSent.enabled) {
                if (packet !is C0FPacketConfirmTransaction && packet !is C00PacketKeepAlive)
                    modMessage(event.packet.javaClass.simpleName)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onPacketReceived(event: ReceivePacketEvent) {
        if (mc.thePlayer == null) return

        when(val packet = event.packet) {
            is S08PacketPlayerPosLook -> if (showS08.enabled) {
                val x = packet.x
                val y = packet.y
                val z = packet.z
                val yaw = packet.yaw
                val pitch = packet.pitch
                val flags: String = packet.func_179834_f().joinToString(",") { it.name }
                modMessage("§lC03:§r $x / $y / $z ($yaw | $pitch); flags=$flags")
            }
            is S00PacketKeepAlive -> {}
            is S1BPacketEntityAttach -> if (showS1B.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S1CPacketEntityMetadata -> if (showS1C.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S2APacketParticles -> if (showS2A.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S02PacketChat, is S02PacketLoginSuccess -> if (showS02.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S03PacketTimeUpdate -> if (showS03.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S12PacketEntityVelocity -> if (showS12.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S13PacketDestroyEntities -> if (showS13.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S14PacketEntity -> if (showS14.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S18PacketEntityTeleport -> if (showS18.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S19PacketEntityHeadLook, is S19PacketEntityStatus -> if (showS19.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S23PacketBlockChange -> if (showS23.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S29PacketSoundEffect -> if (showS29.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S32PacketConfirmTransaction -> if (showS32.enabled) {
                modMessage(packet.javaClass.simpleName)
            }
            is S45PacketTitle -> if (showS45.enabled) {
                modMessage(packet.javaClass.simpleName)
            }

            else -> if (showAllOtherReceived.enabled) {
                modMessage(event.packet.javaClass.simpleName)
            }
        }
    }
}