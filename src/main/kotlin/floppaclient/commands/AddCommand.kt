package floppaclient.commands

import floppaclient.utils.DataHandler
import floppaclient.utils.Utils.modMessage
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender


class AddCommand : CommandBase() {
    override fun getCommandName(): String {
        return "addclip"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "addc",
            "adc"
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
            DataHandler.addClip(args.map { it.toDouble() })
        } catch (e: Throwable){
            modMessage("§cArguments error.")
        }
    }
}