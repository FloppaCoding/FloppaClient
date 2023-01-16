package floppaclient.module.settings

/**
 * The super class that all Module Settings for the gui and config should inherit from.
 *
 * @author Aton
 */
open class Setting(
    val name: String,
    val visibility: Visibility = Visibility.VISIBLE,
    var description: String? = null,
) {
    open fun reset() {}
}