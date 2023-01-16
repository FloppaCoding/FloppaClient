package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

class BooleanSetting (
    name: String,
    var enabled: Boolean = false,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
): Setting(name, visibility, description) {

    fun toggle() {
        enabled = !enabled
    }
}