package floppaclient.module.impl.player

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ClipTools.dClip
import floppaclient.utils.GeometryUtils.pitch
import floppaclient.utils.GeometryUtils.yaw

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

    override fun onKeyBind() {
        if (this.enabled) {
            if (allow3DClip.enabled) {
                dClip(clipDistance.value, yaw(), pitch())
            }else {
                dClip(clipDistance.value)
            }
        }
    }
}