package floppaclient.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import floppaclient.config.jsonutils.BlockPosDeserializer
import floppaclient.config.jsonutils.BlockPosSerializer
import floppaclient.config.jsonutils.IntListDeserializer
import floppaclient.floppamap.core.AutoActionData
import net.minecraft.util.BlockPos
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * ## A class to handle the auto actions config file for the mod.
 *
 * @author Aton
 */
class AutoActionConfig(path: File) {

    private val gson = GsonBuilder()
        .registerTypeAdapter(object : TypeToken<MutableList<Int>>(){}.type, IntListDeserializer())
        .registerTypeAdapter(object : TypeToken<BlockPos>(){}.type, BlockPosSerializer())
        .registerTypeAdapter(object : TypeToken<BlockPos>(){}.type, BlockPosDeserializer())
        .setPrettyPrinting().create()


    private val configFile = File(path, "actionConfig.json")
    private val backupFile = File(path, "newBackup.json")

    private val configFileOther = File(path, "actionConfigOther.json")


    /**
     * Map of room name to extras data
     */
    var autoActionRooms: MutableMap<String, AutoActionData> = mutableMapOf()

    var autoActionRegions: MutableMap<String, AutoActionData> = mutableMapOf()


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
            backupFile.createNewFile()
            configFile.createNewFile()
            configFileOther.createNewFile()
        } catch (e: Exception) {
            println("Error initializing Auto Action Config")
        }
    }

    fun loadConfig() {
        try {
            with(configFile.bufferedReader().use { it.readText() }) {
                if (this == "") {
                    return
                }
                autoActionRooms = gson.fromJson(
                    this,
                    object : TypeToken<MutableMap<String, AutoActionData>>() {}.type
                )
            }

            with(configFileOther.bufferedReader().use { it.readText() }) {
                if (this == "") {
                    return
                }
                autoActionRegions = gson.fromJson(
                    this,
                    object : TypeToken<MutableMap<String, AutoActionData>>() {}.type
                )
            }
        } catch (e: JsonSyntaxException) {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            println("Error parsing Auto Action config. Backing up current config.")
            println(e.message)
            e.printStackTrace()
        } catch (e: JsonIOException) {
            println("Error reading Auto Action config.")
        }
    }

    fun saveConfig() {
        try {
            configFile.bufferedWriter().use {
                it.write(gson.toJson(autoActionRooms))
            }
            configFileOther.bufferedWriter().use {
                it.write(gson.toJson(autoActionRegions))
            }
        } catch (e: IOException) {
            println("Error saving Auto Action config.")
        }
    }
}
