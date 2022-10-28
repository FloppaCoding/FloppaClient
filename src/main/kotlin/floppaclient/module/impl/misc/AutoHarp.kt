package floppaclient.module.impl.misc

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Blocks
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

/**
 * Module to automatically do the Melody's Harp minigame.
 *
 * @author Aton
 */
object AutoHarp : Module(
    "Auto Harp",
    category = Category.MISC,
    description = "Automatically Completes Melody's Harp"
){
    private var inHarp = false
    private var lastInv = 0

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiChest || !FloppaClient.inSkyblock) return
        val container = (event.gui as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val chestName = container.lowerChestInventory.displayName.unformattedText
            if (chestName.startsWith("Harp -")) {
                inHarp = true
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (!inHarp || event.phase != TickEvent.Phase.END || mc.thePlayer == null) return
        val container = mc.thePlayer.openContainer ?: return
        if (container !is ContainerChest) return
        val inventoryName = container.inventorySlots?.get(0)?.inventory?.name
        if (inventoryName == null || !inventoryName.startsWith("Harp -")) {
            inHarp = false
            return
        }
        val newHash = container.inventorySlots.subList(0,36).joinToString("") { it?.stack?.displayName ?: "" }.hashCode()
        if (lastInv == newHash) return
        lastInv = newHash
        for (ii in 0..6) {
            val slot = container.inventorySlots[37 + ii]
            if ((slot.stack?.item as? ItemBlock)?.block === Blocks.quartz_block) {
                mc.playerController.windowClick(
                    container.windowId,
                    slot.slotNumber,
                    2,
                    3,
                    mc.thePlayer
                )
                break
            }
        }
    }
}