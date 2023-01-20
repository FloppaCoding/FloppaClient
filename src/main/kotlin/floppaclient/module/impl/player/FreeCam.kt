package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PacketSentEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.world.World
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Leave your body
 */
object FreeCam : Module(
    "Free Cam",
    category = Category.PLAYER,
    description = "Allows you to fly with no clip and stops sending position packets to the server."
){
    private var fakePlayer: EntityOtherPlayerMP? = null

    private val speed = NumberSetting("Speed", 3.0, 0.1, 5.0, 0.1, description = "Fly speed.")
    private val glide = BooleanSetting("Glide", false, description = "Lets you glide upon release movement keys.")
    private val reloadChunks = BooleanSetting("Reload Chunks", false, description = "Reloads all chunks on disable.")


    init {
        this.addSettings(
            speed,
            glide,
            reloadChunks
        )
    }

    override fun onEnable() {
        super.onEnable()
        if (mc.theWorld == null) return
        clonePlayer()
    }

    override fun onDisable() {
        super.onDisable()
        if (mc.thePlayer == null || mc.theWorld == null || fakePlayer == null) return
        mc.thePlayer.noClip = false
        resetPlayer()
        if (reloadChunks.enabled) {
            mc.renderGlobal.loadRenderers()
        }
    }

    @SubscribeEvent
    fun onLivingUpdate(event: LivingUpdateEvent) {
        mc.thePlayer.noClip = true
        mc.thePlayer.onGround = false
        mc.thePlayer.capabilities.isFlying = false
        mc.thePlayer.fallDistance = 0.0f

        // First handle horizontal movement.
        // When not moving set the momentum to 0 to cancel the glide
        if (mc.thePlayer.moveForward == 0.0f && mc.thePlayer.moveStrafing == 0.0f && !glide.enabled) {
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.motionX = 0.0
        }
        val speed = speed.value * 0.1
        // This value will be used for the movement speed, because the player is not flying but also not on ground
        mc.thePlayer.jumpMovementFactor = speed.toFloat()
        // Now handle vertical movement
        mc.thePlayer.motionY = 0.0
        if (mc.gameSettings.keyBindJump.isKeyDown) mc.thePlayer.motionY += speed * 3.0
        if (mc.gameSettings.keyBindSneak.isKeyDown) mc.thePlayer.motionY -= speed * 3.0
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        this.toggle()
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (event.packet is C03PacketPlayer) event.isCanceled = true
    }

    /**
     * Creates a clone of the player, which will stay where the player was before activation free cam.
     */
    private fun clonePlayer() {
        fakePlayer = EntityOtherPlayerMP(mc.theWorld as World, mc.thePlayer.gameProfile)
        fakePlayer!!.copyLocationAndAnglesFrom(mc.thePlayer)
        fakePlayer!!.rotationYawHead = mc.thePlayer.rotationYawHead
        fakePlayer!!.onGround = mc.thePlayer.onGround
        fakePlayer!!.inventory = mc.thePlayer.inventory
        mc.theWorld.addEntityToWorld(-33574, fakePlayer)
    }

    /**
     * Resets teh player to where he was before activating freecam and removes the clone from the world.
     */
    private fun resetPlayer() {
        mc.thePlayer.setPosition(fakePlayer!!.posX, fakePlayer!!.posY, fakePlayer!!.posZ)
        fakePlayer = null
        mc.thePlayer.setVelocity(0.0, 0.0, 0.0)
        mc.theWorld.removeEntityFromWorld(-33574)
    }
}