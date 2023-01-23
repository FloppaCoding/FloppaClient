package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

class BooleanSetting (
    name: String,
    enabled: Boolean = false,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
): Setting(name, visibility, description) {

    var processInput: (Boolean) -> Boolean = { input: Boolean -> input }

    var enabled = enabled
     set(value) {
         field = processInput(value)

     }

    fun toggle() {
        enabled = !enabled
    }
}