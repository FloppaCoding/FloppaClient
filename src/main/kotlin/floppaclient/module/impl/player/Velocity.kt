package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ExplosionHandledEvent
import floppaclient.events.VelocityUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.GeometryUtils.rotateYawTo
import floppaclient.utils.GeometryUtils.scale
import floppaclient.utils.GeometryUtils.scaleHorizontal
import floppaclient.utils.GeometryUtils.scaleVertical
import floppaclient.utils.Utils.equalsOneOf
import floppaclient.utils.ItemUtils.itemID
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

/**
 * Modifies Skyblock Knockback.
 * @author Aton
 */
object Velocity : Module(
    "Velocity",
    category = Category.PLAYER,
    description = "Modifies knockback in Skyblock."
) {
    private val swapTime = NumberSetting("Swap time", 500.0, 0.0, 1000.0, 50.0, description = "Determines how long after swapping off of the Bonzo staff you will still be able to take knockback form it.")
    private val jerryRocket = BooleanSetting("Vertical Jerry Staff", true, description = "Modifies the jerry gun knockback to only be vertical.")
    private val jerryBoostVert = NumberSetting("Jerry boost vert.", 1.0, 0.5, 1.5, 0.01, description = "Modifies the vertical component of the Jerry staff kb.")
    private val bonzoDirect = BooleanSetting("Bonzo Direction", false, description = "If enabled the bonzo staff only lets you take kb in the direction for which you are currently holding the key down.")
    private val bonzoVert = BooleanSetting("Bonzo Vertical", false, description = "For use in conjunction with Bonzo Direction. When enabled you still get the vertical kb from the Bonzo staff if no direction key is held.")
    private val bonzoBoostHori = NumberSetting("Bonzo boost hori.", 1.0, 0.5, 1.5, 0.01, description = "Modifies the horizontal component of the bonzo staff kb.")
    private val bonzoBoostVert = NumberSetting("Bonzo boost vert.", 1.0, 0.5, 1.5, 0.01, description = "Modifies the vertical component of the bonzo staff kb.")
    private val cancelHighLavaBounce = BooleanSetting("Low Lava Bounce", true, description = "Only allows for low lava bounces.")
    private val fullDisableWithStaff = BooleanSetting("All Kb with Staff", false, description = "Fully disables anti kb when holding Jerry or Bonzo Staff.")

    init {
        this.addSettings(
            swapTime,
            jerryRocket,
            jerryBoostVert,
            bonzoDirect,
            bonzoVert,
            bonzoBoostHori,
            bonzoBoostVert,
            cancelHighLavaBounce,
            fullDisableWithStaff
        )
    }

    private var bonzoTime = 0L
    private var lavaTime = 0L
    private const val lavaTimeout = 1000L

    private val moveKeys: Map<KeyBinding, Int>
        get() {
            return mapOf(
                mc.gameSettings.keyBindForward to 0b0001,
                mc.gameSettings.keyBindRight to 0b0100,
                mc.gameSettings.keyBindBack to 0b0011,
                mc.gameSettings.keyBindLeft to 0b1100,
            )
        }


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
        if (entity != mc.thePlayer) return
        if (!inSkyblock || shouldTakeKb()) return

        // Jerry chine will always give +4800 motionY and bonzo Staff always +4000
        // Lava bounce will always give +28000 motionY
        // Bonzo Staff will always have in between 159_000_000 and 160_000_000 total momentum square
        val totalMomentumSquare =
            (event.packet.motionX * event.packet.motionX + event.packet.motionY * event.packet.motionY + event.packet.motionZ * event.packet.motionZ)
        run kbCheck@ {
            // Check for lava bounce
            if ((event.packet.motionY == 28000 && (if (cancelHighLavaBounce.enabled) mc.thePlayer.isInLava else System.currentTimeMillis() < lavaTime)))
                return
            //Jerry gun
            else if (mc.thePlayer.heldItem?.itemID.equals("JERRY_STAFF") && event.packet.motionY == 4800) {
                if (jerryRocket.enabled) {
                    mc.thePlayer.motionY = event.packet.motionY * jerryBoostVert.value / 8000.0
                } else {
                    mc.thePlayer.setVelocity(
                        mc.thePlayer.motionX,
                        event.packet.motionY * jerryBoostVert.value / 8000.0,
                        mc.thePlayer.motionZ
                    )
                }
            }
            // Check bonzo staff
            else if (event.packet.motionY == 4000 && totalMomentumSquare in 159_000_000..160_000_000 && System.currentTimeMillis() < bonzoTime){

                var newVelo = Vec3(Vec3i(event.packet.motionX, event.packet.motionY, event.packet.motionZ))
                    .scale(1/8000.0)
                    .scaleHorizontal(bonzoBoostHori.value)
                    .scaleVertical(bonzoBoostVert.value)
                if (bonzoDirect.enabled){
                    val yaw = mc.thePlayer.rotationYaw
                    // last bit dentoes whether forward / back wards at all
                    // second bit dentoes the direction.
                    // thrid bit denotes whether to go left / right
                    // fourth bit denotes the direction
                    var direction = 0b0000
                    moveKeys.forEach { (bind, value) ->
                        if (bind.isKeyDown) direction = direction xor value
                    }
                    // Break if no key pressed
                    if (direction and 0b0101 == 0b0000) {
                        if (bonzoVert.enabled)
                            newVelo = newVelo.scaleHorizontal(0.0)
                        else
                            return@kbCheck
                    }

                    val forward = (if (direction and 0b0011 == 0b0011) 180 else 0)
                    val sidewards =
                        (if ((direction and 0b0101) == 0b0101) 45 else if ((direction and 0b0100) == 0b0100) 90 else 0) *
                                (if (direction and 0b1000 == 0b1000) -1 else +1)
                    val angle = forward + sidewards * (if (forward > 0) -1 else 1)
                    val newAngle = yaw + angle
                    newVelo = newVelo.rotateYawTo(newAngle)
                }
                mc.thePlayer.setVelocity(
                    newVelo.xCoord,
                    newVelo.yCoord,
                    newVelo.zCoord
                )
            }
            else if (fullDisableWithStaff.enabled && mc.thePlayer?.heldItem?.itemID.equalsOneOf("BONZO_STAFF", "STARRED_BONZO_STAFF", "JERRY_STAFF"))
                return
            // Check for spring boots, if worn allow vertical kb
            else if (mc.thePlayer.getEquipmentInSlot(1)?.itemID.equals("TARANTULA_BOOTS")
                || (!mc.thePlayer.isSneaking && mc.thePlayer.getEquipmentInSlot(1)?.itemID.equals("SPRING_BOOTS"))
            ) {
                mc.thePlayer.setVelocity(
                    mc.thePlayer.motionX,
                    event.packet.motionY / 8000.0,
                    mc.thePlayer.motionZ
                )
            }
        }
        event.isCanceled = true
    }

    private fun shouldTakeKb(): Boolean {
        return mc.thePlayer.heldItem?.itemID.equalsOneOf("LEAPING_SWORD", "SILK_EDGE_SWORD")
    }

    //"JERRY_STAFF","BONZO_STAFF", "STARRED_BONZO_STAFF",
}