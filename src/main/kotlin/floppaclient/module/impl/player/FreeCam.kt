package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PacketSentEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.Setting.Companion.withInputTransform
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSelectorSetting
import floppaclient.utils.ChatUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.MathHelper
import net.minecraft.world.World
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * Leave your body
 *
 * This module has two operation modes which can both achieve a free-cam behaviour.
 *
 * @author Aton
 * @see floppaclient.mixins.render.EntityRendererMixin Camera Positioning, Tweaking  [objectMouseOver][net.minecraft.client.Minecraft.objectMouseOver]
 * @see floppaclient.mixins.render.ChunkRendererWorkerMixin Render chunks around the camera
 * @see floppaclient.mixins.render.RenderGlobalMixin Render the correct entities.
 * @see floppaclient.mixins.render.RenderPlayerMixin Makes sure that the player character gets rendered.
 * @see floppaclient.mixins.MovementInputFromOptionsMixin Intercepts movement inputs and enabled arrow key movement.
 */
object FreeCam : Module(
    "Free Cam",
    category = Category.PLAYER,
    description = "Allows you to freely move the camera in two different modes.\n" +
            "${ChatUtils.BOLD}${ChatUtils.DARK_AQUA}True Free Cam${ChatUtils.RESET} Mode lets you move the camera independently from the player character. " +
            "Communication with the server is not affected and the character you see is not a clone but the actual placer character. " +
            "You can still move your character using the arrow keys while in True Free Fam Mode. " +
            "The command ${ChatUtils.ITALIC}${ChatUtils.YELLOW}/fc freewalk${ChatUtils.RESET} freezes the camera but lets you move your character normally.\n" +
            "${ChatUtils.BOLD}${ChatUtils.BLUE}Ping Spoof${ChatUtils.RESET} Mode allows you to fly with no clip and stops sending position packets to the server. " +
            "A clone of the player character will be placed in its position. \n" +
            "${ChatUtils.RED}Ping Spoof Mode is not recommended!${ChatUtils.RESET} It does change the way the client communicates with the server."
){
    private val mode = StringSelectorSetting("Mode", "True Free Cam", arrayListOf("True Free Cam", "Ping Spoof"),
        description =
                "${ChatUtils.BOLD}${ChatUtils.DARK_AQUA}True Free Cam${ChatUtils.RESET} Mode lets you move the camera without moving the player character. " +
                "Communication with the server is not affected and the character you see is not a clone but the actual placer character.\n" +
                "${ChatUtils.BOLD}${ChatUtils.BLUE}Ping Spoof${ChatUtils.RESET} Mode allows you to fly with no clip and stops sending position packets to the server. " +
                "A clone of the player character will be placed in its position."
    ).withInputTransform {input, setting ->
        // Prevent the mode from being changed while free cam is active.
        if (FreeCam.enabled) setting.index
        else input
    }
    private val speed = NumberSetting("Speed", 3.0, 0.1, 5.0, 0.1, description = "Fly speed.")
    private val glide = BooleanSetting("Glide", false, description = "Lets you glide upon release movement keys.")
        .withDependency { mode.isSelected("Ping Spoof") }
    private val tweakTarget = BooleanSetting("Use Camera Target", true, description = "Use the camera position to determine what the player is looking at. If disabled the targeted block will be determined by what your character is looking at and will not change when you move the camera.")
        .withDependency { mode.isSelected("True Free Cam") }
    private val reloadChunks = BooleanSetting("Reload Chunks", false, description = "Reloads all chunks on disable.")

    /** Used to clone the player for Ping Spoof Mode. This entity will be visible as the player. */
    private var fakePlayer: EntityOtherPlayerMP? = null
    /**
     * This entitly is used as a convenient way of storing camera position data for True Free Cam Mode.
     * It replaces the [renderViewEntity][net.minecraft.client.Minecraft.renderViewEntity] in some places.
     * @see tweakRenderViewEntityHook
     */
    private var viewEntity: EntityOtherPlayerMP? = null

    private var canControlCharacter = false

    init {
        this.addSettings(
            mode,
            speed,
            glide,
            tweakTarget,
            reloadChunks
        )
    }

    override fun onEnable() {
        if(mc.thePlayer == null || mc.theWorld == null) {
            toggle()
            return
        }
        super.onEnable()
        when(mode.selected) {
            "True Free Cam" -> setupViewEntity()
            "Ping Spoof" -> clonePlayer()
        }
    }

    override fun onDisable() {
        super.onDisable()
        if (mc.thePlayer == null || mc.theWorld == null) return
        when(mode.selected) {
            "True Free Cam" -> resetViewEntity()
            "Ping Spoof" -> resetPlayer()
        }
        if (reloadChunks.enabled) {
            mc.renderGlobal.loadRenderers()
        }
    }

    /**
     * Used in mixins to determine whether the [renderViewEntity][net.minecraft.client.Minecraft.renderViewEntity] should be tweaked.
     * @return true when the module is [enabled] and True Free Cam [mode] is selected.
     * @see tweakRenderViewEntityHook
     * @see floppaclient.mixins.render.EntityRendererMixin
     * @see floppaclient.mixins.render.ChunkRendererWorkerMixin
     * @see floppaclient.mixins.render.RenderGlobalMixin
     * @see floppaclient.mixins.render.RenderPlayerMixin
     */
    fun shouldTweakViewEntity(): Boolean {
        return this.enabled && mode.isSelected("True Free Cam")
    }

    /**
     * Used in mixins to determine whether movement inputs should be intercepted to only affect the camera.
     * Also used to enable the use of the arrow keys for walking.
     * @return true when the module is [enabled], True Free Cam [mode] is selected and [canControlCharacter] is disabled.
     * @see floppaclient.mixins.render.EntityRendererMixin
     * @see floppaclient.mixins.MovementInputFromOptionsMixin
     */
    fun shouldTweakMovement(): Boolean {
        return this.enabled && !canControlCharacter && mode.isSelected("True Free Cam")
    }

    /**
     * Used in mixins to determine whether [objectMouseOver][net.minecraft.client.Minecraft.objectMouseOver] should be tweaked.
     * @return true when the module is [enabled], [tweakTarget] is enabled and True Free Cam [mode] is selected.
     * @see floppaclient.mixins.render.EntityRendererMixin.tweakRenderViewEntityMouseOver
     * @see floppaclient.mixins.render.EntityRendererMixin.tweakMouseOver
     */
    fun shouldTweakLookingAt(): Boolean{
        return this.enabled && tweakTarget.enabled && mode.isSelected("True Free Cam")
    }

    /**
     * Used in mixins to replace the [renderViewEntity][net.minecraft.client.Minecraft.renderViewEntity].
     * @return [viewEntity] if it is not null, [renderViewEntity][net.minecraft.client.Minecraft.renderViewEntity] otherwise.
     * @see shouldTweakViewEntity
     * @see floppaclient.mixins.render.EntityRendererMixin
     * @see floppaclient.mixins.render.ChunkRendererWorkerMixin
     * @see floppaclient.mixins.render.RenderGlobalMixin
     * @see floppaclient.mixins.render.RenderPlayerMixin
    */
    fun tweakRenderViewEntityHook(): Entity {
        return viewEntity ?: mc.renderViewEntity
    }

    /**
     * Sets the angles of [viewEntity].
     */
    fun setViewAngles(yaw: Float, pitch: Float) {
        viewEntity?.prevRotationYaw   = viewEntity!!.rotationYaw
        viewEntity?.prevRotationPitch = viewEntity!!.rotationPitch
        viewEntity?.setAngles(yaw, pitch)
    }

    /**
     * Toggles [canControlCharacter].
     * @return the new state of [canControlCharacter]
     */
    fun toggleControlCharacter(): Boolean {
        canControlCharacter = !canControlCharacter
        return canControlCharacter
    }

    /**
     * Updates the camera position when True Free Cam [mode] is active.
     */
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!mode.isSelected("True Free Cam") || event.phase != TickEvent.Phase.START) return
        if (canControlCharacter) return

        val moveSpeed = speed.value.toFloat() * 0.5f
        moveViewEntity(moveSpeed)
    }

    /**
     * Tweaks the player movement for Ping Spoof [mode].
     */
    @SubscribeEvent
    fun onLivingUpdate(event: LivingUpdateEvent) {
        if (!mode.isSelected("Ping Spoof") || event.entity != mc.thePlayer) return
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

    /**
     * Disable on warp.
     */
    @SubscribeEvent
    fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent.Load) {
        this.toggle()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPacket(event: PacketSentEvent) {
        if (!mode.isSelected("Ping Spoof")) return
        if (event.packet is C03PacketPlayer) {
            event.isCanceled = true
        }
    }

    /**
     * Moves the camera ([viewEntity]) based on key inputs.
     */
    private fun moveViewEntity(moveSpeed: Float) {
        if(viewEntity == null) return
        viewEntity!!.lastTickPosX = viewEntity!!.posX
        viewEntity!!.lastTickPosY = viewEntity!!.posY
        viewEntity!!.lastTickPosZ = viewEntity!!.posZ

        viewEntity!!.prevPosX = viewEntity!!.posX
        viewEntity!!.prevPosY = viewEntity!!.posY
        viewEntity!!.prevPosZ = viewEntity!!.posZ

        var moveStrafe = 0.0f
        var moveForward = 0.0f

        if (mc.gameSettings.keyBindForward.isKeyDown) {
            ++moveForward
        }

        if (mc.gameSettings.keyBindBack.isKeyDown) {
            --moveForward
        }

        if (mc.gameSettings.keyBindLeft.isKeyDown) {
            ++moveStrafe
        }

        if (mc.gameSettings.keyBindRight.isKeyDown) {
            --moveStrafe
        }

        var f = moveStrafe * moveStrafe + moveForward * moveForward
        if (f >= 1.0E-4f) {
            f = MathHelper.sqrt_float(f)
            if (f < 1.0f) {
                f = 1.0f
            }
            f = moveSpeed / f
            moveStrafe *= f
            moveForward *= f
            val yaw = viewEntity!!.rotationYaw
            val f1 = MathHelper.sin(yaw * Math.PI.toFloat() / 180.0f)
            val f2 = MathHelper.cos(yaw * Math.PI.toFloat() / 180.0f)
            viewEntity!!.posX += (moveStrafe * f2 - moveForward * f1).toDouble()
            viewEntity!!.posZ += (moveForward * f2 + moveStrafe * f1).toDouble()
        }

        if (mc.gameSettings.keyBindJump.isKeyDown)  viewEntity!!.posY += moveSpeed * 1.0
        if (mc.gameSettings.keyBindSneak.isKeyDown) viewEntity!!.posY -= moveSpeed * 1.0
    }

    private fun setupViewEntity() {
        viewEntity = EntityOtherPlayerMP(mc.theWorld as World, mc.thePlayer.gameProfile)
        viewEntity!!.copyLocationAndAnglesFrom(mc.thePlayer)

        canControlCharacter = false

        mc.thePlayer.movementInput.moveForward = 0f
        mc.thePlayer.movementInput.moveStrafe = 0f
        mc.thePlayer.movementInput.jump = false
        mc.thePlayer.movementInput.sneak = false
        mc.thePlayer.moveForward = 0f
        mc.thePlayer.moveStrafing = 0f
        mc.thePlayer.setJumping(false)
    }

    private fun resetViewEntity() {
        viewEntity = null
    }


    /**
     * Creates a clone of the player, which will stay where the player was before activating free cam.
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
     * Resets the player to where he was before activating free cam and removes the clone from the world.
     */
    private fun resetPlayer() {
        mc.thePlayer.noClip = false
        if (fakePlayer == null) return
        mc.thePlayer.setPosition(fakePlayer!!.posX, fakePlayer!!.posY, fakePlayer!!.posZ)
        fakePlayer = null
        mc.thePlayer.setVelocity(0.0, 0.0, 0.0)
        mc.theWorld.removeEntityFromWorld(-33574)
    }
}