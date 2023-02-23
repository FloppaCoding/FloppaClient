package floppaclient.mixins.render;

import floppaclient.module.impl.render.ChestEsp;
import floppaclient.utils.render.WorldRenderUtils;
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
        if (ChestEsp.INSTANCE.isPhaseMode() && ChestEsp.INSTANCE.isDrawingWorld()){
            // Polygon offset is a pretty scuffed way of doing this, since it is meant only for small offsets,
            // but it works and is really simple. Stencil would probably be the proper way.
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(0,-7_000_000);
        }
    }

    @Inject(method = {"renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDFI)V"}, at = @At("RETURN"))
    public void onRenderReturn(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (ChestEsp.INSTANCE.isBoxMode() && ChestEsp.INSTANCE.isDrawingWorld())
            WorldRenderUtils.INSTANCE.drawBoxAtBlock(x, y, z, ChestEsp.INSTANCE.getBoxColor(), false, false, ChestEsp.INSTANCE.getBoxThickness(), 1f);
        if (ChestEsp.INSTANCE.isPhaseMode() && ChestEsp.INSTANCE.isDrawingWorld()) {
            GlStateManager.disablePolygonOffset();
        }
    }
}
