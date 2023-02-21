package floppaclient.mixins;

import floppaclient.events.BlockDestroyEvent;
import floppaclient.module.impl.misc.FastMine;
import floppaclient.utils.inventory.ItemUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
abstract class PlayerControllerMixin {

    @Shadow @Final private Minecraft mc;

    @Shadow private float curBlockDamageMP;

    @Shadow private float stepSoundTickCounter;

    @Shadow private int blockHitDelay;

    @Inject(method = {"onPlayerDestroyBlock"}, at = @At("HEAD"))
    public void onBlockDestroy(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir){
        IBlockState state = this.mc.theWorld.getBlockState(pos);
        MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(pos, side, state));
    }

    @Redirect(method = "onPlayerDamageBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getPlayerRelativeBlockHardness(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;)F"))
    private float tweakBlockDamage(Block instance, EntityPlayer playerIn, World worldIn, BlockPos posBlock){

        Block block = this.mc.theWorld.getBlockState(posBlock).getBlock();

        float relHardness = block.getPlayerRelativeBlockHardness(playerIn, worldIn, posBlock);
        if (FastMine.INSTANCE.shouldTweakVanillaMining()){
            float threshold = FastMine.INSTANCE.getThreshold();
            if (this.curBlockDamageMP + relHardness >= threshold) {
                return relHardness + 1f - threshold;
            }
        }
        return relHardness;
    }

    @Inject(method = "onPlayerDamageBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;sendBlockBreakProgress(ILnet/minecraft/util/BlockPos;I)V"))
    private void preBreakBlock(BlockPos posBlock, EnumFacing directionFacing, CallbackInfoReturnable<Boolean> cir){
        if (FastMine.INSTANCE.shouldPreBreakBlock(this.stepSoundTickCounter, this.curBlockDamageMP)) {
            this.mc.theWorld.setBlockToAir(posBlock); // Maybe instead use: this.onPlayerDestroyBlock(posBlock, directionFacing);
            // The following is probably not required.
            this.curBlockDamageMP = 0.0F;
            this.stepSoundTickCounter = 0f;

            this.blockHitDelay = 5;
        }
    }

    @Inject(method = "onPlayerDamageBlock", at = @At("HEAD"))
    private void tweakHitDelay(BlockPos posBlock, EnumFacing directionFacing, CallbackInfoReturnable<Boolean> cir) {
        if (FastMine.INSTANCE.shouldRemoveHitDelay(this.blockHitDelay)) {
            this.blockHitDelay = 0;
        }
    }

    @Redirect(method = "isHittingPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areItemStackTagsEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean shouldTagsBeEqual(ItemStack stackA, ItemStack stackB) {
        if (FastMine.INSTANCE.shouldPreventReset()) {
            return ItemUtils.INSTANCE.getItemID(stackA).equals(ItemUtils.INSTANCE.getItemID(stackB));
        }
        return ItemStack.areItemStackTagsEqual(stackA, stackB);
    }
}
