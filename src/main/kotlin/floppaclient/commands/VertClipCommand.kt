package floppaclient.commands

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.impl.player.Clip
import floppaclient.utils.Utils
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos

class VertClipCommand : CommandBase()  {
    override fun getCommandName(): String {
        return "vertclip"
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()){
            val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
            val x = pos.x
            val z = pos.z
            for (y in pos.y downTo 3){
                val newPos = BlockPos(x,y,z)

                if(mc.theWorld.getBlockState(newPos).block.material.isSolid) continue
                if(mc.theWorld.getBlockState(newPos.down()).block.material.isSolid) continue
                for (y2 in y-2 downTo 1) {
                    val newPos2 = BlockPos(x, y2, z)
                    if(mc.theWorld.getBlockState(newPos2).block.material.isSolid) {
                        Clip.teleport(x+0.5,y - 1.0, z + 0.5)
                        return
                    }
                }
                break
            }
        }
        if (args[0] == "ground"){
            val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
            val x = pos.x
            val z = pos.z
            for (y in pos.y downTo 3){
                val newPos = BlockPos(x,y,z)

                if(mc.theWorld.getBlockState(newPos).block.material.isSolid) continue
                if(mc.theWorld.getBlockState(newPos.down()).block.material.isSolid) continue
                for (y2 in y-2 downTo 1) {
                    val newPos2 = BlockPos(x, y2, z)
                    if(mc.theWorld.getBlockState(newPos2).block.material.isSolid) {
                        mc.thePlayer.motionY = 0.0
                        Clip.teleport(x+0.5,y2 + 1.05, z + 0.5)
                        return
                    }
                }
                break
            }
        }
        else if(args[0] == "floor"){
            val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
            val x = pos.x
            val z = pos.z
            for (y in pos.y downTo 1){
                val newPos = BlockPos(x,y,z)

                if(mc.theWorld.getBlockState(newPos).block.material.isSolid) {
                    mc.thePlayer.motionY = 0.0
                    Clip.teleport(mc.thePlayer.posX,y + 1.05, mc.thePlayer.posZ)
                    return
                }
            }
        }
        else {
            val y = try {
                args[0].toDouble()
            } catch (e: java.lang.NumberFormatException) {
                Utils.modMessage("§cArguments error.")
                return
            }
            Clip.clip(0.0, y, 0.0)
        }
    }
}