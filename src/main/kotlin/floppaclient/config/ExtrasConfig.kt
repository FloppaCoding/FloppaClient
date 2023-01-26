package floppaclient.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import floppaclient.config.jsonutils.SetBlockPosDeserializer
import floppaclient.config.jsonutils.SetBlockPosSerializer
import floppaclient.floppamap.core.ExtrasData
import net.minecraft.util.BlockPos
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ExtrasConfig(path: File) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(object : TypeToken<MutableSet<BlockPos>>(){}.type, SetBlockPosSerializer())
        .registerTypeAdapter(object : TypeToken<MutableSet<BlockPos>>(){}.type, SetBlockPosDeserializer())
        .setPrettyPrinting().create()

    private val configFile = File(path, "extrasConfig.json")
    private val backupFile = File(path, "extrasBackup.json")

    private val configFileOther = File(path, "extrasConfigOther.json")

    // Map of room name to extras data
    var extraRooms: MutableMap<String, ExtrasData> = mutableMapOf()

    var extraRegions: MutableMap<String, ExtrasData> = mutableMapOf()

    init {
        try {
            // This gets run before the pre initialization event (it gets run when the Companion object from AtonClient
            // is created)
            // therefore the directory did not get created by the preInit handler.
            // It is created here
            if (!path.exists()) {
                path.mkdirs()
            }
            // create file if it doesn't exist
            backupFile.createNewFile()
            configFile.createNewFile()
            configFileOther.createNewFile()
        } catch (e: Exception) {
            println("Error initializing Floppa Map Extras")
        }
    }

    fun loadConfig() {
        try {
            with(configFile.bufferedReader().use { it.readText() }) {
                if (this == "") {
                    return
                }
                extraRooms = gson.fromJson(
                    this,
                    object : TypeToken<MutableMap<String, ExtrasData>>() {}.type
                )
            }

            with(configFileOther.bufferedReader().use { it.readText() }) {
                if (this == "") {
                    return
                }
                extraRegions = gson.fromJson(
                    this,
                    object : TypeToken<MutableMap<String, ExtrasData>>() {}.type
                )
            }
        } catch (e: JsonSyntaxException) {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            println("Error parsing Floppa Map Extras config. Backing up current config.")
            println(e.message)
            e.printStackTrace()
        } catch (e: JsonIOException) {
            println("Error reading Floppa Map Extras config.")
        }
    }

    fun saveConfig() {
        try {
            configFile.bufferedWriter().use {
                it.write(gson.toJson(extraRooms))
            }
            configFileOther.bufferedWriter().use {
                it.write(gson.toJson(extraRegions))
            }
        } catch (e: IOException) {
            println("Error saving Floppa Map Extras config.")
        }
    }
}
