package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * This module is made to automatically click chests when looking at them in Dungeons (Pairs nicely with Stonk Delay)
 * @author Stivais
 */

object SecretTriggerbot : Module(
    "Secret Triggerbot",
    category = Category.MISC,
    description = "Automatically clicks on a secret when looking at it"
) {


    private val cooldown = NumberSetting("Cooldown", 1000.0, 250.0, 2000.0, 10.0, description = "Cooldown for clicking secrets")
    private val keyHelper = BooleanSetting("Redstone Key", false, description = "Places redstone keys from Inventory")
    private val trappedChests = BooleanSetting("Trapped Chests", true, description = "Also clicks trapped chests.")

    init {
        addSettings(
            cooldown,
            keyHelper,
            trappedChests
        )
    }

    // Change it to only work in Dungeons

    private var nextAction: Long = System.currentTimeMillis()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null) return
        if (System.currentTimeMillis() < nextAction || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return

        val objectPos = mc.objectMouseOver.blockPos
        val state = mc.theWorld.getBlockState(objectPos).block
        val id = (mc.theWorld.getTileEntity(objectPos) as TileEntitySkull).playerProfile?.id?.toString()

        if (state == Blocks.chest || state == Blocks.lever ||
            (state == Blocks.skull && id == "26bb1a8d-7c66-31c6-82d5-a9c04c94fb02" || (id == "edb0155f-379c-395a-9c7d-1b6005987ac8" && keyHelper.enabled) ||
                    state == Blocks.trapped_chest && trappedChests.enabled)
        ) {

            nextAction = System.currentTimeMillis() + cooldown.value.toLong()
            FakeActionUtils.clickBlock(objectPos)

        }
    }
    // Maybe change this to its own module because it doesn't automatically click the block but just places from inventory

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        val oP = mc.objectMouseOver.blockPos

        if (mc.thePlayer == null || !keyHelper.enabled || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
        if (mc.theWorld.getBlockState(oP).block == Blocks.redstone_block && Utils.findItem("Redstone Key", inInv = true) != null
        ) {
            FakeActionUtils.clickBlockWithItem(oP, mc.thePlayer.inventory.currentItem, "Redstone Key", 10.0, fromInv = true, abortIfNotFound = true
            )
        }
    }
}