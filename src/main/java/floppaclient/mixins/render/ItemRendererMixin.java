package floppaclient.mixins.render;

import floppaclient.module.impl.misc.QOL;
import floppaclient.module.impl.render.ItemAnimations;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ItemRenderer.class})
public class ItemRendererMixin {

    @Shadow private ItemStack itemToRender;

    @Inject(method = {"transformFirstPersonItem(FF)V"}, at = @At("HEAD"), cancellable = true)
    public void itemTransform(float equipProgress, float swingProgress, CallbackInfo ci) {
        if (ItemAnimations.INSTANCE.itemTransforHook(equipProgress, swingProgress)) ci.cancel();
    }

    @Inject(method = {"doItemUsedTransformations"}, at = @At("HEAD"), cancellable = true)
    public void useTransform(float swingProgress, CallbackInfo ci){
        if (ItemAnimations.INSTANCE.scaledSwing(swingProgress)) ci.cancel();
    }

    @Inject(method = {"performDrinking"}, at = @At("HEAD"), cancellable = true)
    public void drinkTransform(AbstractClientPlayer clientPlayer, float partialTicks, CallbackInfo ci){
        if (ItemAnimations.INSTANCE.scaledDrinking(clientPlayer, partialTicks, itemToRender)) ci.cancel();
    }

    @Redirect(method = {"renderOverlays"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isBurning()Z"))
    public boolean shouldRenderFireOverlay(EntityPlayerSP instance){
        return QOL.INSTANCE.shouldDisplayBurnOverlayHook();
    }
}
