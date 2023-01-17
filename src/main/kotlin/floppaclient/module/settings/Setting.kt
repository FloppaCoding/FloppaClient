package floppaclient.module.settings

/**
 * The super class that all Module Settings for the gui and config should inherit from.
 *
 * <br>
 * This class is not abstract because it is instanced as a default if reading the config fails
 * </br>
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