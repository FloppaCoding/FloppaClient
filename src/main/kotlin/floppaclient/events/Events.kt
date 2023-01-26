package floppaclient.events

import floppaclient.floppamap.core.Room
import floppaclient.floppamap.core.RoomState
import floppaclient.floppamap.core.Tile
import net.minecraft.block.state.IBlockState
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
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
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

//<editor-fold desc="Dungeon Events">

/**
 * Fired in [Dungeon.onTick][floppaclient.floppamap.dungeon.Dungeon.onTick] whenever the room is changed.
 */
class RoomChangeEvent(val newRoomPair: Pair<Room, Int>?, val oldRoomPair: Pair<Room, Int>?) : Event()

/**
 * Fired when a secret is picked up in dungeons.
 * Currently gets fired whenever SecretChime plays its sound.
 */
class DungeonSecretEvent : Event()

/**
 * Fired in [Dungeon.onChat][floppaclient.floppamap.dungeon.Dungeon.onChat] when the "> EXTRA STATS <" message is received.
 */
class DungeonEndEvent : Event()

/**
 * Posted in [MapUpdate.updateRooms][floppaclient.floppamap.dungeon.MapUpdate.updateRooms] right before the state of a Tile is changed.
 * The old state is still contained in [tile] as [Tile.state].
 */
class DungeonRoomStateChangeEvent(val tile: Tile, val newState: RoomState) : Event()

//</editor-fold>

@Cancelable
class BlockStateChangeEvent(val pos: BlockPos, val oldState: IBlockState, val newState: IBlockState) : Event()

class BlockDestroyEvent(val pos: BlockPos, val side: EnumFacing, val state: IBlockState) : Event()

/**
 * Fired when a clip chain is finished.
 */
class ClipFinishEvent : Event()
