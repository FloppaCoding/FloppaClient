package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.VelocityUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.utils.GeometryUtils.cosDeg
import floppaclient.utils.GeometryUtils.sinDeg
import floppaclient.utils.GeometryUtils.yaw
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module aimed at modifying the Floor 7 lava bounce velocity.
 *
 * @author Aton
 */
object LavaBounce : Module(
    "Lava Bounce",
    category = Category.DUNGEON,
    description = "Modify the behaviour of the floor7 lava bounce."
){
    private val vertMult = NumberSetting("Vertical Multiplier", 1.0, 0.0, 2.0,0.05, description = "Changes the vertical momentum of the lava bounce.")
    private val horiMult = NumberSetting("Horizontal Multiplier", 0.5, 0.0, 2.0,0.05, description = "Strength of the horizontal momentum added to the bounce.")
    private val condition: SelectorSetting

    init {
        val conditions = arrayListOf(
            "Sneak",
            "Space",
            "Walk",
            "None"
        )
        condition = SelectorSetting("Condition", conditions[0], conditions, description = "Condition for this module to affect the bounce.")
        this.addSettings(
            condition,
            vertMult,
            horiMult
        )
    }


    @SubscribeEvent(priority = EventPriority.HIGH)
    fun handleEntityVelocity(event: VelocityUpdateEvent){
        val entity = mc.theWorld.getEntityByID(event.packet.entityID)
        if (entity == mc.thePlayer) {
            if (!FloppaClient.inSkyblock || !mc.thePlayer.isInLava) return
            if (event.packet.motionY != 28000) return
            when (condition.selected) {
                "Sneak" -> if (!mc.gameSettings.keyBindSneak.isKeyDown) return
                "Jump" -> if (!mc.gameSettings.keyBindJump.isKeyDown) return
                "Walk" -> if (!mc.gameSettings.keyBindForward.isKeyDown) return
            }

            val yaw = yaw()
            entity.setVelocity(
                (event.packet.motionX - sinDeg(yaw)*event.packet.motionY * horiMult.value) / 8000.0,
                event.packet.motionY * vertMult.value / 8000.0,
                (event.packet.motionZ + cosDeg(yaw)*event.packet.motionY * horiMult.value) / 8000.0,
            )

            event.isCanceled = true
        }
    }
}