package floppaclient.commands

import floppaclient.utils.ChatUtils
import floppaclient.utils.DataHandler
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class AddCmdCommand : CommandBase() {
    override fun getCommandName(): String {
        return "addcmd"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "addc"
        )
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName <cmd>"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        try {
            DataHandler.add(args.map { it.toDouble() })
        } catch (e: Throwable){
            ChatUtils.modMessage("§cArguments error.")
        }
    }
}