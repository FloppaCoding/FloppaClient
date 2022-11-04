package floppaclient.mixins.render;

import floppaclient.module.impl.render.ChestEsp;
import floppaclient.utils.RenderObject;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityChestRenderer.class)
abstract class ChestRendererMixin {

    @Inject(method = {"renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDFI)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableDepth()V", shift = At.Shift.AFTER))
    public void onRender(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci){
        if (ChestEsp.INSTANCE.isPhaseMode())
            GlStateManager.disableDepth();
    }

    @Inject(method = {"renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDFI)V"}, at = @At("RETURN"))
    public void onRenderReturn(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci){
        if (ChestEsp.INSTANCE.isBoxMode())
            RenderObject.INSTANCE.drawBoxAtBlock(x, y, z, ChestEsp.INSTANCE.getBoxColor(), ChestEsp.INSTANCE.getBoxThickness(), false);
        if (ChestEsp.INSTANCE.isPhaseMode())
            GlStateManager.enableDepth();
    }
}
