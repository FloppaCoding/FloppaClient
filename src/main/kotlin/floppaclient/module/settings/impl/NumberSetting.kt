package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility
import net.minecraft.util.MathHelper
import kotlin.math.round

class NumberSetting(
    name: String,
    override val default: Double = 1.0,
    val min: Double = -10000.0,
    val max: Double = 10000.0,
    val increment: Double = 1.0,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
) : Setting<Double>(name, visibility, description) {

    override var value: Double = default
        set(newVal) {
            field = MathHelper.clamp_double(roundToIncrement(processInput(newVal)), min, max)
        }

    private fun roundToIncrement(x: Double): Double {
        return round(x / increment) * increment
    }
}