package floppaclient.mixins.entity;


import floppaclient.module.impl.misc.QOL;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityPlayer.class})
public abstract class MixinPlayer extends MixinEntityLivingBase {
    @Inject(method = {"isEntityInsideOpaqueBlock"}, at = @At("HEAD"), cancellable = true)
    public void inOpaque(CallbackInfoReturnable<Boolean> cir){
        if (QOL.INSTANCE.cancelHeadInBlock()){
            cir.setReturnValue(false);
        }
    }
}
