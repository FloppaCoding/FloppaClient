package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ClipTools
import floppaclient.utils.GeometryUtils.scale
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object BarPhase : Module(
    "Bar Phase",
    category = Category.PLAYER,
    description = "Lets you phase through iron bars and glass panes by walking against them."
){
    private val phaseDelay = NumberSetting("Phase Delay", 0.0, 0.0, 5.0, 1.0, description = "Determines how long you have to walk against iron bars for it to trigger the phase through. 0 works fine, but higher delays give you more control.")
    private val onlyInBomb = BooleanSetting("Only in Bomb", false, description = "Will only enable this module in the bomb puzzle room.")

    init {
        this.addSettings(
            phaseDelay,
            onlyInBomb
        )
    }

    private var phaseTicks = 0

    // These values are also in QOL
    const val minCoord = 0.446f
    const val maxCoord = 0.5455f
    const val range = 0.00015

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent){
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return
        if (onlyInBomb.enabled) {
            val room = Dungeon.currentRoomPair ?: return
            if (room.first.data.name != "Bomb Defuse") return
        }
        val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        if (mc.thePlayer.isCollidedHorizontally
            && ( mc.theWorld.getBlockState(pos)
                .block.equalsOneOf(Blocks.iron_bars, Blocks.glass_pane, Blocks.stained_glass_pane)
                    || mc.theWorld.getBlockState(pos.up())
                .block.equalsOneOf(Blocks.iron_bars, Blocks.glass_pane, Blocks.stained_glass_pane))
        ){
            val dir = direction()
            if(dir == null) {
                phaseTicks = 0
                return
            }
            val loc = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
            // perpendicular to the direction the player will be clipped in
            val offsVec = Vec3(0.0, 1.0,0.0).crossProduct(Vec3(dir.directionVec)).scale(0.3)


            val flag = mc.theWorld.getBlockState(BlockPos(loc.add(offsVec)).offset(dir)).block === Blocks.air
                    && mc.theWorld.getBlockState(BlockPos(loc.subtract(offsVec)).offset(dir)).block === Blocks.air
                    && mc.theWorld.getBlockState(BlockPos(loc.add(offsVec)).offset(dir).up()).block === Blocks.air
                    && mc.theWorld.getBlockState(BlockPos(loc.subtract(offsVec)).offset(dir).up()).block === Blocks.air
            if (flag && phaseTicks >= phaseDelay.value){
                ClipTools.hClip(0.7,dir.horizontalIndex * 90f)
                phaseTicks = 0
            }else phaseTicks++
        }else phaseTicks = 0
    }

    private fun direction(): EnumFacing?{
        return  when {
            inRange(mc.thePlayer.posX, minCoord -0.3f) -> EnumFacing.EAST
            inRange(mc.thePlayer.posZ, minCoord -0.3f) -> EnumFacing.SOUTH
            inRange(mc.thePlayer.posX, maxCoord +0.3f) -> EnumFacing.WEST
            inRange(mc.thePlayer.posZ, maxCoord +0.3f) -> EnumFacing.NORTH
            else -> null
        }
    }

    private fun inRange(a: Double, coord: Float): Boolean {
        val b = if (a > 0)
            a % 1
        else
            1 + a % 1
        return b % 1 > coord - range && b % 1 < (coord + range)
    }


}