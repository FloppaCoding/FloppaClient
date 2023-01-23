package floppaclient.mixins.render;

import floppaclient.module.impl.render.XRay;
import floppaclient.replacements.render.BlockRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ReportedException;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRendererDispatcher.class)
abstract public class BlockRendererDispatcherMixin implements IResourceManagerReloadListener {
    @Shadow @Final private BlockFluidRenderer fluidRenderer;

    @Unique final private BlockRenderer blockRenderer = new BlockRenderer();

    @Shadow public abstract IBakedModel getModelFromBlockState(IBlockState state, IBlockAccess worldIn, BlockPos pos);

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void onRenderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, WorldRenderer worldRendererIn, CallbackInfoReturnable<Boolean> cir) {
        if (XRay.INSTANCE.getEnabled()) {

            try
            {
                int i = state.getBlock().getRenderType();

                if (i == -1)
                {
                    cir.setReturnValue(false);
                }
                else
                {
                    switch (i)
                    {
                        case 1:
                            cir.setReturnValue(this.fluidRenderer.renderFluid(blockAccess, state, pos, worldRendererIn));
                            return;
                        case 2:
                            cir.setReturnValue(false);
                            return;
                        case 3:
                            IBakedModel ibakedmodel = this.getModelFromBlockState(state, blockAccess, pos);

                            cir.setReturnValue( this.blockRenderer.renderModel(blockAccess, ibakedmodel, state, pos, worldRendererIn));
                            return;
                        default:
                            cir.setReturnValue(false);
                    }
                }
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block in world");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being tesselated");
                CrashReportCategory.addBlockInfo(crashreportcategory, pos, state.getBlock(), state.getBlock().getMetaFromState(state));
                throw new ReportedException(crashreport);
            }
        }
    }
}
