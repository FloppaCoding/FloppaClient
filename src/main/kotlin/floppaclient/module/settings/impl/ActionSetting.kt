package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

class ActionSetting(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
) : Setting(name, visibility, description) {

    var action: () -> Unit = {}

    fun doAction() {
        action()
    }
}