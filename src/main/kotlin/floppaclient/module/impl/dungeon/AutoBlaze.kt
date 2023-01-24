package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.events.PositionUpdateEvent
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.funnymap.features.extras.EditMode
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ClipTools
import floppaclient.utils.DataHandler
import floppaclient.utils.DataHandler.toMutableIntList
import floppaclient.utils.GeometryUtils.getDirection
import floppaclient.utils.Utils
import floppaclient.utils.Utils.isHolding
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.fakeactions.FakeActionManager
import floppaclient.utils.fakeactions.FakeActionUtils
import kotlinx.coroutines.runBlocking
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.Vec3
import net.minecraft.world.World
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.text.DecimalFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * This module is made to automatically complete the Blaze puzzle in dungeons.
 *
 * @author Aton
 */
object AutoBlaze : Module(
    "Auto Blaze",
    category = Category.DUNGEON,
    description = "Automatically completes the blaze puzzle. Activated by left clicking with either AOTV or terminator after walking far enough into the room."
){

    private val fastSleep = NumberSetting("Sleep at 100 ats", 310.0, 200.0, 1000.0, 10.0)
    private val mediumSleep = NumberSetting("Sleep above 50 ats", 610.0, 200.0, 1000.0, 10.0)
    private val slowSleep = NumberSetting("Sleep below 50 ats", 810.0, 450.0, 1000.0, 10.0)
    private val offset = NumberSetting("y offset", 0.25, -1.0, 1.0, 0.01)
    private val checkPosition = BooleanSetting("Check Pos", false, description = "Interrupts the process if you move out of the correct position.")
    private val forceRotate = BooleanSetting("Pre rotate", false, description = "Rotates to the next blaze before doing the shot.")
    private val realRotate = BooleanSetting("Real rotate", false, description = "Rotates to the next blaze before doing the shot.")

    /**
     * Determines the order in which the blazes have to be sorted.
     */
    private var topDown: Boolean? = null
    private var rotation = 0
    var doingBlaze = false
    private var orderedBlazes = ArrayList<ShootableBlaze>()
    private var shotBlazes = ArrayList<ShootableBlaze>()
    private var impossible = false
    private var lastShot = System.currentTimeMillis()
    var startTime = System.currentTimeMillis()
    private var bowSlot = 1
    private var expectedTimeToHit = 0.0
    private val etherStart = Vec3(-5.0, 69.0, 0.0)
    private const val detRange = 3.0
    private const val HEIGHT_OFFS = 50
    private val etherTarget = listOf(
        Vec3(3.0, 45.0, 6.0),
        Vec3(-2.0, 48.0, 7.0),
    )
    private val startPos = listOf(
        etherTarget.first().addVector(0.0, 1.0, 0.0).toMutableIntList(),
        etherTarget.first().addVector(0.0, 1.0 + HEIGHT_OFFS, 0.0).toMutableIntList(),
    )
    private val startPos2 = listOf(
        etherTarget.last().addVector(0.0, 1.0, 0.0).toMutableIntList(),
        etherTarget.last().addVector(0.0, 1.0 + HEIGHT_OFFS, 0.0).toMutableIntList(),
    )
    private val blockVec = Vec3(3.0, -23.0, 6.0)
    private val behindCornerBottom = Vec3(0.0, 75.0, -1.0)
    private val behindCornerTop = Vec3(-1.0, 110.0, -3.0)

    init{
        this.addSettings(
            fastSleep,
            mediumSleep,
            slowSleep,
            offset,
            checkPosition,
            forceRotate,
            realRotate
        )
    }

    /*
    The Blaze solver:
    The best spot that I have found to complete the puzzle from is a corner by the waterfall. The room relative
    coordinates for that spot are 3, 46/96, 6 with the y level depending on whether it is ascending or descending order.
    However that position does not always work. Sometimes the blazes are moved a bit, such that some are behind the iron
    bars in the center of the room. That is the case when there are blazes in the box from -2 25 0 to -3 110 1.
    In that case a different spot: 6, 49 / 99, 3 works better.
     */


    /**
     * For left click detection with aotv to activate the action.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (EditMode.enabled || !inDungeons || topDown == null) return
        if (!mc.thePlayer.isHolding("Aspect of the Void") && !mc.thePlayer.isHolding("Terminator")) return
        val room = Dungeon.currentRoomPair ?: return
        if (room.first.data.name != "Blaze" && room.first.data.name != "Blaze 2") return
        val relPos = DataHandler.getRelativeCoords(mc.thePlayer.positionVector, room.first.x, room.first.z, -rotation)
        if (relPos.distanceTo(etherStart) > detRange) return
        val offs = if (topDown == true) 0 else HEIGHT_OFFS
        var target = BlockPos(DataHandler.getRotatedCoords(etherTarget[0], -rotation))
            .add(room.first.x, 0, room.first.z)

        val cornerBot = DataHandler.getRotatedCoords(behindCornerBottom, -rotation)
            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
        val cornerTop = DataHandler.getRotatedCoords(behindCornerTop, -rotation)
            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
        val aabb = AxisAlignedBB(
            cornerBot.xCoord,
            cornerBot.yCoord,
            cornerBot.zCoord,
            cornerTop.xCoord,
            cornerTop.yCoord,
            cornerTop.zCoord
        )
        val blazes = mc.theWorld.getEntitiesWithinAABB(
            EntityBlaze::class.java, aabb
        )
        val fakePlayer = EntityOtherPlayerMP(mc.theWorld as World, mc.thePlayer.gameProfile)
        fakePlayer.setPosition(target.x + 0.5, target.y + 1.0, target.z + 0.5)
        mc.theWorld.addEntityToWorld(-3357468, fakePlayer)

        val areBlazeObscured = kotlin.run {
            blazes.forEach {
                if(!it.canEntityBeSeen(fakePlayer)) return@run true
            }
            return@run false
        }
        mc.theWorld.removeEntityFromWorld(-3357468)
        if (areBlazeObscured) {
            target = BlockPos(DataHandler.getRotatedCoords(etherTarget[1], -rotation))
                .add(room.first.x, 0, room.first.z)
        }
        target = target.up(offs)

        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionY = -0.0784000015258789
        mc.thePlayer.motionZ = 0.0

        FakeActionUtils.etherwarpTo(target, true)

        event.isCanceled = true
    }

    /**
     * Detects etherwarp sound and initiats the auto blaze process when ehterwarped to the correct location.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onSoundPlay(event: PlaySoundSourceEvent) {
        if (!inDungeons || doingBlaze || topDown == null) return
        if (event.name == "mob.enderdragon.hit") {
            val pos = Vec3(
                floor(mc.thePlayer.posX),
                floor(mc.thePlayer.posY),
                floor(mc.thePlayer.posZ)
            )

            val room = Dungeon.currentRoomPair ?: return
            if (room.first.data.name != "Blaze" && room.first.data.name != "Blaze 2") return
            val key = DataHandler.getKey(
                pos,
                room.first.x,
                room.first.z,
                -rotation
            )

            // If in a valid position start auto blaze
            val angle: Int
            val route = if (startPos.contains(key)) {
                angle = 0
                mutableListOf(-0.1, 0.0, -0.1, -0.1, 0.0, -0.1, -0.1, 0.0, -0.1, -0.1, 0.0, -0.1, -0.1, 0.0, -0.1)
            } else if (startPos2.contains(key)) {
                 angle = 48
                mutableListOf(-0.1, 0.0, -0.125, -0.05, 0.0, -0.125, 0.0, 0.0, -0.125, 0.0, 0.0, -0.125, 0.0, 0.0, -0.125)
            } else return

            modMessage("Starting Auto Blaze.")
            // clip to the correct position
            val expDelay = ClipTools.executeClipRoute(route, -rotation, startDelay = 50)
            bowSlot = Utils.findItem("Terminator") ?: Utils.findItem("Shortbow") ?: return modMessage("No Shortbow found in your hotbar!")
            mc.thePlayer.inventory.currentItem = bowSlot
            startTime = System.currentTimeMillis()
            mc.thePlayer.rotationYaw = 155f - rotation.toFloat() + angle
            mc.thePlayer.rotationPitch = 0f

            Timer().schedule(expDelay + 50) {
                doingBlaze = true
            }

        }
    }

    /**
     * Solved the blaze puzzle.
     * Based on Skytils Blaze solver
     */
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) = runBlocking {
        if (event.phase != TickEvent.Phase.START || !inDungeons) return@runBlocking
        val room = Dungeon.currentRoomPair ?: return@runBlocking
        if (room.first.data.name != "Blaze" && room.first.data.name != "Blaze 2") return@runBlocking

        // detect the order if not already done
        if (topDown == null) {
            topDown = when (mc.theWorld.getBlockState(BlockPos( room.first.x, 68, room.first.z)).block) {
                Blocks.air, Blocks.iron_bars -> true
                else -> false
            }
            val yLevel = if(topDown == true) {
                68
            } else {
                118
            }
            rotation = if (mc.theWorld.getBlockState(BlockPos(room.first.x, yLevel, room.first.z-7)).block != Blocks.air)
                270
            else if (mc.theWorld.getBlockState(BlockPos(room.first.x+7, yLevel, room.first.z)).block != Blocks.air)
                180
            else if (mc.theWorld.getBlockState(BlockPos(room.first.x, yLevel, room.first.z+7)).block != Blocks.air)
                90
            else
                0

            // set the start block to diamond
            val position = DataHandler.getRotatedCoords(blockVec, -rotation)
                .addVector(room.first.x.toDouble(), yLevel.toDouble(), room.first.z.toDouble())
            mc.theWorld.setBlockState(BlockPos(position), Blocks.diamond_block.defaultState)
        }

        // get sorted blaze list
        orderedBlazes.clear()
        mc.theWorld.loadedEntityList.stream()
            .filter { entity ->
                entity is EntityArmorStand && entity.getName().contains("Blaze") && entity.getName()
                    .contains("/")
                        && entity.getDistance(room.first.x.toDouble(), entity.posY, room.first.z.toDouble()) < 15
            }.forEach { entity ->
                val blazeName = EnumChatFormatting.getTextWithoutFormattingCodes(entity.name)
                try {
                    val health =
                        blazeName.substringAfter("/").dropLast(1).toInt()
                    val aabb = AxisAlignedBB(
                        entity.posX - 0.5,
                        entity.posY - 2,
                        entity.posZ - 0.5,
                        entity.posX + 0.5,
                        entity.posY,
                        entity.posZ + 0.5
                    )
                    val blazes = mc.theWorld.getEntitiesWithinAABB(
                        EntityBlaze::class.java, aabb
                    )
                    if (blazes.isEmpty()) return@forEach
                    orderedBlazes.add(ShootableBlaze(blazes[0], health))
                } catch (ex: NumberFormatException) {
                    ex.printStackTrace()
                }
            }
        orderedBlazes.sortWith { blaze1, blaze2 ->
            val compare = blaze1.health.compareTo(blaze2.health)
            if (compare == 0 && !impossible) {
                impossible = true
                modMessage("§cDetected two blazes with the exact same amount of health!")
                val first = blaze1.blaze.health
                val second = blaze2.blaze.health
                if (first.toInt() == second.toInt()) return@sortWith first.compareTo(second)
            }
            return@sortWith compare
        }
    }

    /**
     * Plans and stages the next shot.
     */
    @SubscribeEvent
    fun updateBlaze(event: PositionUpdateEvent.Pre) {
        if (!doingBlaze || !inDungeons) return

        // check whether the player is a good position, and if not abort
        if (checkPosition.enabled && mc.thePlayer.posX.getDecimals() in 0.15 .. 0.85) {
            doingBlaze = false
            modMessage("Aborted Auto Blaze due to failed positioning.")
            return
        }

        // check whether there are blazes left, if not stop
        if (orderedBlazes.minus(shotBlazes.toSet()).isEmpty()) {
            doingBlaze = false
            modMessage("Finished Auto Blaze in ${DecimalFormat("#.##").format((System.currentTimeMillis() - startTime).toDouble()/1000.0)} seconds.")
            return
        }


        // plan the shot
        // return if already doing something else
        if (FakeActionManager.doAction) return

        val sorting = topDown ?: return
        val shootableBlaze = when(sorting){
            true -> orderedBlazes.minus(shotBlazes.toSet()).last()
            false -> orderedBlazes.minus(shotBlazes.toSet()).first()
        }
        val target = shootableBlaze.blaze
        val direction = getDirection(
            mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ,
            target.posX, target.posY + offset.value, target.posZ
        )

        if (forceRotate.enabled){
            mc.thePlayer.rotationYaw = direction[1].toFloat()
            mc.thePlayer.rotationPitch = direction[2].toFloat()
        }

        // the expected delay when the next shot can be fired. offset by 100ms to account for errors in flight time estimation.
        val attackSpeed = Utils.getAttackspeed()
        val minDelay = if (attackSpeed == 100) {
            fastSleep.value
        } else if ((attackSpeed ?: 0) >= 50) {
            mediumSleep.value
        }else {
            slowSleep.value
        }
        val delay = max(100 + expectedTimeToHit - flightTime(direction[0]), minDelay)
        if (System.currentTimeMillis() - lastShot < delay) return

        if (realRotate.enabled){
            mc.thePlayer.rotationYaw = direction[1].toFloat()
            mc.thePlayer.rotationPitch = direction[2].toFloat()
        }

        // find shortbow; if none found stop
        val slot = Utils.findItem("Terminator") ?: Utils.findItem("Shortbow")
        if (slot == null) {
            doingBlaze = false
            modMessage("No Shortbow found in your hotbar. Stopped Auto Blaze!")
            return
        }
        bowSlot = slot
        // execute the shot
//        modMessage("shooting at ${target.posX.toString() + "," + target.posY.toString() + "," + target.posZ.toString()} " +
//                "with direction ${direction[1].toString() + "," + direction[2].toString()} ")
        FakeActionManager.stageLeftClickSlot(direction[1].toFloat(), direction[2].toFloat(), bowSlot)
        lastShot = System.currentTimeMillis()

        shotBlazes.add(shootableBlaze)

        //swap to bow for attackspeed (might not be needed idk)
        mc.thePlayer.inventory.currentItem = bowSlot

        // calculte the expected travel time for this arrow
        expectedTimeToHit = flightTime(direction[0])

    }

    /**
     * Approximates the time in ms an arrow needs to travel the given distance.
     * Gravity and drag are neglected.
     */
    private fun flightTime(distance: Double): Double {
    // reference values:
    // initial speed: 3.0 blocks per tick
    // drag: velocity multiplied by 0.99 each tick
    // gravity: accelerated with 1 block / second^2
        return (distance * 50.0 / 3.0)
    }

    fun checkBlazes () {
        if (orderedBlazes.size < 1) return
        orderedBlazes.forEach{
            modMessage(it.blaze.positionVector.toString() + ", " + it.blaze.canEntityBeSeen(mc.thePlayer).toString() + ", " + mc.thePlayer.canEntityBeSeen(it.blaze).toString())
        }
    }


    private fun Double.getDecimals(): Double =
        abs(this) - floor(abs(this))


    data class ShootableBlaze(@JvmField var blaze: EntityBlaze, var health: Int)

    /**
     *  reset on warp
     */
    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        doingBlaze = false
        topDown = null
        orderedBlazes.clear()
        shotBlazes.clear()
        impossible = false
        expectedTimeToHit = 0.0
    }

    /**
     * Force stops the solving process.
     */
    fun stop() {
        doingBlaze = false
    }
}