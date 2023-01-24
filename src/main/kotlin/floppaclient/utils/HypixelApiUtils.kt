package floppaclient.utils

import floppaclient.module.impl.render.ClickGui
import com.google.gson.JsonParser
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

/**
 * # Methods for accessing the Hypixel API.
 *
 * Getting a response from an http query takes some time.
 * Therefore the methods in here should only be used suspended in coroutines that do not block the main thread.
 * To ensure that none of the methods are used on accident in the main thread they have the *suspend* keyword.
 * This only allows them to be used in a coroutine. In general the IO dispatcher is the best choice for these coroutines.
 * The following shows an example of how to use [getSecrets].
 *
 *     scope.launch(Dispatchers.IO) {
 *          val uuid = mc.thePlayer.uniqueID.toString()
 *          val secrets = HypixelApiUtils.getSecrets(uuid)
 *          modMessage("$secrets")
 *     }
 * @author Aton
 */
object HypixelApiUtils {

    /**
     * Get the total amount of secrets a player has collected.
     * Only use this method in a coroutine to not freeze the main thread.
     *
     * Based on a method provided by [Harry282](https://github.com/Harry282 "Github")
     */
    suspend fun getSecrets(uuid: String): Int? {
        val response = fetch("https://api.hypixel.net/player?key=${ClickGui.apiKey.text}&uuid=${uuid}")
        return if (response == null) null else try {
            val jsonObject = JsonParser().parse(response).asJsonObject
            if (jsonObject.getAsJsonPrimitive("success")?.asBoolean == true) {
                jsonObject.getAsJsonObject("player")?.getAsJsonObject("achievements")
                    ?.getAsJsonPrimitive("skyblock_treasure_hunter")?.asInt
            }else
                null
        }catch (_: Exception){
            null
        }
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun fetch(uri: String): String? {
        HttpClients.createMinimal().use {
            try {
                val httpGet = HttpGet(uri)
                return EntityUtils.toString(it.execute(httpGet).entity)
            }catch (_: Exception) {
                return null
            }
        }
    }
}