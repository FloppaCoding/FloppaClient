package floppaclient.module.impl.render

import floppaclient.FloppaClient
import floppaclient.floppamap.core.AutoActionData
import floppaclient.floppamap.core.Room
import floppaclient.floppamap.core.RoomData
import floppaclient.floppamap.core.RoomType
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.impl.misc.ClipSettings
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.DataHandler
import floppaclient.utils.GeometryUtils.cosDeg
import floppaclient.utils.GeometryUtils.getDirection
import floppaclient.utils.GeometryUtils.sinDeg
import floppaclient.utils.render.WorldRenderUtils
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor

/**
 * This module is aimed at rendering visual aids for automated actions.
 *
 * @author Aton
 */
object DrawRoutes : Module(
    "Draw Routes",
    category = Category.RENDER,
    description = "Renders the clip routes."
){

    private val renderClip  = BooleanSetting("Clip Routes",true)
    private val renderEther = BooleanSetting("Etherwarp Routes",true)
    private val renderCmd   = BooleanSetting("Command Points",true, description = "Renders the command points for the current room.")
    private val onlyInBoss  = BooleanSetting("Only in Boss",false)
    private val acLineWidth         = NumberSetting("Clip Line Width", 3.0, 0.1, 10.0, 0.1, description = "Line width for Auto Clip routes.")
    private val acStartColor        = ColorSetting("Clip Start", Color(255, 0, 0), true, description = "Color for the start block of Auto Clip routes.")
    private val acTargetColor       = ColorSetting("Clip Target", Color(0, 255, 0), true, description = "Color for the final and intermediate target blocks of Auto Clip routes.")
    private val acStepColor         = ColorSetting("Clip Step", Color(0, 255, 255), true, description = "Color for automatically created intermediate steps of Auto Clip routes.")
    private val acPathColor         = ColorSetting("Clip Path", Color(0, 0, 255), true, description = "Color for the path that the Auto Clip route follows.")
    private val etherLineWidth      = NumberSetting("Ether Line Width", 3.0, 0.1, 10.0, 0.1, description = "Line width for Etherwarp routes.")
    private val etherStartColor     = ColorSetting("Ether Start", Color(255, 115, 0), true, description = "Color for the start block of Etherwarp routes.")
    private val etherTargetColor    = ColorSetting("Ether Target", Color(0, 82, 75), true, description = "Color for the target Block of Etberwarp routes.")
    private val etherPathColor      = ColorSetting("Ether Path", Color(255, 0, 255), true, description = "Color for the path that the Etherwarp route follows.")
    private val cmdLineWidth        = NumberSetting("Cmd Line Width", 3.0, 0.1, 10.0, 0.1, description = "Line width for command points.")
    private val cmdPointColor       = ColorSetting("Cmd Point", Color(255, 255, 0), true, description = "Color for the command points.")


    init {
        this.addSettings(
            renderClip,
            renderEther,
            onlyInBoss,
            acLineWidth,
            acStartColor,
            acTargetColor,
            acStepColor,
            acPathColor,
            etherLineWidth,
            etherStartColor,
            etherTargetColor,
            etherPathColor,
            cmdLineWidth,
            cmdPointColor
        )
    }

    override fun onEnable() {
        reset()
        super.onEnable()
    }

    private var room = Pair(Room(0,0, RoomData("Unknown", RoomType.ENTRANCE)),0)
    private var autoClipData: AutoActionData? = null

    /**
     * Updates the variables.
     */
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !this.enabled) return
        room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return
        autoClipData = RoomUtils.getRoomAutoActionData(room.first)
    }

    /**
     * Initiates the route rendering.
     */
    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (autoClipData == null || !this.enabled  || (!Dungeon.inBoss && onlyInBoss.enabled)) return
        synchronized(autoClipData!!) {
            if (renderClip.enabled) autoClipData!!.clips.forEach { (key, route) ->
                drawRoute(key, route)
            }
            if (renderEther.enabled) autoClipData!!.etherwarps.forEach{ (key, value) ->
                drawEther(key, value)
            }
            if (renderCmd.enabled) autoClipData!!.autocmds.forEach { (key, value) ->
                drawCmdPoint(key, value)
            }
        }

    }

    private fun drawRoute(key: MutableList<Int>, route: MutableList<Double>) {

        // Start coordinates for the clip route
        val start = DataHandler.getRotatedCoords(
            Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
        ).addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())

        //Draw start box
        WorldRenderUtils.drawBoxAtBlock(start.xCoord, start.yCoord-1.0 , start.zCoord, acStartColor.value, acLineWidth.value.toFloat())

        //Draw lines and target boxes for all paths (one or multiple clips along a straight line) that are part of the route
        var direction: Array<Double>
        val dist = ClipSettings.baseClipDistance.value
        var path: Vec3
        var starPos = start.addVector(0.5, 0.0, 0.5)
        for (j in 0 until route.size / 3) {
            path = DataHandler.getRotatedCoords(
                Vec3(
                    route[3 * j], route[3 * j + 1], route[3 * j + 2]
                ), room.second
            )
            // Draw the line
            WorldRenderUtils.drawLine(starPos.xCoord, starPos.yCoord, starPos.zCoord, starPos.xCoord + path.xCoord,
                starPos.yCoord + path.yCoord, starPos.zCoord + path.zCoord, acPathColor.value, acLineWidth.value.toFloat())

            // Test whether path will be split up and render intermediate steps
            direction = getDirection(0.0, 0.0, 0.0, path.xCoord, path.yCoord, path.zCoord)
            val yaw = direction[1]
            val pitch = direction[2]
            if(direction[0] > dist) {
                for(ii in 1..floor(abs(direction[0] / dist)).toInt()) {
                    // Draw intermediate boxes
                    WorldRenderUtils.drawBoxAtBlock(starPos.xCoord - sinDeg(yaw) * cosDeg(pitch) *dist*ii -0.5,
                        starPos.yCoord - sinDeg(pitch) *dist*ii - 1,
                        starPos.zCoord + cosDeg(yaw) * cosDeg(pitch) *dist*ii - 0.5, acStepColor.value, acLineWidth.value.toFloat())
                }
            }

            // Draw end location box
            WorldRenderUtils.drawBoxAtBlock(starPos.xCoord + path.xCoord -0.5,
                starPos.yCoord + path.yCoord - 1,
                starPos.zCoord + path.zCoord- 0.5, acTargetColor.value, acLineWidth.value.toFloat())

            // Set the start position to the target position of this path for next iteration
            starPos = starPos.add(path)
        }
    }

    private fun drawEther(key: MutableList<Int>, value: BlockPos) {

        // Start coordinates for the clip route
        val start = DataHandler.getRotatedCoords(
            Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
        ).addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())

        //Draw start box
        WorldRenderUtils.drawBoxAtBlock(start.xCoord, start.yCoord , start.zCoord, etherStartColor.value, etherLineWidth.value.toFloat())

        val target = DataHandler.getRotatedCoords(
            value, room.second)
            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
