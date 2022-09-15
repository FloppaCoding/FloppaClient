package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils
import net.minecraft.util.MathHelper
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

/**
 * Automatically click left when held down. No Carpel Tunnel!
 * @author Aton
 */
object AutoClicker : Module(
    "Auto Clicker",
    category = Category.MISC,
    description = "Left Click Auto Clicker. Activates when left click is being held down. The click speed will be randomized between max Cps and min Cps for each click."
){
    private val maxCps: NumberSetting = NumberSetting("Max CPS", 12.0, 1.0, 20.0, 1.0, description = "Maximum cps to be used.").apply {
        processInput = {
            input: Double -> MathHelper.clamp_double(input, minCps.value, max)
        }
    }

    private val minCps: NumberSetting = NumberSetting("Min CPS", 10.0, 1.0, 20.0, 1.0, description = "Minimum CPS to be used.").apply {
        processInput = {
            input: Double -> MathHelper.clamp_double(input, min, maxCps.value)
        }
    }

    private var nextClick = System.currentTimeMillis()

    init {
        this.addSettings(
            maxCps,
            minCps
        )
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (mc.thePlayer != null && mc.currentScreen == null && !mc.thePlayer.isUsingItem && mc.gameSettings.keyBindAttack.isKeyDown) {
            if (System.currentTimeMillis() - nextClick > 0){
                nextClick = System.currentTimeMillis() + Random.nextDouble(1000.0/ maxCps.value, 1000.0/ minCps.value).toInt()
                Utils.leftClick()
            }
        }
    }

}