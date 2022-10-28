package floppaclient.config.jsonutils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import net.minecraft.util.BlockPos
import java.lang.reflect.Type

class SetBlockPosDeserializer : JsonDeserializer<MutableSet<BlockPos>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MutableSet<BlockPos> {
        val blockSet = mutableSetOf<BlockPos>()

        if (json?.isJsonArray == true) {
            json.asJsonArray.forEach{ element ->
                // drop first and last element as those are "
                val coordList = (element.toString().dropLast(1).drop(1)
                    .takeIf(String::isNotEmpty)
                    ?.split(", ")
                    ?: listOf<String>())
                    .map { it.toIntOrNull() ?: 0 }
                if(coordList.size >= 3) {
                    blockSet.add(BlockPos(coordList[0], coordList[1], coordList[2]))
                }
            }
        }
        return blockSet
    }
}