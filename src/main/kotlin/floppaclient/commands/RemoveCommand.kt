package floppaclient.commands

import floppaclient.utils.DataHandler
import floppaclient.utils.Utils
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender


class RemoveCommand : CommandBase() {
    override fun getCommandName(): String {
        return "removeclip"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "remc",
            "rmc"
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
            DataHandler.removeClip(args.map { it.toDouble() })
        } catch (e: Throwable){
            Utils.modMessage("&cArguments error.")
        }
    }
}