package floppaclient.module.impl.misc

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting

/**
 * This module makes blocks break faster.
 *
 * @author Aton
 * @see floppaclient.mixins.PlayerControllerMixin
 */
object FastMine : Module(
    "Fast Mine",
    category = Category.MISC,
    description = "Breaks blocks sooner when mining them allowing for effectively faster mining. Also has some options to reduce the delay in between block breaking."
){
    private val mode = SelectorSetting("Mode", "Vanilla", arrayListOf("Vanilla", "Skyblock", "None"), description = "Choose this according to where you are mining. In regions with custom mining like the Crystal Hollows select 'Skyblock' everywhere else select 'Vanilla'. Select 'None' if you only want to use this module to modify the softcap.")
    private val threshold = NumberSetting("Threshold", 0.7, 0.7, 1.0, 0.01, description = "Effectively reduces the time it takes to break the block by this factor.")
        .withDependency { this.mode.index == 0 }
    private val ticks = NumberSetting("Ticks", 20.0, 1.0, 100.0, 1.0, description = "The amount of ticks after which the block your are mining should break.")
        .withDependency { this.mode.index == 1 }
    private val modifyDelay = BooleanSetting("Modify Hit Delay", false, description = "Modifies the hit delay of 5 ticks before the next block can be mined. This should allow for bypassing the softcap.")
    private val newHitDelay = NumberSetting("New Delay", 0.0, 0.0, 5.0, 1.0, description = "New delay in ticks until the next block can be broken. The vanilla value is 5.")
        .withDependency { this.modifyDelay.enabled }
    private val noReset = BooleanSetting("No Reset", false, description = "Prevents the block breaking progress from resetting when the NBT data of the held item gets updated. This can happen because of the Compact enchant or drill fuel updating.")



    init {
        this.addSettings(
            mode,
            threshold,
            ticks,
            modifyDelay,
            newHitDelay,
            noReset
        )
    }

    /**
     * @see floppaclient.mixins.PlayerControllerMixin.tweakBlockDamage
     */
    fun getThreshold(): Float{
        return threshold.value.toFloat()
    }

    /**
     * @see floppaclient.mixins.PlayerControllerMixin.tweakBlockDamage
     */
    fun shouldTweakVanillaMining(): Boolean {
        return this.enabled && mode.isSelected("Vanilla")
    }

    /**
     * @see floppaclient.mixins.PlayerControllerMixin.preBreakBlock
     */
    fun shouldPreBreakBlock(miningTicks: Float, blockDamage: Float) : Boolean {
        return this.enabled && mode.isSelected("Skyblock") && blockDamage < 1f && miningTicks >= ticks.value.toFloat()
    }

    /**
     * @see floppaclient.mixins.PlayerControllerMixin.tweakHitDelay
     */
    fun shouldRemoveHitDelay(hitDelay: Int) : Boolean {
        return this.enabled && modifyDelay.enabled && hitDelay <= (5.0 - this.newHitDelay.value)
    }

    /**
     * @see floppaclient.mixins.PlayerControllerMixin.shouldTagsBeEqual
     */
    fun shouldPreventReset() : Boolean {
        return this.enabled && noReset.enabled
    }
}