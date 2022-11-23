package floppaclient.config.jsonutils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import net.minecraft.util.BlockPos
import java.lang.reflect.Type

class BlockPosDeserializer : JsonDeserializer<BlockPos> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): BlockPos {
        var blockPos = BlockPos(0,0,0)

        if (json?.isJsonPrimitive == true) {
            json.asJsonPrimitive.let{ primitive ->
                // drop first and last element as those are "
                val coordList = (primitive.toString().dropLast(1).drop(1)
                    .takeIf(String::isNotEmpty)
                    ?.split(", ")
                    ?: listOf<String>())
                    .map { it.toIntOrNull() ?: 0 }
                if(coordList.size >= 3) {
                    blockPos = BlockPos(coordList[0], coordList[1], coordList[2])
                }
            }
        }
        return blockPos
    }
}