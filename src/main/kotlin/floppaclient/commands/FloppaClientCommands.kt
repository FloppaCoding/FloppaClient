package floppaclient.commands

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.clickGUI
import floppaclient.FloppaClient.Companion.display
import floppaclient.FloppaClient.Companion.extras
import floppaclient.FloppaClient.Companion.mc
import floppaclient.funnymap.core.RoomData
import floppaclient.funnymap.features.dungeon.DungeonScan
import floppaclient.module.impl.dungeon.AutoBlaze
import floppaclient.module.impl.dungeon.AutoWater
import floppaclient.module.impl.render.ClickGui
import floppaclient.utils.Utils.chatMessage
import floppaclient.utils.Utils.modMessage
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.BlockPos


class FloppaClientCommands : CommandBase() {
    override fun getCommandName(): String {
        return "floppaclient"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "fclient",
            "fcl",
            "fc"
        )
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            display = clickGUI
            return
        }
        when (args[0].lowercase()) {
            "gui" -> display = clickGUI
            "scan" -> DungeonScan.scanDungeon()
            "roomdata" -> DungeonScan.getRoomCentre(mc.thePlayer.posX.toInt(), mc.thePlayer.posZ.toInt()).let {
                DungeonScan.getRoomData(it.first, it.second) ?: DungeonScan.getCore(it.first, it.second)
            }.run {
                GuiScreen.setClipboardString(this.toString())
                modMessage(
                    if (this is RoomData) "Copied room data to clipboard."
                    else "Existing room data not found. Copied room core to clipboard."
                )
            }
            "stop" -> {
                AutoWater.stop()
                AutoBlaze.stop()
            }
            "reload" -> {
                modMessage("Reloading config files.")
                extras.loadConfig()
                FloppaClient.moduleConfig.loadConfig()
                clickGUI.setUpPanels()
            }
            "resetgui" -> {
                modMessage("Resetting positions in the click gui.")
                ClickGui.resetPositions()
            }
            "clickentity" -> {
                FakeActionUtils.interactWithEntity(args[1].toInt())
            }
            "armorstands" -> {
                mc.theWorld.loadedEntityList
                    .filter { entity ->
                        entity is EntityArmorStand
                    }
                    .sortedBy {
                        -mc.thePlayer.getDistanceToEntity(it)
                    }
                    .forEach { entity ->
                        chatMessage("Name: " + entity.name +", Custom Name: " + entity.customNameTag + ", Id: " + entity.entityId)
                    }
            }
            "entities" -> {
                mc.theWorld.loadedEntityList
                    .sortedBy {
                        -mc.thePlayer.getDistanceToEntity(it)
                    }
                    .forEach { entity ->
                        chatMessage("Type: " + entity::class.simpleName +
                                ", Name: " + entity.name +", Custom Name: " + entity.customNameTag + ", Id: " + entity.entityId)
                    }
            }
            "clickblock" -> {
                if (args.size < 4)
                    return modMessage("Not enough arguments.")
                val blockPos = BlockPos(args[1].toDouble(), args[2].toDouble(), args[3].toDouble())
                FakeActionUtils.clickBlock(blockPos)
            }
            "core" -> {
                if (args.size < 3)
                    return modMessage("Not enough arguments.")
                modMessage(DungeonScan.getCore(args[1].toInt(), args[2].toInt()).toString())
            }
            else -> {
                modMessage("Command not recognized!")
            }
        }
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
        pos: BlockPos
    ): MutableList<String> {
        if (args.size == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                mutableListOf("scan", "roomdata", "reload" , "clickentity" , "armorstands" , "entities")
            )
        }
        return mutableListOf()
    }
}
