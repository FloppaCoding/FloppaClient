package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PacketSentEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent
import java.util.concurrent.LinkedBlockingQueue


object Blink : Module(
    "Blink",
    category = Category.PLAYER,
    description = "A"
) {

    private val pulse = BooleanSetting("Pulse", enabled = false, description = "Pulse packets.")
    private val pulseDelay = NumberSetting("Pulse Delay", default = 100.0, min = 10.0, max = 3000.0, description = "How often it pulses")
        .withDependency { this.pulse.enabled }

    init {
        this.addSettings(
           pulse,
           pulseDelay
        )
    }

    private val packets = LinkedBlockingQueue<Packet<INetHandlerPlayServer>>()

    private var nextPulse = System.currentTimeMillis()

    private var noBlink = false

    override fun onEnable() {
        if (mc.thePlayer == null) return
        super.onEnable()
        nextPulse = System.currentTimeMillis() + pulseDelay.value.toLong()
    }

    override fun onDisable() {
        if (mc.thePlayer == null) return
        blink()
        super.onDisable()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onPacket(event: PacketSentEvent) {
        val packet = event.packet
        if (mc.thePlayer == null || noBlink) return

        if (event.packet is C03PacketPlayer) {
            event.isCanceled = true
            packets.add(packet as Packet<INetHandlerPlayServer>)
        }
    }


    @SubscribeEvent
    fun onLivingUpdate(event: LivingUpdateEvent) {
        if (pulse.enabled) {
            if (nextPulse < System.currentTimeMillis()) {
                blink()
                nextPulse = System.currentTimeMillis() + pulseDelay.value.toLong()
            }
        }
    }

    private fun blink() {
        try {
            noBlink = true
            while (!packets.isEmpty()) {
                mc.netHandler.addToSendQueue(packets.take())
            }
            noBlink = false
        } finally {
            noBlink = false
        }
    }
}