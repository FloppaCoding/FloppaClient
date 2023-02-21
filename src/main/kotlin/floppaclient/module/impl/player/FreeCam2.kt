package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ChatUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.util.MathHelper
import net.minecraft.world.World
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

/**
 * Leave your body
 */
object FreeCam2 : Module(
    "Free Cam2",
    category = Category.PLAYER,
    description = "Allows you to move the camera independently from the character.\n" +
            "Move your character using the arrow keys while in free cam. " +
            "The command ${ChatUtils.ITALIC}\"fc freewalk\"${ChatUtils.RESET} freezes the camera but lets you move your character normally."
){
    private val speed = NumberSetting("Speed", 3.0, 0.1, 5.0, 0.1, description = "Fly speed.")
    private val tweakTarget = BooleanSetting("Use Camera Target", true, description = "Use the camera position to determine what the player is looking at. If disabled the targeted block will be determined by what your character is looking at and will not change when you move the camera.")

    private var viewEntity: EntityOtherPlayerMP? = null

    private var canControlCharacter = false

    init {
        this.addSettings(
            speed,
            tweakTarget,
        )
    }

    override fun onEnable() {
        if(mc.thePlayer == null || mc.theWorld == null) {
            toggle()
            return
        }
        super.onEnable()
        setupViewEntity()

        canControlCharacter = false

        mc.thePlayer.movementInput.moveForward = 0f
        mc.thePlayer.movementInput.moveStrafe = 0f
        mc.thePlayer.movementInput.jump = false
        mc.thePlayer.movementInput.sneak = false
        mc.thePlayer.moveForward = 0f
        mc.thePlayer.moveStrafing = 0f
        mc.thePlayer.setJumping(false)
    }

    override fun onDisable() {
        super.onDisable()
        if (mc.thePlayer == null || mc.theWorld == null) return
        resetViewEntity()
    }

    fun isFreecamActive(): Boolean {
        return this.enabled
    }

    fun shouldTweakMovement(): Boolean {
        return this.enabled && !canControlCharacter
    }

    fun shouldTweakLookingAt(): Boolean{
        return this.enabled && tweakTarget.enabled
    }

    fun tweakRenderViewEntityHook(): Entity {
        return viewEntity ?: mc.renderViewEntity
    }

    fun setViewAngles(yaw: Float, pitch: Float) {
        viewEntity?.prevRotationYaw = viewEntity!!.rotationYaw
        viewEntity?.prevRotationPitch = viewEntity!!.rotationPitch
        viewEntity?.setAngles(yaw, pitch)
    }

    fun toggleControlCharacter(): Boolean {
        canControlCharacter = !canControlCharacter
        return canControlCharacter
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (canControlCharacter) return

        val moveSpeed = speed.value.toFloat() * 0.5f
        moveViewEntity(moveSpeed)
    }

    @SubscribeEvent
    fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent.Load) {
        this.toggle()
    }

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

        if (mc.gameSettings.keyBindJump.isKeyDown) viewEntity!!.posY += moveSpeed * 1.0
        if (mc.gameSettings.keyBindSneak.isKeyDown) viewEntity!!.posY -= moveSpeed * 1.0
    }

    private fun setupViewEntity() {
        viewEntity = EntityOtherPlayerMP(mc.theWorld as World, mc.thePlayer.gameProfile)
        viewEntity!!.copyLocationAndAnglesFrom(mc.thePlayer)
    }

    private fun resetViewEntity() {
        viewEntity = null
    }
}