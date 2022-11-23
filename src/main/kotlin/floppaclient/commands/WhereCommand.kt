package floppaclient.commands

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.funnymap.features.extras.RoomUtils
import floppaclient.utils.DataHandler
import floppaclient.utils.DataHandler.toCoords
import floppaclient.utils.DataHandler.toIntCoords
import floppaclient.utils.Utils.chatMessage
import floppaclient.utils.Utils.modMessage
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.Vec3
import kotlin.math.floor

/**
 * This command is there to give info about where in skyblock you currently are and which automated actions are defined there.
 *
 * @author Aton
 */
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

            RoomUtils.getOrPutRoomAutoActionData(room.first).run {
                val clips = this.clips
                if (clips.isNotEmpty()) {
                    chatMessage("&r&eClip routes in this room: ")
                    clips.map { (key, route) ->
                        val start = DataHandler.getRotatedCoords(
                            Vec3(key[0].toDouble(),key[1].toDouble(),key[2].toDouble()), room.second)
                            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                        val targets = mutableListOf<Vec3>()
                        for (j in 0 until (route.size) / 3) {
                            targets.add( DataHandler.getRotatedCoords(
                                Vec3(
                                    route[3 * j], route[3 * j + 1], route[3 * j + 2]
                                ), room.second
                            ))
                        }
                        Pair(start, targets)
                    }.sortedBy {
                        mc.thePlayer.getDistance(it.first.xCoord, it.first.yCoord, it.first.zCoord)
                    }.forEach { (start, targets) ->
                        when (targets.size) {
                            0 -> chatMessage(start.toCoords())
                            1 -> chatMessage("${start.toCoords()} ${start.add(targets[0]).toCoords()}")
                            2 -> chatMessage("${start.toCoords()} ${start.add(targets[0]).toCoords()} ${start.add(targets[0]).add(targets[1]).toCoords()}")
                            3 -> chatMessage("${start.toCoords()} ${start.add(targets[0]).toCoords()} ${start.add(targets[0]).add(targets[1]).toCoords()} ${start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()}")
                            4 -> chatMessage("${start.toCoords()} ${start.add(targets[0]).toCoords()} ${start.add(targets[0]).add(targets[1]).toCoords()} ${start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()} ${start.add(targets[0]).add(targets[1]).add(targets[2]).add(targets[3]).toCoords()}")
                            5 -> chatMessage("${start.toCoords()} ${start.add(targets[0]).toCoords()} ${start.add(targets[0]).add(targets[1]).toCoords()} ${start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()} ${start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()} ${start.add(targets[0]).add(targets[1]).add(targets[2]).add(targets[3]).add(targets[4]).toCoords()}")
                        }

                    }
                } else {
                    chatMessage("&r&eNo clip start points found in this room")
                }

                val ethers = this.etherwarps
                if (ethers.isNotEmpty()) {
                    chatMessage("&r&eEtherwarp Pairs in this room: ")
                    ethers.map { (key, value) ->
                        val start = DataHandler.getRotatedCoords(
                            Vec3(key[0].toDouble(),key[1].toDouble(),key[2].toDouble()), room.second)
                            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                        val target = DataHandler.getRotatedCoords(
                            value, room.second)
                            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())

                        Pair(start, target)
                    }.sortedBy {
                        mc.thePlayer.getDistance(it.first.xCoord, it.first.yCoord, it.first.zCoord)
                    }.forEach { (start, target) ->
                        chatMessage("${start.toIntCoords()} ${target.toIntCoords()}")
                    }
                } else {
                    chatMessage("&r&eNo Etherwarps found in this room")
                }
            }
        }catch (e: Throwable) {
            modMessage("§cCould not get data!")
        }
    }
}