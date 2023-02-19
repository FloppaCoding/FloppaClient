package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.inventory.ItemUtils.hasAbility
import floppaclient.utils.inventory.InventoryUtils.isHolding
import net.minecraft.block.Block
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos

/**
 * Cancels block interactions to allow for items to be used.
 *
 * @author Aton
 */
object CancelInteract : Module(
    "Cancel Interact",
    category = Category.MISC,
    description = "Cancels the interaction with certain blocks, so that the item can be used instead. " +
            "The following rules will be followed in that priority: \n" +
            "Will never cancel interaction with chests, levers, buttons.\n" +
            "Will always cancel interactions with pearls. \n" +
            "Will cancel interaction with blacklisted blocks. This can be limited to only take place when holding an " +
            "item with ability."
){
    private val onlyInDungen = BooleanSetting("Only In Dungeon", true, description = "Only enable this module in dungeons.")
    private val onlyWithAbility = BooleanSetting("Only Ability", false, description = "Check whether the item has an ability before cancelling interactions.")

    /**
     * Block which should always be interacted with.
     */
    private val interactionWhielist = setOf<Block>(
        Blocks.lever,
        Blocks.chest,
        Blocks.trapped_chest,
        Blocks.stone_button,
        Blocks.wooden_button
    )

    /**
     * Set containing all the block which interactions should be cancelled with.
     */
    private val interactionBlakclist = setOf<Block>(
        //Fences and cobble wall
        Blocks.cobblestone_wall,
        Blocks.oak_fence,
        Blocks.dark_oak_fence,
        Blocks.acacia_fence,
        Blocks.birch_fence,
        Blocks.jungle_fence,
        Blocks.nether_brick_fence,
        Blocks.spruce_fence,
        Blocks.birch_fence_gate,
        Blocks.acacia_fence_gate,
        Blocks.dark_oak_fence_gate,
        Blocks.oak_fence_gate,
        Blocks.jungle_fence_gate,
        Blocks.spruce_fence_gate,
        // Hopper
        Blocks.hopper,
    )

    init {
        this.addSettings(
            onlyInDungen,
            onlyWithAbility
        )
    }

    /**
     * Redirected to by the MinecraftMixin. Replaces the check for whether the targeted block is air.
     * When true is retured the item ability will be used, with false an interactionm with the block will be performed.
     */
    fun shouldPriotizeAbilityHook(instance: WorldClient, blockPos: BlockPos): Boolean {
        // When the module is not enabled preform the vanilla action.
        if (this.enabled && (inDungeons || !onlyInDungen.enabled)) {
            if (interactionWhielist.contains(instance.getBlockState(blockPos).block)) return false
            if (mc.thePlayer.isHolding("Ender Pearl")) return true
            if (!onlyWithAbility.enabled || mc.thePlayer.heldItem.hasAbility )
                return interactionBlakclist.contains(instance.getBlockState(blockPos).block) || instance.isAirBlock(
                    blockPos
                )
        }
        return instance.isAirBlock(blockPos)
    }
}