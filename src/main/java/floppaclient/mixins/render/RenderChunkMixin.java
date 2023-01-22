package floppaclient.mixins.render;

import floppaclient.module.impl.render.XRay;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderChunk.class)
abstract public class RenderChunkMixin {
    @Redirect(method = "rebuildChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/VisGraph;computeVisibility()Lnet/minecraft/client/renderer/chunk/SetVisibility;"))
    private SetVisibility tweakVisibility(VisGraph instance) {
        if (XRay.INSTANCE.getEnabled()) {
            if (((VisGraphAccessor) instance).getField_178611_f() < 4096) {
                ((VisGraphAccessor) instance).setField_178611_f(4090);
            }
        }
        return instance.computeVisibility();
    }
}
