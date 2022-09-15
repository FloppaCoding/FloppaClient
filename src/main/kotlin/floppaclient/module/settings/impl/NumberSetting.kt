package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import net.minecraft.util.MathHelper
import kotlin.math.round

class NumberSetting(
    name: String,
    val default: Double = 1.0,
    var min: Double = -10000.0,
    var max: Double = 10000.0,
    var increment: Double = 1.0,
    hidden: Boolean = false,
    description: String? = null,
) : Setting(name, hidden, description) {

    var processInput: (Double) -> Double = { input: Double -> input}

    var value: Double = default
        set(newVal) {
            field = MathHelper.clamp_double(roundToIncrement(processInput(newVal)), min, max)
        }

    private fun roundToIncrement(x: Double): Double {
        return round(x / increment) * increment
    }

    override fun reset() {
        value = default
        super.reset()
    }
}