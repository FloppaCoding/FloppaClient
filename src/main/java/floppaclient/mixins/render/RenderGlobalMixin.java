package floppaclient.mixins.render;

import floppaclient.module.impl.player.FreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.world.IWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderGlobal.class)
abstract public class RenderGlobalMixin implements IWorldAccess, IResourceManagerReloadListener {
    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
    public Entity tweakViewEntity(Minecraft instance) {
        if (FreeCam.INSTANCE.shouldTweakViewEntity()) {
            return FreeCam.INSTANCE.tweakRenderViewEntityHook();
        }else {
            return instance.getRenderViewEntity();
        }
    }

}
