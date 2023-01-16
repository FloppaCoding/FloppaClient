package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

class StringSetting(
    name: String,
    var default: String = "",
    var length: Int = 30,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
) : Setting(name, visibility, description) {

    var text: String = default
        set(newStr) {
            field = newStr
            if (newStr.length > length) {
                field = field.substring(0, length - 1)
            }
        }

    override fun reset() {
        text = default
        super.reset()
    }
}