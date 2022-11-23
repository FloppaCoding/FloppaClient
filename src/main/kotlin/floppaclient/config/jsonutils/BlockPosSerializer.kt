package floppaclient.config.jsonutils

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.minecraft.util.BlockPos
import java.lang.reflect.Type

class BlockPosSerializer : JsonSerializer<BlockPos> {
    override fun serialize(src: BlockPos?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        src?.let {
            return JsonPrimitive("${it.x}, ${it.y}, ${it.z}")
        }

        return JsonPrimitive("")
    }
}
