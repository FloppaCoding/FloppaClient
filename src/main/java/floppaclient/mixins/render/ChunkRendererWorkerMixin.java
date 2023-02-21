package floppaclient.mixins.render;

import floppaclient.module.impl.player.FreeCam2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkRenderWorker.class)
abstract public class ChunkRendererWorkerMixin implements Runnable {
    @Redirect(method = "processTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
    public Entity tweakRenderViewEntity(Minecraft instance) {
        if (FreeCam2.INSTANCE.isFreecamActive()) {
            return FreeCam2.INSTANCE.tweakRenderViewEntityHook();
        }else {
            return instance.getRenderViewEntity();
        }
    }
}
