package floppaclient.config.jsonutils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class IntListDeserializer : JsonDeserializer<MutableList<Int>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MutableList<Int> {
        return if (json?.isJsonPrimitive == true) {
            // for some reason conversion of the json, that is of type JsonString, which is JsonPrimitive, to JsonArray is needed
            (json.toString().substringAfter("[").substringBefore("]")
                .takeIf(String::isNotEmpty)
                ?.split(", ")
                ?.toMutableList()
                ?: mutableListOf<String>())
                .map { it.toIntOrNull() ?: 0 }
                .toMutableList()
//            Gson().fromJson(json, typeOfT)
            // object : TypeToken<MutableList<Int>>(){}.type

        } else {
            mutableListOf<Int>()
        }
    }
}