//        target = target.addVector(DataHandler.isNegative(target.xCoord), 0.0, DataHandler.isNegative(target.zCoord))

        //Draw target box
        WorldRenderUtils.drawBoxAtBlock(target.xCoord, target.yCoord , target.zCoord, etherTargetColor.value, etherLineWidth.value.toFloat())

        val starPos = start.addVector(0.5, 1.0, 0.5)
        val targetPos = target.addVector(0.5, 1.0, 0.5)

        // Draw the line
        WorldRenderUtils.drawLine(starPos.xCoord, starPos.yCoord, starPos.zCoord, targetPos.xCoord,
            targetPos.yCoord, targetPos.zCoord, etherPathColor.value, etherLineWidth.value.toFloat())

    }

    private fun drawCmdPoint(key: MutableList<Int>, value: String) {

        // Start coordinates for the clip route
        val start = DataHandler.getRotatedCoords(
            Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
        ).addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())

        //Draw start box
        WorldRenderUtils.drawBoxAtBlock(start.xCoord, start.yCoord , start.zCoord, cmdPointColor.value, cmdLineWidth.value.toFloat())
        //TODO: Draw Cmd as a name tag

    }




    /**
     * Initiates reset in warp.
     */
    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        reset()
    }

    /**
     * Resets the data in the module
     */
    private fun reset() {
        room = Pair(Room(0,0, RoomData("Unknown", RoomType.ENTRANCE)), 0)
        autoClipData = null
    }
}