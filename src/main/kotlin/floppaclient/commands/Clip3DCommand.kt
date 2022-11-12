package floppaclient.commands

import floppaclient.utils.ClipTools
import floppaclient.utils.GeometryUtils
import floppaclient.utils.Utils
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class Clip3DCommand : CommandBase()  {
    override fun getCommandName(): String {
        return "clip3d"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "3dclip",
            "dclip"
        )
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
        ClipTools.dClip(y, GeometryUtils.yaw(), GeometryUtils.pitch())
    }
}