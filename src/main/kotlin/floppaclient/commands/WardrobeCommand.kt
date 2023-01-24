package floppaclient.commands

import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.utils.ChatUtils
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.inventory.ContainerChest

object WardrobeCommand : CommandBase() {

    private var thread: Thread? = null

    override fun getCommandName(): String {
        return "fwardrobe"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "fwd"
        )
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "/$commandName"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<String>) {
        if (!inSkyblock) return
        if (args.isEmpty()) {
            modMessage("Specify slot")
            return
        }
        val slot = args[0].toInt()
        if (slot !in 1..9) return modMessage("Invalid slot")

        ChatUtils.sendChat("/wardrobe")
        if (thread == null || !thread!!.isAlive) {
            thread = Thread({
                for (i in 0..100) {
                    if (mc.thePlayer.openContainer is ContainerChest && mc.thePlayer.openContainer?.inventorySlots?.get(0)?.inventory?.name?.startsWith("Wardrobe") == true) {
                        mc.playerController.windowClick(
                            mc.thePlayer.openContainer.windowId,
                            35+slot,
                            0,
                            0,
                            mc.thePlayer
                        )
                        mc.thePlayer.closeScreen()
                        return@Thread
                    }
                    Thread.sleep(20)
                }
                modMessage("§aWarobe failed, timed out")
            }, "Auto Wardrobe")
            thread!!.start()
        }
    }
}