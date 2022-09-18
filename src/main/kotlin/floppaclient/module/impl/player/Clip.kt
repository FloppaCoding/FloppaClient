package floppaclient.module.impl.player

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clip module for changing the player position client side.
 * @author Aton
 */
object Clip : Module(
    "3D Clip",
    category = Category.PLAYER,
    description = "Clips you with the specified settings."
){
    private val allow3DClip  = BooleanSetting("Use 3D Clip", true, description = "Toggles between purely horizontal and omnidirectional clip for the clip key bind.")
    private val clipDistance = NumberSetting("Clip Distance",9.5, 0.0, 10.0,0.1, description = "Distance that the clip key bind will teleport you. Default 9.5.")

    init {
        this.addSettings(
            allow3DClip,
            clipDistance
        )
    }

    override fun keyBind() {
        if (this.enabled) {
            if (allow3DClip.enabled) {
                dClip(clipDistance.value, yaw(), pitch())
            }else {
                dClip(clipDistance.value)
            }
        }
    }

    private fun sinDeg(alpha: Float): Double {
        return sin(alpha*Math.PI/180)
    }

    private fun cosDeg(alpha: Float): Double {
        return cos(alpha*Math.PI/180)
    }

    fun yaw(): Float {
        return FloppaClient.mc.thePlayer.rotationYaw % 360F
    }

    fun pitch(): Float {
        return FloppaClient.mc.thePlayer.rotationPitch
    }

    fun dClip(dist: Double, yaw: Float = yaw(), pitch: Float = 0.0f) {
        clip(-sinDeg(yaw)*cosDeg(pitch)*dist, -sinDeg(pitch)*dist, cosDeg(yaw)*cosDeg(pitch)*dist)
    }

    fun hClip(dist: Double, yaw: Float = yaw(), yOffs: Double = 0.0) {
        clip(-sinDeg(yaw)*dist, yOffs, cosDeg(yaw)*dist)
    }

    /**
     * Teleport relative to the current position.
     */
    fun clip(x: Double, y: Double, z: Double) {
        teleport(FloppaClient.mc.thePlayer.posX + x, FloppaClient.mc.thePlayer.posY + y, FloppaClient.mc.thePlayer.posZ + z)
    }

    /**
     * Teleport to the specified coordinates.
     */
    fun teleport(x: Double, y: Double, z: Double) {
        // check whether inputs are NaN to prevent kick
        if(x.isNaN() || y.isNaN() || z.isNaN()) {
            Utils.modMessage("§cArgument error")
        }else {
            FloppaClient.mc.thePlayer.setPosition(x, y, z)
        }
    }
}