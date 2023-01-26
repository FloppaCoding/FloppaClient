package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PacketSentEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent
import net.minecraftforge.event.world.WorldEvent
import java.util.concurrent.LinkedBlockingQueue


object Blink : Module(
    "Blink",
    category = Category.PLAYER,
    description = "Temporarily stops sending movement packets, might flag"
) {

    private val pulse = BooleanSetting("Pulse", enabled = false, description = "Pulse packets.")
    private val pulseDelay =
        NumberSetting("Pulse Delay", default = 100.0, min = 10.0, max = 1000.0, description = "How often it pulses")
            .withDependency { this.pulse.enabled }
    private val test = BooleanSetting("test", false, description = "test")
    private val debug = BooleanSetting(
        "Debug Messages",
        enabled = false,
        Visibility.ADVANCED_ONLY,
        description = "Sends Debug messages"
    )

    init {
        this.addSettings(
            pulse,
            pulseDelay,
            test,
            debug
        )
    }

    val packets = LinkedBlockingQueue<Packet<INetHandlerPlayServer>>()

    private var nextPulse = System.currentTimeMillis()

    var noLag = false

    override fun onEnable() {
        if (mc.thePlayer == null) return
        super.onEnable()
        nextPulse = System.currentTimeMillis() + pulseDelay.value.toLong()
    }

    override fun onDisable() {
        if (mc.thePlayer == null) return
        sendPackets()
        super.onDisable()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onPacket(event: PacketSentEvent) {
        val packet = event.packet
        if (mc.thePlayer == null || noLag) return

        if (packet is C03PacketPlayer) {
            event.isCanceled = true
            packets.add(packet as Packet<INetHandlerPlayServer>)
        }
    }


    @SubscribeEvent
    fun onLivingUpdate(event: LivingUpdateEvent) {
        if (pulse.enabled) {
            if (nextPulse < System.currentTimeMillis()) {
                sendPackets()
                nextPulse = System.currentTimeMillis() + pulseDelay.value.toLong()
            }
        }
    }

    fun sendPackets() {
        try {
            noLag = true
            while (!packets.isEmpty()) {
                mc.netHandler.addToSendQueue(packets.take())
            }
            noLag = false
            if (debug.enabled) return modMessage("Send Packets")
        } finally {
            noLag = false
        }
    }

    @SubscribeEvent
    fun onWarp(event: WorldEvent.Load) {
        packets.clear()
    }
}