package floppaclient.mixins.render;

import floppaclient.module.impl.render.XRay;
import net.minecraft.client.renderer.BlockFluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BlockFluidRenderer.class)
abstract public class BlockFluidRendererMixin {
    @ModifyArg(method = "renderFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;color(FFFF)Lnet/minecraft/client/renderer/WorldRenderer;"), index = 3)
    private float changeAlpha(float oldAlpha) {
        if (XRay.INSTANCE.getEnabled() && XRay.INSTANCE.shouldTweakFluids()) {
            return XRay.INSTANCE.getAlphaFloat();
        }
        return oldAlpha;
    }
}
