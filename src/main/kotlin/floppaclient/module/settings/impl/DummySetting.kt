package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility

/**
 * A direct subclass of [Setting] which does not provide any backing field.
 *
 * **Do not use this as a Setting for your Module!**
 *
 * This class is meant to be used as a placeholder when no other Setting is applicable.
 * Doing this allows you to avoid using the nullable type *Setting?*.
 *
 * Used for pasrsing the config file and for displaying the Module Keybind in the advanced gui.
 * @author Aton
 */
class DummySetting(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
) : Setting<Any?>(name, visibility, description) {
    /**
     * Always is null.
     * Not intended to be used.
     */
    override val default: Any?
        get() = null

    /**
     * Always is null.
     * Not intended to be used.
     */
    override var value: Any?
        get() = null
        set(_) {}
}