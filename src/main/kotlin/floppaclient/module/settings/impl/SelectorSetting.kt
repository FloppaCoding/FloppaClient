package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import net.minecraft.util.MathHelper

class SelectorSetting(
    name: String,
    var default: String,
    var options: ArrayList<String>,
    hidden: Boolean = false,
    description: String? = null,
) : Setting(name, hidden, description){

    var index: Int = optionIndex(default)
     set(value) {
         /** guarantees that index is in bounds and enables cycling behaviour */
         field = if (value > options.size - 1)  0 else if ( value < 0) options.size - 1 else value
     }

    var selected: String
     set(value) {
        index = optionIndex(value)
    }
    get() {
        return options[index]
    }

    /**
     * Finds the index of given option in the option list.
     * Ignores the case of the strings and returns 0 if not found.
     */
    private fun optionIndex(string: String): Int {
        return MathHelper.clamp_int(this.options.map { it.lowercase() }.indexOf(string.lowercase()), 0, options.size - 1)
    }

    fun isSelected(string: String): Boolean {
        return  this.selected.equals(string, ignoreCase = true)
    }

    override fun reset() {
        selected = default
        super.reset()
    }

}