package floppaclient.module.settings

/**
 * The super class that all Module Settings for the gui and config should inherit from.
 *
 * To avoid nullability you can use [DummySetting][floppaclient.module.settings.impl.DummySetting] instead of null.
 *
 * @author Aton
 */
abstract class Setting(
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