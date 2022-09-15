package floppaclient.mixins;

import floppaclient.module.impl.misc.QOL;
import net.minecraft.block.BlockCarpet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockCarpet.class)
public abstract class CarpetMixin extends BlockMixin {
    @Inject(method = {"setBlockBoundsFromMeta"}, at = @At("HEAD"), cancellable = true)
    public void setBounds(int meta, CallbackInfo ci){
        if (QOL.INSTANCE.ignoreCarpet()) {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.0f, 1.0F);
            ci.cancel();
        }
    }
}
