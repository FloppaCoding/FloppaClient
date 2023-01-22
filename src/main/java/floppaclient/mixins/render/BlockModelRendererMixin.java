package floppaclient.mixins.render;

import floppaclient.module.impl.render.XRay;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockModelRenderer.class)
abstract public class BlockModelRendererMixin {
    @Unique
    private final ThreadLocal<Integer> alphaLocal = ThreadLocal.withInitial(() -> -1);

    @Inject(method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/WorldRenderer;Z)Z", at = @At("HEAD"))
    private void onRenderModel(IBlockAccess blockAccessIn, IBakedModel modelIn, IBlockState blockStateIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSides, CallbackInfoReturnable<Boolean> cir) {
        if (XRay.INSTANCE.getEnabled()) {
            alphaLocal.set(XRay.INSTANCE.getBlockAlpha(blockStateIn));
        }
    }

    @ModifyArg(method = "renderModelStandardQuads", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;addVertexData([I)V"))
    private int[] modifyVertexData(int[] vertexData) {
        if (XRay.INSTANCE.getEnabled()) {
            int alpha = alphaLocal.get();
            if (alpha != -1 && vertexData.length >= 28) {
                int[] newData = vertexData.clone();
                for (int ii = 0; ii < 4; ii++) {
                    newData[ii * 7 + 3] = (vertexData[ii * 7 + 3] & 0x00FFFFFF) | (alpha << 24);
                }
                return newData;
            }
        }
        return vertexData;
    }
}

