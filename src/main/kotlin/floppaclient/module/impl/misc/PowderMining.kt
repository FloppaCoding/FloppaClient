package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ReceivePacketEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.GeometryUtils
import net.minecraft.block.BlockChest.FACING
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.*
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.math.RoundingMode

object PowderMining : Module(
    "Powder Mine",
    category = Category.MISC,
    description = "Powder Mine"
) {
    private val autoSolve = BooleanSetting("Auto Open", true, Visibility.ADVANCED_ONLY, description = "Auto opens crystal hollow treasure chests.")
    private val cancelParticle = BooleanSetting("Cancel Particle", false, Visibility.ADVANCED_ONLY, description = "Hides the particles from the chest")
    private val cancelChat = BooleanSetting("Hide messages", false, description = "Hides messages from opening a treasure chest")

    init {
        addSettings(
            autoSolve,
            cancelParticle,
            cancelChat,
        )
    }

    private var particlePos = Vec3(0.0, -1.0, 0.0)
    private var coordinateThing = 0.0

    @SubscribeEvent(receiveCanceled = true)
    fun onPacket(event: ReceivePacketEvent) {
        val packet = event.packet

        if (packet !is S2APacketParticles) return
        if (packet.particleType.equals(EnumParticleTypes.CRIT)) {

            val x = packet.xCoordinate
            val y = packet.yCoordinate - mc.thePlayer.eyeHeight
            val z = packet.zCoordinate

            val xRounded = x.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
            val zRounded = z.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()

            if (xRounded == coordinateThing || zRounded == coordinateThing) {
                particlePos = Vec3(x, y, z)
                if (cancelParticle.enabled) event.isCanceled = true
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChat(event: ClientChatReceivedEvent) {
        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        when {
            text.startsWith("You received") && cancelChat.enabled -> event.isCanceled = true
            text.startsWith("You have successfully picked the lock on this chest!") -> {
                particlePos = Vec3(0.0, -1.0, 0.0)
                if (cancelChat.enabled) event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LivingEvent.LivingUpdateEvent) {
        if (mc.thePlayer == null || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
        getChestRotation(mc.objectMouseOver.blockPos)

        if (particlePos.yCoord == -1.0) return
        val direction = GeometryUtils.getDirection(mc.thePlayer.positionVector, particlePos)

        if (autoSolve.enabled && direction[0] < 4.5) {
            mc.thePlayer.rotationYaw = direction[1].toFloat()
            mc.thePlayer.rotationPitch = direction[2].toFloat()
        }
    }

    private fun getChestRotation(blockPos: BlockPos) {
        val state = mc.theWorld.getBlockState(blockPos)
        if (state.block != Blocks.chest) return

        coordinateThing = when (state.getValue(FACING)) {

            EnumFacing.NORTH -> blockPos.z - 0.1
            EnumFacing.SOUTH -> blockPos.z + 1.1
            EnumFacing.EAST -> blockPos.x + 1.1
            EnumFacing.WEST -> blockPos.x - 0.1
            else -> return
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        particlePos = Vec3(0.0, -1.0, 0.0)
        coordinateThing = 0.0
    }
}