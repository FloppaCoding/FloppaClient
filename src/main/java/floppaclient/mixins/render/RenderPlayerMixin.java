package floppaclient.mixins.render;

import floppaclient.FloppaClient;
import floppaclient.module.impl.player.FreeCam2;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderPlayer.class)
abstract public class RenderPlayerMixin {

    @Redirect(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/RenderManager;livingPlayer:Lnet/minecraft/entity/Entity;"))
    public Entity tweakViewEntity(RenderManager instance) {
        if (FreeCam2.INSTANCE.isFreecamActive()) {
            return FloppaClient.mc.thePlayer;
        }else {
            return instance.livingPlayer;
        }
    }
}
