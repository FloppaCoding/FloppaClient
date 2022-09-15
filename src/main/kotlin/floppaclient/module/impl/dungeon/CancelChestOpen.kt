package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ReceivePacketEvent
import floppaclient.module.Category
import floppaclient.module.Module
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module aimed to insta close secret chests in dungeons.
 *
 * @author Aton
 */
object CancelChestOpen : Module(
    "Cancel Chest Open",
    category = Category.DUNGEON,
    description = "Cancels secret chests from opening."
){
    @SubscribeEvent
    fun onOpenWindow(event: ReceivePacketEvent){
        if (!inDungeons || event.packet !is S2DPacketOpenWindow) return
        if (event.packet.windowTitle.unformattedText.equals("Chest") ) {
            event.isCanceled = true
            mc.netHandler.networkManager.sendPacket( C0DPacketCloseWindow(event.packet.windowId))
        }
    }
}