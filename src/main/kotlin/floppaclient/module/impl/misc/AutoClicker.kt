package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.inventory.SkyblockItem
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
    description = "A simple auto clicker for both left and right click. Activates when the corresponding key is being held down. " +
            "When ${TerminatorClicker.name} is active the right click auto clicker will not activate for shortbows. "
) {
    private val leftClick = BooleanSetting("Left Click", true, description = "Toggles the auto clicker for left click.")
    private val maxCps: NumberSetting = NumberSetting("Max CPS", 12.0, 1.0, 20.0, 1.0, description = "Maximum cps for left click.").apply {
            processInput = { input: Double ->
                MathHelper.clamp_double(input, minCps.value, max)
            }
        }.withDependency { leftClick.enabled }

    private val minCps: NumberSetting = NumberSetting("Min CPS", 10.0, 1.0, 20.0, 1.0, description = "Minimum CPS for left click.").apply {
            processInput = { input: Double ->
                MathHelper.clamp_double(input, min, maxCps.value)
            }
        }.withDependency { leftClick.enabled }

    private val rightClick = BooleanSetting("Right Click", false, description = "Toggles the auto clicker for right click.")
    private val rightClickSleep = NumberSetting("RC Sleep", 100.0, 20.0, 200.0, 5.0, description = "Delay in between right clicks in milliseconds.")
            .withDependency { rightClick.enabled }

    private var nextLeftClick = System.currentTimeMillis()
    private var nextRightClick = System.currentTimeMillis()

    init {
        this.addSettings(
            leftClick,
            maxCps,
            minCps,
            rightClick,
            rightClickSleep,
        )
    }

    /**
     * Set the click delay.
     * Prevents double click on the initial click.
     */
    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if(leftClick.enabled) {
            val nowMillis = System.currentTimeMillis()
            nextLeftClick = nowMillis + Random.nextDouble(1000.0 / maxCps.value, 1000.0 / minCps.value).toInt()
        }
    }

    /**
     * Set the click delay.
     * Prevents double click on the initial click.
     */
    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if(rightClick.enabled) {
            val nowMillis = System.currentTimeMillis()
            val overshoot =  (nowMillis - nextRightClick).takeIf { it < 200 } ?: 0L
            nextRightClick = nowMillis + (rightClickSleep.value.toLong() - overshoot).coerceAtLeast(0L)
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (mc.thePlayer != null && mc.currentScreen == null && !mc.thePlayer.isUsingItem) {
            val nowMillis = System.currentTimeMillis()
            if (leftClick.enabled && mc.gameSettings.keyBindAttack.isKeyDown && nowMillis >= nextLeftClick) {
                Utils.leftClick()
                // The delay is set in onLeftClick
            }
            // Ensure that not this and the terminator clicker are both active at the same time
            if (rightClick.enabled && mc.gameSettings.keyBindUseItem.isKeyDown
                && (!TerminatorClicker.enabled || !mc.thePlayer.isHolding(SkyblockItem.Attribute.SHORTBOW))
                && nowMillis >= nextRightClick
            ) {
                Utils.rightClick()
                // The delay is set in onRightClick
            }
        }
    }
}