package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ExplosionHandledEvent
import floppaclient.events.VelocityUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
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
){
    private val swapTime = NumberSetting("Swap time", 500.0,0.0,1000.0,50.0, description = "Determines how long after swapping off of the Bonzo staff you will still be able to take knockback form it.")
    init {
        this.addSettings(
            swapTime
        )
    }

    private var bonzoTime = 0L


    @SubscribeEvent
    fun handleExplosion(event: ExplosionHandledEvent){
        if (!inSkyblock) return
        event.isCanceled = true
    }

    /**
     * Keep trck of bonzo staff being held, to account for ping, so that bonzo staff packets are not cancelled.
     */
    @SubscribeEvent
    fun onTick(event: ClientTickEvent){
        if (event.phase != TickEvent.Phase.START || !inSkyblock) return
        if(mc.thePlayer.heldItem?.itemID.equalsOneOf( "BONZO_STAFF", "STARRED_BONZO_STAFF",))
            bonzoTime = System.currentTimeMillis() + swapTime.value.toInt()
    }


    @SubscribeEvent
    fun handleEntityVelocity(event: VelocityUpdateEvent){
        val entity = mc.theWorld.getEntityByID(event.packet.entityID)
        if (entity == mc.thePlayer) {
            if (!inSkyblock || shouldTakeKb()) return
            // Check for spring boots, if worn allow vertical kb
            if (mc.thePlayer.getEquipmentInSlot(1)?.itemID.equalsOneOf("SPRING_BOOTS", "TARANTULA_BOOTS")) {
                entity.setVelocity(
                    entity.motionX,
                    event.packet.motionY / 8000.0,
                    entity.motionY
                )
            }
            // Jerry chine will always give +4800 motionY and bonzo Staff always +4000
            // Bonzo Staff will always have in between 159_000_000 and 160_000_000 total momentum square
            val totalMomentumSquare =
                (event.packet.motionX * event.packet.motionX + event.packet.motionY * event.packet.motionY + event.packet.motionZ * event.packet.motionZ)
            if ((event.packet.motionY == 4000 && totalMomentumSquare in 159_000_000..160_000_000 && System.currentTimeMillis() < bonzoTime)
                || (event.packet.motionY == 4800 && mc.thePlayer.heldItem?.itemID.equals("JERRY_STAFF"))
            ) return
            event.isCanceled = true
        }
    }

    private fun shouldTakeKb(): Boolean {
        return mc.thePlayer.heldItem?.itemID.equalsOneOf( "LEAPING_SWORD",  "SILK_EDGE_SWORD")
                || mc.thePlayer.isInLava
    }

    //"JERRY_STAFF","BONZO_STAFF", "STARRED_BONZO_STAFF",
}