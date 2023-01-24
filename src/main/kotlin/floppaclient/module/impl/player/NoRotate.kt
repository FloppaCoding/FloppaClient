package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ReceivePacketEvent
import floppaclient.events.TeleportEventPre
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.Utils.isHolding
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S07PacketRespawn
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Cancels the rotation in teleport packets, but sends the correct response to the server.
 * @author Aton.
 */
object NoRotate : Module(
    "No Rotate",
    category = Category.PLAYER,
    description = "Prevents rotation on received teleport packets."
){
    /**
     * Option to toggle no rotate on packets with 0 pitch. Those are used for special teleports which in general should
     * rotate you.
     */
    private val pitch = BooleanSetting("0 Pitch", false, description = "Also prevents rotation of packets with 0 pitch, those are in general used for teleport which should rotate you..")
    private val keepMotion = BooleanSetting("Keep Motion", false, description = "Teleporting will not reset your horizontal motion.")
    private val stopOnHopper = BooleanSetting("Stop on Hopper", false, description = "Teleporting onto a hopper will stop your movement. Press your walk keys again to move again.")
    private val clipInHopper = BooleanSetting("Clip into Hopper", false, description = "Will directly place you inside of a hopper when you teleport onto it.")
    private val stopMotionWithPearl = BooleanSetting("Stop with Pearl", true, description = "Stops keep motion when holding a Pearl.")
        .withDependency { this.keepMotion.enabled }

    private var doneLoadingTerrain = false

    private val moveBinds: List<KeyBinding>
        get() {
            return listOf(
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindBack
            )
        }

    init {
        this.addSettings(
            pitch,
            keepMotion,
            stopOnHopper,
            clipInHopper,
            stopMotionWithPearl
        )
    }

    /**
     * Intercepts the teleport packet and preforms custom handling of the packet if conditions are met.
     * When custom teleport handling is performed the event gets cancelled which prevents the vanilla mc processing the
     * packet.
     * This must not receive cancelled events, so that if different handling is implemented somewhere else this one can
     * be cancelled and only one will activate.
     */
    @SubscribeEvent
    fun onTeleportPacket(event: TeleportEventPre) {
        if (mc.thePlayer != null && ((event.packet).pitch != 0.0f || this.pitch.enabled)) {


            // At this point no rotate is active

            event.isCanceled = true

            val stopMotion = !keepMotion.enabled || (stopMotionWithPearl.enabled && mc.thePlayer.isHolding("Ender Pearl"))


            val packetIn = event.packet
            val entityplayer: EntityPlayer = mc.thePlayer
            var d0: Double = packetIn.x
            var d1: Double = packetIn.y
            var d2: Double = packetIn.z
            var f: Float = packetIn.yaw
            var f1: Float = packetIn.pitch

            if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X)) {
                d0 += entityplayer.posX
            } else {
                if (stopMotion) {
                    entityplayer.motionX = 0.0
                }
            }

            if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
                d1 += entityplayer.posY
            } else {
                entityplayer.motionY = 0.0
            }

            if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
                d2 += entityplayer.posZ
            } else {
                if (stopMotion) {
                    entityplayer.motionZ = 0.0
                }
            }

            if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
                f1 += entityplayer.rotationPitch
            }

            if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
                f += entityplayer.rotationYaw
            }

            // Stop on Hopper Part

            var hopperClip = false
            if (stopOnHopper.enabled || clipInHopper.enabled){
                val pos = BlockPos(d0, d1, d2).down()
                if (mc.theWorld.getBlockState(pos).block === Blocks.hopper) {
                    if (stopOnHopper.enabled) {
                        mc.thePlayer.setVelocity(0.0, 0.0, 0.0)
                        moveBinds.forEach {
                            KeyBinding.setKeyBindState(it.keyCode, false)
                        }
                    }
                    if (clipInHopper.enabled){
                        hopperClip = true
                    }
                }
            }

            // Norotate Part
            entityplayer.setPosition(d0, d1, d2)
            val fakeYaw = f % 360.0f
            val fakePitch = f1 % 360.0f

            mc.netHandler.networkManager.sendPacket(
                C06PacketPlayerPosLook(
                    entityplayer.posX,
                    entityplayer.entityBoundingBox.minY,
                    entityplayer.posZ,
                    fakeYaw,
                    fakePitch,
                    false
                )
            )

            if (hopperClip){
                entityplayer.setPosition(d0, d1-0.3, d2)
            }

            if (!this.doneLoadingTerrain) {
                mc.thePlayer.prevPosX = mc.thePlayer.posX
                mc.thePlayer.prevPosY = mc.thePlayer.posY
                mc.thePlayer.prevPosZ = mc.thePlayer.posZ
                this.doneLoadingTerrain = true
                mc.displayGuiScreen(null as GuiScreen?)
            }
        }
        doneLoadingTerrain = true
    }


    @SubscribeEvent(receiveCanceled = true)
    fun onRespawn(event: ReceivePacketEvent) {
        if (event.packet is S07PacketRespawn) {
            doneLoadingTerrain = false
        }
    }
}