package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ExplosionHandledEvent
import floppaclient.events.VelocityUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils.equalsOneOf
import floppaclient.utils.ItemUtils.itemID
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

/**
 * Cancel Skyblock Knockback.
 * @author Aton
 */
object Velocity : Module(
    "Velocity",
    category = Category.PLAYER,
    description = "Modifies knockback in Skyblock"
) {
    private val swapTime = NumberSetting("Swap time", 500.0, 0.0, 1000.0, 50.0, description = "Determines how long after swapping off of the Bonzo staff you will still be able to take knockback form it.")
    private val cancelHighLavaBounce = BooleanSetting("Low Lava Bounce", true, description = "Only allows for low lava bounces.")
    private val fullDisableWithStaff = BooleanSetting("All Kb with Staff", false, description = "Fully disables anti kb when holding Jerry or Bonzo Staff.")

    // TODO more options like bonzo staff boost.

    init {
        this.addSettings(
            swapTime,
            cancelHighLavaBounce,
            fullDisableWithStaff
        )
    }

    private var bonzoTime = 0L
    private var lavaTime = 0L
    private const val lavaTimeout = 1000L


    @SubscribeEvent
    fun handleExplosion(event: ExplosionHandledEvent) {
        if (!inSkyblock) return
        event.isCanceled = true
    }

    /**
     * Keep trck of bonzo staff being held, to account for ping, so that bonzo staff packets are not cancelled.
     */
    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !inSkyblock) return
        if (mc.thePlayer?.heldItem?.itemID.equalsOneOf("BONZO_STAFF", "STARRED_BONZO_STAFF"))
            bonzoTime = System.currentTimeMillis() + swapTime.value.toInt()
        if (mc.thePlayer.isInLava)
            lavaTime = System.currentTimeMillis() + lavaTimeout
    }


    @SubscribeEvent
    fun handleEntityVelocity(event: VelocityUpdateEvent) {
        val entity = mc.theWorld.getEntityByID(event.packet.entityID)
        if (entity == mc.thePlayer) {
            if (!inSkyblock || shouldTakeKb()) return

            // Jerry chine will always give +4800 motionY and bonzo Staff always +4000
            // Lava bounce will always give +28000 motionY
            // Bonzo Staff will always have in between 159_000_000 and 160_000_000 total momentum square
            val totalMomentumSquare =
                (event.packet.motionX * event.packet.motionX + event.packet.motionY * event.packet.motionY + event.packet.motionZ * event.packet.motionZ)

            // Check for lava bounce
            if ((event.packet.motionY == 28000 && (if (cancelHighLavaBounce.enabled) mc.thePlayer.isInLava else System.currentTimeMillis() < lavaTime)))
                return

            // Check jerry gun and bonzo staff
            else if ((event.packet.motionY == 4000 && totalMomentumSquare in 159_000_000..160_000_000 && System.currentTimeMillis() < bonzoTime)
                || (event.packet.motionY == 4800 && mc.thePlayer.heldItem?.itemID.equals("JERRY_STAFF"))
            )
                return
            else if (fullDisableWithStaff.enabled && mc.thePlayer?.heldItem?.itemID.equalsOneOf("BONZO_STAFF", "STARRED_BONZO_STAFF", "JERRY_STAFF"))
                return

            // Check for spring boots, if worn allow vertical kb
            else if (mc.thePlayer.getEquipmentInSlot(1)?.itemID.equals("TARANTULA_BOOTS")
                || (!mc.thePlayer.isSneaking && mc.thePlayer.getEquipmentInSlot(1)?.itemID.equals("SPRING_BOOTS"))
            ) {
                entity.setVelocity(
                    entity.motionX,
                    event.packet.motionY / 8000.0,
                    entity.motionZ
                )
            }
            event.isCanceled = true
        }
    }

    private fun shouldTakeKb(): Boolean {
        return mc.thePlayer.heldItem?.itemID.equalsOneOf("LEAPING_SWORD", "SILK_EDGE_SWORD")
    }

    //"JERRY_STAFF","BONZO_STAFF", "STARRED_BONZO_STAFF",
}