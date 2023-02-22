package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

class ActionSetting(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
    override val default: () -> Unit = {}
) : Setting<() -> Unit>(name, visibility, description) {

    override var value: () -> Unit = default

    var action: () -> Unit by this::value

    fun doAction() {
        action()
    }
}
