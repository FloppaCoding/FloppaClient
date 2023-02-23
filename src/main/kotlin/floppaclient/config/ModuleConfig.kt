package floppaclient.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import floppaclient.FloppaClient.Companion.MOD_NAME
import floppaclient.config.jsonutils.SettingDeserializer
import floppaclient.config.jsonutils.SettingSerializer
import floppaclient.module.ConfigModule
import floppaclient.module.ModuleManager
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.*
import java.awt.Color
import java.io.File
import java.io.IOException

/**
 * ## A class to handle the module config file for the mod.
 *
 * Provides methods to save and load the settings for all [modules][ModuleManager.modules] in [ModuleManager] to / from the file.
 *
 * @author Aton
 */
class ModuleConfig(path: File) {

    private val gson = GsonBuilder()
        .registerTypeAdapter(object : TypeToken<Setting<*>>(){}.type, SettingSerializer())
        .registerTypeAdapter(object : TypeToken<Setting<*>>(){}.type, SettingDeserializer())
        .excludeFieldsWithoutExposeAnnotation()
        .setPrettyPrinting().create()


    private val configFile = File(path, "floppaclientConfig.json")

    init {
        try {
            // This gets run before the pre initialization event (it gets run when the Companion object from FloppaClient
            // is created)
            // therefore the directory did not get created by the preInit handler.
            // It is created here
            if (!path.exists()) {
                path.mkdirs()
            }
            // create file if it doesn't exist
            configFile.createNewFile()
        } catch (e: Exception) {
            println("Error initializing $MOD_NAME module config")
        }
    }

    fun loadConfig() {
        try {
            val configModules: ArrayList<ConfigModule>
            with(configFile.bufferedReader().use { it.readText() }) {
                if (this == "") {
                    return
                }
                configModules= gson.fromJson(
                    this,
                    object : TypeToken<ArrayList<ConfigModule>>() {}.type
                )
            }
            configModules.forEach { configModule ->
                ModuleManager.getModuleByName(configModule.name).run updateModule@{
                    // If the module was not found check whether it can be a keybind
                    val module = this ?: if (configModule.settings.find { (it is BooleanSetting) && it.name == "THIS_IS_A_KEY_BIND" } != null) {
                        ModuleManager.addNewKeybind()
                    }else {
                        return@updateModule
                    }
                    if (module.enabled != configModule.enabled) module.toggle()
                    module.keyCode = configModule.keyCode
                    for (configSetting in configModule.settings) {
                        // It seems like when the config parsing failed it can result in this being null. The compiler does not know this.
                        // This check ensures that the rest of the config will still get processed in that case, avoiding the loss of data.
                        // So just suppress the warning here.
                        @Suppress("SENSELESS_COMPARISON")
                        if (configSetting == null) continue
                        val setting = module.getSettingByName(configModule.name) ?: continue
                        when (setting) {
                            is BooleanSetting -> setting.enabled = (configSetting as BooleanSetting).enabled
                            is NumberSetting -> setting.value = (configSetting as NumberSetting).value
                            is ColorSetting -> setting.value = Color((configSetting as NumberSetting).value.toInt(), true)
                            is StringSelectorSetting -> setting.selected = (configSetting as StringSetting).text
                            is StringSetting -> setting.text = (configSetting as StringSetting).text
                        }
                    }
                }
            }

        } catch (e: JsonSyntaxException) {
            println("Error parsing $MOD_NAME config.")
            println(e.message)
            e.printStackTrace()
        } catch (e: JsonIOException) {
            println("Error reading $MOD_NAME config.")
        } catch (e: Exception) {
            println("$MOD_NAME Config Error.")
            println(e.message)
            e.printStackTrace()
        }
    }

    fun saveConfig() {
        try {
            configFile.bufferedWriter().use {
                it.write(gson.toJson(ModuleManager.modules))
            }
        } catch (e: IOException) {
            println("Error saving $MOD_NAME config.")
        }
    }
}