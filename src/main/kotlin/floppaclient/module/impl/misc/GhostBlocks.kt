package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.display
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

/**
 * Set blocks to air client side
 * @author Aton
 */
object GhostBlocks : Module(
 "Ghost Blocks",
    category = Category.MISC,
    description = "Creates ghost blocks where you are looking when the key bind is pressed."
){
    private val ghostBlockSkulls = BooleanSetting("Ghost Skulls", true, description = "If enabled skulls will also be turned into ghost blocks.")

    init {
        this.addSettings(ghostBlockSkulls)
    }

    private val blacklist = listOf(
        Blocks.stone_button,
        Blocks.chest,
        Blocks.trapped_chest,
        Blocks.lever
    )

    /**
     * Prevent the key bind from toggling the module, so that it can be used here.
     */
    override fun keyBind() { }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || display != null) return
        if (this.keyCode > 0 && !Keyboard.isKeyDown(this.keyCode)) return
        if (this.keyCode < 0 && !Mouse.isButtonDown(this.keyCode + 100)) return
        if (!mc.inGameHasFocus) return
        toAir(mc.objectMouseOver.blockPos)
    }

    private fun toAir(blockPos: BlockPos?): Boolean {
        if (blockPos != null) {
            val block = mc.theWorld.getBlockState(mc.objectMouseOver.blockPos).block
            if (!blacklist.contains(block) && (block !== Blocks.skull || ghostBlockSkulls.enabled)) {
                mc.theWorld.setBlockToAir(mc.objectMouseOver.blockPos)
                return true
            }
        }
        return false
    }
}