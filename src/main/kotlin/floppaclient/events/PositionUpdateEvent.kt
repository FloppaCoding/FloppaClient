package floppaclient.events

import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

@Cancelable
open class PositionUpdateEvent constructor(
    var x: Double,
    var y: Double,
    var z: Double,
    var yaw: Float,
    var pitch: Float,
    var onGround: Boolean,
    var sprinting: Boolean,
    var sneaking: Boolean
) : Event() {


    @Cancelable
    class Pre(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean, sprinting: Boolean, sneaking: Boolean) : PositionUpdateEvent(x, y, z, yaw, pitch, onGround, sprinting, sneaking)

    @Cancelable
    class Post(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean, sprinting: Boolean, sneaking: Boolean) : PositionUpdateEvent(x, y, z, yaw, pitch, onGround, sprinting, sneaking)
}