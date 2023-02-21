package floppaclient.utils

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.impl.misc.ClipSettings
import floppaclient.utils.GeometryUtils.getDirection
import floppaclient.utils.GeometryUtils.yaw
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.util.Vec3
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

/**
 * Collection of methods used for clipping
 *
 * @author Aton
 */
object ClipTools {

    private fun getDelay(index: Int): Long {
        return longArrayOf(
            0, ClipSettings.clipDelay1.value.toLong(), ClipSettings.clipDelay2.value.toLong(), ClipSettings.clipDelay3.value.toLong(),
            ClipSettings.clipDelay4.value.toLong(), ClipSettings.clipDelay5.value.toLong(), ClipSettings.clipDelay6.value.toLong(),
            ClipSettings.clipDelay7.value.toLong(), ClipSettings.clipDelay8.value.toLong(), ClipSettings.clipDelay9.value.toLong()
        )[index]
    }

    fun dClip(dist: Double, yaw: Float = yaw(), pitch: Float = 0.0f) {
        clip(-GeometryUtils.sinDeg(yaw) * GeometryUtils.cosDeg(pitch) *dist,
            -GeometryUtils.sinDeg(pitch) *dist, GeometryUtils.cosDeg(yaw) * GeometryUtils.cosDeg(pitch) *dist)
    }

    fun hClip(dist: Double, yaw: Float = yaw(), yOffs: Double = 0.0) {
        clip(-GeometryUtils.sinDeg(yaw) *dist, yOffs, GeometryUtils.cosDeg(yaw) *dist)
    }

    /**
     * Teleport relative to the current position.
     */
    fun clip(x: Double, y: Double, z: Double) {
        teleport(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z)
    }

    /**
     * Teleport to the specified coordinates.
     */
    fun teleport(x: Double, y: Double, z: Double) {
        // check whether inputs are NaN to prevent kick
        if(x.isNaN() || y.isNaN() || z.isNaN()) {
            modMessage("§cArgument error")
        }else {
            mc.thePlayer.setPosition(x, y, z)
        }
    }

    private fun farclip(distTotal: Double, yaw: Double = yaw().toDouble(), pitch: Double = GeometryUtils.pitch().toDouble(), startii: Int = 0, delayOffs: Long = 0): Int {
        return farclip(distTotal,yaw.toFloat(), pitch.toFloat(), startii, delayOffs)
    }

    private fun farclip(distTotal: Double, yaw: Float = yaw(), pitch: Float = GeometryUtils.pitch(), startii: Int = 0, delayOffs: Long = 0): Int { //directional clip towards where you are facing in 3D
        val dist = ClipSettings.baseClipDistance.value * sign(distTotal)
        val n = floor(abs(distTotal / dist)).toInt()
//        val timer = Timer()
        if(startii + n > 10) {
            modMessage("§cError: specified distance exceeds max set distance of "
                    + ClipSettings.baseClipDistance.value*10 + " Blocks")
            return startii + n
        }
        if(n > 0 && startii == 0) {
            if (delayOffs == 0L) {
                dClip(dist, yaw, pitch) // first clip without set timeout required to make it faster, setTimeout seems to add about 40ms delay when 0ms are specified
            } else{
                Timer().schedule(delayOffs) {
                    dClip(dist, yaw, pitch)
                }
            }
        }
        for(ii in startii.coerceAtLeast(1)..(n + startii-1).coerceAtMost(9)) { //max 10 iterations, further not possible
            Timer().schedule(getDelay(ii) + delayOffs) {
                dClip(dist, yaw, pitch)
            }
        }
        val remainder = distTotal % abs(dist)
        if(startii + n < 10 && remainder != 0.0) { // there is still distance left to cover (smaller than the base distance) and it has been less than the maximum amount of clips
            if(startii + n + delayOffs == 0L) { //no clip has been executed so far and also no delay is needed; the total distance is smaller than the base distance
                dClip(remainder, yaw, pitch)
            }else {
                Timer().schedule(getDelay(startii + n) + delayOffs) {
                    dClip(remainder, yaw, pitch)
                }
            }
        }
        return startii + n + remainder.compareTo(0.0) // compareTo outputs 1 if bigger, 0 if same, -1 if smaller
    }

    /**
     * Performs the clip route given as route, starting at startPos.
     * The argument relative determines, whether the coordinates in route are relative to the previous position
     * or absolute. The startPos and route coordinates all get rotated according to rotation.
     */
    fun executeClipRoute(route: MutableList<Double>,
                         rotation: Int = 0,
                         startDelay: Int = 0,
                         relative: Boolean = true,
                         startPos: MutableList<Double> = mutableListOf(0.0, 0.0, 0.0),
                         delayOffs: Int = 0
    ) : Long{
        var direction: Array<Double>
        val steps = mutableListOf<Int>()
        steps.add(delayOffs)
        var pos0 = DataHandler.getRotatedCoords(Vec3(startPos[0], startPos[1], startPos[2]), rotation)
        for (j in 0 until (route.size) / 3) {
            val pos1 = DataHandler.getRotatedCoords(
                Vec3(
                    route[3 * j], route[3 * j + 1], route[3 * j + 2]
                ), rotation
            )
            direction = getDirection(pos0.xCoord, pos0.yCoord, pos0.zCoord, pos1.xCoord, pos1.yCoord, pos1.zCoord)
            if (!relative) pos0 = pos1
            steps.add(farclip(direction[0],direction[1],direction[2],steps[j],startDelay.toLong()))
        }
        return startDelay + getDelay(steps.last())
    }
}