package floppaclient.commands

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.utils.DataHandler
import floppaclient.utils.Utils.chatMessage
import floppaclient.utils.Utils.modMessage
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.Vec3
import kotlin.math.floor

class WhereCommand : CommandBase() {
    override fun getCommandName(): String {
        return "where"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        try {
            if (!FloppaClient.inDungeons) modMessage("§cNot in Dungeon!")
            val room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("§cRoom not recognized!")
            val pos = Vec3(floor(mc.thePlayer.posX), floor(mc.thePlayer.posY), floor(mc.thePlayer.posZ))
            val key = DataHandler.getKey(
                pos,
                room.first.x,
                room.first.z,
                room.second
            )
            modMessage("Room Information")
            chatMessage("§r&eCurrent room: §r" + room.first.data.name)
            chatMessage("§r&eRoom coordinates: §r" + room.first.x + ", " + room.first.z)
            chatMessage("§r&eRoom rotation: §r" + room.second)
            chatMessage("§r&eRelative Player coordinates: §r" + key.joinToString())
        }catch (e: Throwable) {
            modMessage("§cCould not get data!")
        }
    }
}