package floppaclient.module.impl.render

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.util.Vec3

/**
 * Module to modify the camera behaviour.
 *
 * @author Aton
 */
object Camera : Module(
    "Camera",
    category = Category.RENDER,
    description = "Modifies the behaviour of the Camera."
){

    private val thridPDist = NumberSetting("Distance", 4.0, 1.0,10.0,0.1, description = "Distance of the third person view camera.")

    /**
     * Used in the Entity Renderer Mixin
     */
    val clip = BooleanSetting("Camera Clip", true, description = "Lets the camera clip through blocks.")

    init {
        this.addSettings(
            thridPDist,
            clip
        )
    }


    /**
     * Called by the EntityRendererMixin when it checks the thirdPersonDistance.
     * Returns the value of the setting when the module is enabled, null otherwise.
     */
    fun thirdPersonDistanceHook(): Float? {
        return if (this.enabled)
            thridPDist.value.toFloat()
        else
            null
    }

    /**
     * Redirected to by the EntityRendererMixin. Returns a high distance to clip the camera if that is enabled.
     */
    fun cameraClipHook(instance: Vec3, vec: Vec3): Double {
        return if (this.enabled && clip.enabled)
            thridPDist.value
        else
            instance.distanceTo(vec)
    }
}