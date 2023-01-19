package floppaclient.module.settings

/**
 * The super class that all Module Settings for the gui and config should inherit from.
 *
 * <br>
 * This class is not abstract because it is instanced as a default if reading the config files.
 * It is also used as a place holder for Keybind elements in the gui to avoid using null.
 * </br>
 *
 * @author Aton
 */
open class Setting(
    val name: String,
    val visibility: Visibility = Visibility.VISIBLE,
    var description: String? = null,
) {
    var visibilityDependency: () -> Boolean = dependency@{ return@dependency true }

    val shouldBeVisible: Boolean
        get() {
            return visibilityDependency()
        }

    open fun reset() {}

    companion object{
        /** Set a dependency which has to be fulfilled for this Setting to show up in the GUI.
         *
         * <br>
        The definition of this function as an extension function within a companion object is really scuffed and overly
        complicated.
        But this allows for a streamlined usage:
            private val setting = SomeSetting("Name", ... parameters)
            .withDependency {
                this.mode.index == 0
            }
        Almost like with a lambda function in the constructor (which would not be a good choice here).
         </br>
        */
        fun <K: Setting> K.withDependency(dependency: () -> Boolean): K {
            visibilityDependency = dependency
            return this
        }
    }
}