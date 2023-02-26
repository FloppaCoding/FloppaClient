package floppaclient.module.impl.player

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ClipTools
import floppaclient.utils.GeometryUtils

object HClip : Module(
    "H Clip",
    category = Category.PLAYER,
    description = "Changes your position. From testing it seems like you need a y offset value of at least -1.7 and the maximum distance you can go is 7.9."
){
    private val hDist = NumberSetting("Distance", 7.5,0.0,9.9,0.1, description = "Horizontal distance.")
    private val yOffs = NumberSetting("Y Offset", -1.7,-5.0,2.0,0.1, description = "Vertical offset.")

    init {
        this.addSettings(
            hDist,
            yOffs
        )
    }

    override fun onKeyBind() {
        if (this.enabled) {
            ClipTools.hClip(hDist.value, GeometryUtils.yaw(), yOffs.value)
        }
    }
}