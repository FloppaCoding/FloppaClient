package floppaclient.module.impl.render

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent

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

    private val thirdPDist = NumberSetting("Distance", 4.0, 1.0,10.0,0.1, description = "Distance of the third person view camera.")
    private val noFrontView = BooleanSetting("No Front View", false, description = "Skips the front view when toggling perspective.")

    /**
     * Used in the Entity Renderer Mixin
     */
    val clip = BooleanSetting("Camera Clip", true, description = "Lets the camera clip through blocks.")

    init {
        this.addSettings(
            thirdPDist,
            clip
        )
    }


    /**
     * Called by the EntityRendererMixin when it checks the thirdPersonDistance.
     * Returns the value of the setting when the module is enabled, null otherwise.
     */
    fun thirdPersonDistanceHook(): Float? {
        return if (this.enabled)
            thirdPDist.value.toFloat()
        else
            null
    }

    /**
     * Redirected to by the EntityRendererMixin. Returns a high distance to clip the camera if that is enabled.
     */
    fun cameraClipHook(instance: Vec3, vec: Vec3): Double {
        return if (this.enabled && clip.enabled)
            thirdPDist.value
        else
            instance.distanceTo(vec)
    }

    @SubscribeEvent
    fun onKey(event: InputEvent.KeyInputEvent) {
        if (!noFrontView.enabled) return
        /** adds +1 tp the perspective setting to skip front view, does not override the vanilla toggle
         * isKeyDown has to be used here instead of isKeyPressed because the vanilla function is called first.*/
        if (FloppaClient.mc.gameSettings.keyBindTogglePerspective.isKeyDown) {
            if(FloppaClient.mc.gameSettings.thirdPersonView >= 2) { //front view
                FloppaClient.mc.gameSettings.thirdPersonView = 0
            }
        }
    }

}