package floppaclient.commands

import floppaclient.utils.ChatUtils
import floppaclient.utils.ClipTools
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class HClipCommand : CommandBase()  {
    override fun getCommandName(): String {
        return "hclip"
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val values = try {
            args.map { it.toDouble() }
        } catch (e: java.lang.NumberFormatException) {
            ChatUtils.modMessage("§cArguments error.")
            return
        }
        when(values.size) {
            1 -> {
                ClipTools.hClip(values[0])
            }
            2 -> {
                ClipTools.hClip(values[0], yOffs = values[1])
            }
        }
    }
}