package floppaclient.events

import floppaclient.funnymap.core.Room
import net.minecraft.client.audio.ISound
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.model.ModelBase
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

open class ClickEvent : Event() {
    @Cancelable
    class LeftClickEvent : ClickEvent()

    @Cancelable
    class RightClickEvent : ClickEvent()

    @Cancelable
    class MiddleClickEvent : ClickEvent()
}

open class GuiContainerEvent(val container: Container, val gui: GuiContainer) : Event() {
    @Cancelable
    class DrawSlotEvent(container: Container, gui: GuiContainer, var slot: Slot) :
        GuiContainerEvent(container, gui)

    @Cancelable
    class SlotClickEvent(container: Container, gui: GuiContainer, var slot: Slot?, var slotId: Int) :
        GuiContainerEvent(container, gui)
}

@Cancelable
class DrawContainerEvent(val mouseX: Int, val mouseY: Int) : Event()

class DrawContainerLastEvent(val mouseX: Int, val mouseY: Int) : Event()

@Cancelable
class ContainerMouseClickedEvent(val mouseX: Int, val mouseY: Int, val mouseButton: Int): Event()

@Cancelable
class ContainerKeyTypedEvent(val keyCode: Int): Event()

@Cancelable
class ReceivePacketEvent(val packet: Packet<*>) : Event()

@Cancelable
class VelocityUpdateEvent(val packet: S12PacketEntityVelocity) : Event()

/**
 * Gets fired right before the momentum of an S27 is handled by the net handler.
 */
@Cancelable
class ExplosionHandledEvent(val packet: S27PacketExplosion) : Event()

class ReceiveChatPacketEvent(val packet: S02PacketChat) : Event()

/**
 * Gets called upon receiving a S08PacketPlayerPosLook packet
 */
@Cancelable
class TeleportEventPre(val packet: S08PacketPlayerPosLook) : Event()
// This event only gets fired after the vanilla action by the NetHandlerPlayClient ist performed, which does not happen when the
// pacet is cancelled.
class TeleportEventPost(val packet: S08PacketPlayerPosLook) : Event()

class PlaySoundEventPre(val p_sound: ISound) : Event()

/**
 * Fired when an entity is removed from the world.
 */
class EntityRemovedEvent(val entity: Entity) : Event()

@Cancelable
class RenderLivingEntityEvent(
    var entity: EntityLivingBase,
    var p_77036_2_: Float,
    var p_77036_3_: Float,
    var p_77036_4_: Float,
    var p_77036_5_: Float,
    var p_77036_6_: Float,
    var scaleFactor: Float,
    var modelBase: ModelBase
) : Event()


@Cancelable
class PacketSentEvent(val packet: Packet<*>) : Event()

/**
 * Fired in Dungeon.kt whenever the room is changed.
 */
class RoomChangeEvent(val newRoom: Room?, val oldRoom: Room?) : Event()
