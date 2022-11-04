package floppaclient.mixins;

import floppaclient.events.BlockDestroyEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
abstract class PlayerControllerMixin {

    @Shadow @Final private Minecraft mc;

    @Inject(method = {"onPlayerDestroyBlock"}, at = @At("HEAD"))
    public void onBlockDestroy(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir){
        IBlockState state = this.mc.theWorld.getBlockState(pos);
        MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(pos, side, state));
    }
}
