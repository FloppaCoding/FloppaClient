package floppaclient.commands

import floppaclient.FloppaClient.Companion.mc
import floppaclient.utils.ChatUtils
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class SetYawCommand : CommandBase() {

    override fun getCommandName(): String {
        return "setyaw"
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {

        val values = try {
            args.map { it.toFloat() }
        } catch (e: java.lang.NumberFormatException) {
            ChatUtils.modMessage("§cArguments error.")
            return
        }

        when (values.size) {
            1 -> if (values[0] < 180 && values[0] > -180) mc.thePlayer.rotationYaw = values[0]

            2 -> {
                if (values[0] < 180 && values[0] > -180 && values[1] < 90 && values[1] > -90) {
                    mc.thePlayer.rotationYaw = values[0]
                    mc.thePlayer.rotationPitch = values[1]
                }
            }
        }
    }
}