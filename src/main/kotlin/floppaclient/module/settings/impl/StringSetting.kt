package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

class StringSetting(
    name: String,
    override var default: String = "",
    var length: Int = 30,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
) : Setting<String>(name, visibility, description) {

    override var value: String = default
        set(newStr) {
            field = newStr
            if (newStr.length > length) {
                field = field.substring(0, length - 1)
            }
        }

    var text: String by this::value
}