package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.display
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.LocationManager
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraftforge.event.world.WorldEvent
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
) {
    private val ghostBlockSkulls: Boolean by BooleanSetting("Ghost Skulls", true, description = "If enabled skulls will also be turned into ghost blocks.")
    private val gbRange: Double by NumberSetting("Range", 8.0, 4.5, 60.0, 0.5, description = "Maximum range at which ghost blocks will be created.")
    private val onlyDungeon: Boolean by BooleanSetting("Only In Dungeon", false, description = "Will only work inside of a dungeon.")

    private var warned = false
    private val blacklist = listOf(
        Blocks.stone_button,
        Blocks.chest,
        Blocks.trapped_chest,
        Blocks.lever
    )

    /**
     * Prevent the key bind from toggling the module, so that it can be used here.
     */
    override fun onKeyBind() { }

    override fun onEnable() {
        super.onEnable()
        warned = false
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || display != null || (onlyDungeon && !LocationManager.inDungeons)) return

        if (keyCode == 0) return run { if (!warned) modMessage("Ghost Blocks keybind isn't set."); warned = true }
        if (this.keyCode > 0 && !Keyboard.isKeyDown(this.keyCode)) return
        if (this.keyCode < 0 && !Mouse.isButtonDown(this.keyCode + 100)) return
        if (!mc.inGameHasFocus) return

        val lookingAt = mc.thePlayer?.rayTrace(gbRange, 1f)
        toAir(lookingAt?.blockPos)
    }

    private fun toAir(blockPos: BlockPos?): Boolean {
        if (blockPos != null) {
            val block = mc.theWorld.getBlockState(blockPos).block
            if (!blacklist.contains(block) && (block !== Blocks.skull || (ghostBlockSkulls
                        && (mc.theWorld.getTileEntity(blockPos) as? TileEntitySkull)?.playerProfile?.id?.toString() != "26bb1a8d-7c66-31c6-82d5-a9c04c94fb02"))
            ) {
                mc.theWorld.setBlockToAir(blockPos)
                return true
            }
        }
        return false
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        warned = false
    }
}