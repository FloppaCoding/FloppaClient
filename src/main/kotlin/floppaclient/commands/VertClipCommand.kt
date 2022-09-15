package floppaclient.commands

import floppaclient.module.impl.player.Clip
import floppaclient.utils.Utils
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class VertClipCommand : CommandBase()  {
    override fun getCommandName(): String {
        return "vertclip"
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val y = try {
            args[0].toDouble()
        } catch (e: java.lang.NumberFormatException) {
            Utils.modMessage("§cArguments error.")
            return
        }
        Clip.clip(0.0, y, 0.0)
    }
}