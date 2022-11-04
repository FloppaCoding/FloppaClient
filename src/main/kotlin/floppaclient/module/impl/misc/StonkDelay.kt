package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.BlockDestroyEvent
import floppaclient.events.BlockStateChangeEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils.removeIf
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object StonkDelay : Module(
    "Stonk Delay",
    category = Category.MISC,
    description = "Delays mined blocks being placed back."
){
    private val delay = NumberSetting("Delay", 500.0, 0.0, 2000.0, 10.0, description = "The minimum delay in ms before mined blocks can be placed back.")
    private val ghost = BooleanSetting("Ghost", false, description = "Will create ghost blocks instead of actually mining the block.")

    init {
        this.addSettings(
            delay,
            ghost
        )
    }

    private val buffer = mutableMapOf<BlockPos,BrokenBlock>()

    @SubscribeEvent
    fun onBlockDestroyed(event: BlockDestroyEvent) {
        if (!inSkyblock) return
        buffer[event.pos] = BrokenBlock(event.pos, event.state, System.currentTimeMillis())
    }

    @SubscribeEvent
    fun onBlockUpdate(event: BlockStateChangeEvent) {
        if (buffer.isEmpty()) return
        buffer[event.pos]?.run {
            if (this.placing || event.newState.block != this.state.block) return@run false
            this.state = event.newState
            if (!ghost.enabled) this.shouldPutBack = true
            event.isCanceled = true
            return@run true
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if(event.phase != TickEvent.Phase.START || buffer.isEmpty()) return
        val time = System.currentTimeMillis()
        buffer.removeIf{ (_, data) ->
            data.tryPlaceBack(time)
        }
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        buffer.clear()
    }

    internal class BrokenBlock(val pos: BlockPos, var state: IBlockState, private val breakTime: Long) {
        /**
         * Tells the filter that this block should be allowed to be placed
         */
        var placing = false
        /**
         * By default, blocks will not be placed back, only if they actually get replaced.
         */
        var shouldPutBack = false
        /**
         * If enough time has passed places back the buffered Block.
         * @return True when a block was placed, or it is timed out, false otherwise.
         */
        fun tryPlaceBack(now: Long): Boolean {
            return if (now >= breakTime +  delay.value.toLong()) {
                if (shouldPutBack) {
                    placing = true
                    mc.theWorld.setBlockState(pos, state)
                }
                true
            }else false
        }
    }
}