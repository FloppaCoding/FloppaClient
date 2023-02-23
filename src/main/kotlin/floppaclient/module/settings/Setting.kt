package floppaclient.module.settings

import floppaclient.module.Module
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation

/**
 * The super class that all Module Settings for the gui and config should inherit from.
 *
 * See [Module] for instructions on how to use settings.
 *
 * This class provides support for delegation to Settings.
 * Delegating to a Setting will register it to the Module from which you are delegating a property to the Setting.
 * If you do not want this to happen use the [DoNotRegister] annotation.
 *
 * To avoid nullability you can use [DummySetting][floppaclient.module.settings.impl.DummySetting] instead of null.
 *
 * @author Aton
 */
abstract class Setting<T>(
    val name: String,
    val visibility: Visibility = Visibility.VISIBLE,
    var description: String? = null,
) : ReadWriteProperty<Module, T>, PropertyDelegateProvider<Module, ReadWriteProperty<Module,T>> {
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

    /**
     * Returns whether this setting should be visible based on [visibilityDependency].
     * Is true by default.
     * Use [withDependency] to change this behavior.
     */
    val shouldBeVisible: Boolean
        get() {
            return visibilityDependency()
        }

    /**
     * Sets [value] to [default].
     */
    open fun reset() {
        value = default
    }

    /**
     * This operator provides the Delegate to the Setting.
     *
     * It automatically registers the Setting to the Module from which you are delegating a property to this Setting.
     * If the property has the [DoNotRegister] annotation it will not be registered.
     */
    override operator fun provideDelegate(thisRef: Module, property: KProperty<*>): ReadWriteProperty<Module, T> {
        return if (!property.hasAnnotation<DoNotRegister>()) {
            thisRef.register(this)
        }else this
    }

    /**
     * This method acts as the getter for the property delegated to this class.
     */
    override operator fun getValue(thisRef: Module, property: KProperty<*>): T {
        return value
    }

    /**
     * This method acts as the setter for the property delegated to this class.
     */
    override operator fun setValue(thisRef: Module, property: KProperty<*>, value: T) {
        this.value = value
    }

    /**
     * This companion object provides extension functions for the Setting classes.
     */
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