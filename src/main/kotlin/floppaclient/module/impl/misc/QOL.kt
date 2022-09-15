package floppaclient.module.impl.misc

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import net.minecraft.potion.Potion

/**
 * Collection of general qol features.
 * @author Aton
 */
object QOL : Module(
    "QOL",
    category = Category.MISC,
    description = "Collection of general qol features."
) {
    private val noBlind = BooleanSetting("No Blindness", true, description = "Prevents the blindness effect from influencing your vision.")
    private val noBurn = BooleanSetting("No Fire Overlay", true, description = "Hides the burning overlay in first person.")
    private val noPushOut = BooleanSetting("No Push Out Block", true, description = "Prevents you from being pushed out of blocks.")
    private val noHeadInBlock = BooleanSetting("Cancel in Block", true, description = "Removes the in Block Overlay and prevents teh perspective from resetting when in a block.")
    private val noCarpet = BooleanSetting("No Carpet", true, description = "Removes carpet hitboxes.")

    init {
        this.addSettings(
            noBlind,
            noBurn,
            noPushOut,
            noHeadInBlock,
            noCarpet
        )
    }

    /**
     * Called by the EntityRendererMixin when it redirects the blindness check.
     */
    fun blindnessHook(): Boolean =
        if (this.enabled && noBlind.enabled) false else FloppaClient.mc.thePlayer.isPotionActive(Potion.blindness)

    /**
     * Referenced by the ItemRenderer Mixin to determine whether the fire overlay should be rendered.
     */
    fun shouldDisplayBurnOverlayHook(): Boolean =
        if (this.enabled && noBurn.enabled) false else FloppaClient.mc.thePlayer.isBurning

    /**
     * Referenced by the EntityPlayerSPMixin to determine whether the player should be pushed out of a block
     */
    fun preventPushOut(noClip: Boolean): Boolean {
        return noClip || (this.enabled && noPushOut.enabled)
    }

    /**
     * Referenced in the EntityPlayer Mixin to determine whether the head in block should be forced to be false.
     * This affects the perspective toggle in a block, the overlay and suffocation damage.
     */
    fun cancelHeadInBlock(): Boolean {
        return this.enabled && noHeadInBlock.enabled
    }

    /**
     * Referenced by the CarpetMixin to determine whether the bounding box should be set to 0.
     */
    fun ignoreCarpet(): Boolean {
        return this.enabled && noCarpet.enabled
    }
}