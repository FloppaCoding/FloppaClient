package floppaclient.module.settings

import kotlin.reflect.KProperty

/**
 * The super class that all Module Settings for the gui and config should inherit from.
 *
 * To avoid nullability you can use [DummySetting][floppaclient.module.settings.impl.DummySetting] instead of null.
 *
 * @author Aton
 */
abstract class Setting<T>(
    val name: String,
    val visibility: Visibility = Visibility.VISIBLE,
    var description: String? = null,
) {
    protected var visibilityDependency: () -> Boolean = { true }

    /**
     * The default for [value].
     */
    abstract val default: T

    /**
     * This is the main field used to store the state of the setting.
     * There can be additional properties for managing the state in the implementations.
     */
    abstract var value: T

    /**
     * Can be set to add a setting specific restraint / extra action when setting the new value.
     *
     * Is used differently in the implementations alongside with additional restraints on inputs.
     */
    var processInput: (T) -> T = { input: T -> input }

    val shouldBeVisible: Boolean
        get() {
            return visibilityDependency()
        }

    open fun reset() {
        value = default
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

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
        fun <K: Setting<*>> K.withDependency(dependency: () -> Boolean): K {
            visibilityDependency = dependency
            return this
        }

        /**
         * Can be used to set a setting specific restraint / extra action when setting the new value.
         *
         * Is used differently in the implementations alongside with additional restraints on inputs.
         */
        fun <K: Setting<T>, T> K.withInputTransform(transform: (input: T, setting: K) -> T): K {
            processInput = { transform(it, this) }
            return this
        }

        /**
         * Can be used to set a setting specific restraint / extra action when setting the new value.
         *
         * Is used differently in the implementations alongside with additional restraints on inputs.
         */
        fun <K: Setting<T>, T> K.withInputTransform(transform: (input: T) -> T): K {
            processInput = transform
            return this
        }
    }
}