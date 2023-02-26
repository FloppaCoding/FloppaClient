package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

/**
 * A setting which allows for a selection out of a given set of options.
 *
 * In most cases it is more convenient to use the factory function which acts as a constructor which omits the [options]
 * parameter.
 *
 * @author Aton
 */
class SelectorSetting<T>(
    name: String,
    override val default: T,
    val options: Array<out T>,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
) : Setting<T>(name, visibility, description) where T : Options, T: Enum<T> {

    override var value: T = default
        set(input) {
            field = processInput(input)
        }

    var index: Int
        get() = value.ordinal
        set(newVal) {
            // guarantees that index is in bounds and enables cycling behaviour
            value = options[if (newVal > options.size - 1)  0 else if ( newVal < 0) options.size - 1 else newVal]
        }

    /**
     * [displayName][Options.displayName] of the selected Enum.
     * Can be used to set [value] based on the displayName of the Enum.
     * This is required for loading data from the config.
     * If possible [value] should be directly instead.
     */
    var selected: String
        get() = value.displayName
        set(input) {
            value = options.find { it.displayName.equals(input, ignoreCase = true) } ?: return
        }

    fun isSelected(option: Options): Boolean {
        return  this.value === option
    }
}

/**
 * This factory function provides a more convenient Constructor for [SelectorSetting]
 * where [options][SelectorSetting.options] can be omitted.
 * The options are inferred from the provided [default] value. All available constants will be used.
 *
 * If you want to limit the options to be a subset of the available constants, use the main constructor and specify those explicitly.
 */
inline fun <reified L> SelectorSetting(name: String,
                                           default: L,
                                           visibility: Visibility = Visibility.VISIBLE,
                                           description: String? = null
) : SelectorSetting<L> where L : Options, L: Enum<L> =
    SelectorSetting(name, default, enumValues(), visibility, description)

interface Options {
    val displayName: String
}