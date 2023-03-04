package floppaclient.commands

import floppaclient.utils.ChatUtils
import floppaclient.utils.DataHandler
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class RemoveCmdCommand : CommandBase() {
    override fun getCommandName(): String {
        return "removecmd"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "remcmd"
        )
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        try {
            DataHandler.removeCmd(args.map { it.toDouble() })
        } catch (e: Throwable){
            ChatUtils.modMessage("&cArguments error.")
        }
    }
}