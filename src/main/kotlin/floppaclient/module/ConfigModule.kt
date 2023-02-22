package floppaclient.module

import floppaclient.module.settings.Setting

/**
 * A dummy implementation of the [Module] class.
 *
 * This class is made to be used as a dummy class for gson to use when reading the module config.
 * @author Aton
 */
class ConfigModule(name: String,
                   keyCode: Int = 0,
                   category: Category = Category.MISC,
                   toggled: Boolean = false,
                   settings: ArrayList<Setting<*>> = ArrayList(),
                   description: String = ""
) : Module(name, keyCode, category, toggled, settings, description)