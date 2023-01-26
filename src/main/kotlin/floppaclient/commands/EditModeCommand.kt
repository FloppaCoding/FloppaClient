package floppaclient.commands

import floppaclient.FloppaClient
import floppaclient.floppamap.extras.EditMode
import floppaclient.module.impl.render.ExtraBlocks
import floppaclient.utils.ChatUtils.modMessage
import net.minecraft.block.Block
import net.minecraft.block.BlockStainedGlass
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.command.NumberInvalidException
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.BlockPos

class EditModeCommand : CommandBase() {

    /**
     * Maps a shortcut name to a BlockstateID.
     * This map is used for colored glass.
     * This map is only for static shortcuts.
     * Dynamic shortcuts (default stair type, etc.) will be handled else where.
     */
    private val shortcuts = mapOf<String, Int>(
        "glass" to Block.getIdFromBlock(Blocks.glass),
        "wall"  to Block.getIdFromBlock(Blocks.cobblestone_wall),
        "bars"  to Block.getIdFromBlock((Blocks.iron_bars)),
        "brick"  to Block.getIdFromBlock((Blocks.brick_block)),

        //Stained Glass
        "wg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.WHITE)),
        "og"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.ORANGE)),
        "mg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.MAGENTA)),
        "lbg"   to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.LIGHT_BLUE)),
        "yg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.YELLOW)),
        "lg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.LIME)),
        "pig"   to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.PINK)),
        "sg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.GRAY)),
        "lsg"   to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.SILVER)),
        "cg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.CYAN)),
        "pug"   to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.PURPLE)),
        "bg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.BLUE)),
        "brg"   to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.BROWN)),
        "gg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.GREEN)),
        "rg"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.RED)),
        "ng"    to Block.getStateId(Blocks.stained_glass.defaultState.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.BLACK)),

        //Ore Blocks
        "gb"    to Block.getIdFromBlock((Blocks.gold_block)),
        "db"    to Block.getIdFromBlock((Blocks.diamond_block)),
        "ib"    to Block.getIdFromBlock((Blocks.iron_block)),
        "cb"    to Block.getIdFromBlock((Blocks.coal_block)),
        "eb"    to Block.getIdFromBlock((Blocks.emerald_block)),
    )




    private val fences = listOf(
        Blocks.oak_fence,
        Blocks.dark_oak_fence,
        Blocks.acacia_fence,
        Blocks.birch_fence,
        Blocks.jungle_fence,
        Blocks.nether_brick_fence,
        Blocks.spruce_fence
    )

    private val stairs = listOf(
        Blocks.birch_stairs,
        Blocks.acacia_stairs,
        Blocks.stone_brick_stairs,
        Blocks.brick_stairs,
        Blocks.sandstone_stairs,
        Blocks.dark_oak_stairs,
        Blocks.nether_brick_stairs,
        Blocks.jungle_stairs,
        Blocks.oak_stairs,
        Blocks.quartz_stairs,
        Blocks.red_sandstone_stairs,
        Blocks.stone_stairs,
        Blocks.spruce_stairs
    )

    private val slabs = listOf(
        Blocks.stone_slab,
        Blocks.wooden_slab
    )

    override fun getCommandName(): String {
        return "editmode"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "em"
        )
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        // when no extra arguments are specified toggle edit mode
        if (args.isEmpty() || !EditMode.enabled) {
            if (ExtraBlocks.enabled) {
                EditMode.enabled = !EditMode.enabled
                modMessage("Edit Mode ${if (EditMode.enabled) "enabled" else "disabled"}!")
                FloppaClient.extras.saveConfig()
                FloppaClient.extras.loadConfig()
            } else {
                modMessage("Enable extras before using edit mode!")
                return
            }
            if (args.isEmpty())return
        }
        // First check for matches in the static shortcuts
        var id = shortcuts[args[0].lowercase()]
        if (id != null) {
            EditMode.currentBlockID = id
            modMessage("Set block to: ${Block.getStateById(id).block.localizedName}")
            return
        }

        // now check the customizable shortcuts
        id = when (args[0].lowercase()){
            "stair", "stairs" -> Block.getIdFromBlock(stairs[ExtraBlocks.defaultStairs.index])
            "fence" -> Block.getIdFromBlock(fences[ExtraBlocks.defaultFence.index])
            "slab" -> Block.getIdFromBlock(slabs[ExtraBlocks.defaultSlab.index])
            else -> null
        }
        if (id != null) {
            EditMode.currentBlockID = id
            modMessage("Set block to: ${Block.getStateById(id).block.localizedName}")
            return
        }

        // run environment so that return out of it is possible
        kotlin.run getByID@{
            val data = args[0].split(":")
            val blockID = data.getOrNull(0)?.toIntOrNull() ?: return@getByID
            val metadata = data.getOrNull(1)?.toIntOrNull() ?: 0
            val state = Block.getBlockById(blockID).getStateFromMeta(metadata)
            EditMode.currentBlockID = Block.getStateId(state)
            modMessage("Set block to: ${state.block.localizedName}")
            return
        }


        // last check block list for the block and if it is not in there check the block id
        try {
            val block = getBlockByText(sender, args[0])
            EditMode.currentBlockID = Block.getIdFromBlock(block)
            modMessage("Set block to: ${block.localizedName}")
        } catch (e: NumberInvalidException) {
            modMessage("Invalid block name.")
        }
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
        pos: BlockPos
    ): MutableList<String> {
        if (args.size == 1) {
            val matches = mutableListOf(
                "stairs",
                "slab",
                "fence"
            )
            matches.addAll(shortcuts.keys)
            matches.addAll(Block.blockRegistry.keys.map { it.toString().drop(10) })
            return getListOfStringsMatchingLastWord(
                args,
                matches
            )
        }
        return mutableListOf()
    }
}