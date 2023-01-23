package floppaclient.replacements.render;

import floppaclient.module.impl.render.XRay;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.world.IBlockAccess;

import java.util.BitSet;
import java.util.List;

/**
 * This class is for the most part a copy of parts from net.minecraft.client.renderer.BlockModelRenderer.
 * Unfortunately this is necessary because Optifine and Forge both override a lot of code related to that class.
 * While forges override can be disabled, I don't know how to disable optifines override.
 *
 * It was not feasible to sort all of that out with just mixins so that's why this exists.
 *
 * @author Aton
 * @see net.minecraft.client.renderer.BlockModelRenderer
 * @see floppaclient.mixins.render.BlockRendererDispatcherMixin
 */
public class BlockRenderer {
    private final ThreadLocal<Integer> alphaLocal = ThreadLocal.withInitial(() -> -1);

    public boolean renderModel(IBlockAccess blockAccessIn, IBakedModel modelIn, IBlockState blockStateIn, BlockPos blockPosIn, WorldRenderer worldRendererIn)
    {
        Block block = blockStateIn.getBlock();
        block.setBlockBoundsBasedOnState(blockAccessIn, blockPosIn);
        return this.renderModel(blockAccessIn, modelIn, blockStateIn, blockPosIn, worldRendererIn, true);
    }

    public boolean renderModel(IBlockAccess blockAccessIn, IBakedModel modelIn, IBlockState blockStateIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSides)
    {
        if (XRay.INSTANCE.getEnabled()) {
            alphaLocal.set(XRay.INSTANCE.getBlockAlpha(blockStateIn));
        }
        try
        {
            Block block = blockStateIn.getBlock();
            return this.renderModelStandard(blockAccessIn, modelIn, block, blockPosIn, worldRendererIn, checkSides);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, blockPosIn, blockStateIn);
            crashreportcategory.addCrashSection("Using AO", false);
            throw new ReportedException(crashreport);
        }
    }

    public boolean renderModelStandard(IBlockAccess blockAccessIn, IBakedModel modelIn, Block blockIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSides)
    {
        boolean flag = false;
        BitSet bitset = new BitSet(3);

        for (EnumFacing enumfacing : EnumFacing.values())
        {
            List<BakedQuad> list = modelIn.getFaceQuads(enumfacing);

            if (!list.isEmpty())
            {
                BlockPos blockpos = blockPosIn.offset(enumfacing);

                if (!checkSides || blockIn.shouldSideBeRendered(blockAccessIn, blockpos, enumfacing))
                {
                    int i = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos);
                    this.renderModelStandardQuads(blockAccessIn, blockIn, blockPosIn, i, false, worldRendererIn, list, bitset);
                    flag = true;
                }
            }
        }

        List<BakedQuad> list1 = modelIn.getGeneralQuads();

        if (list1.size() > 0)
        {
            this.renderModelStandardQuads(blockAccessIn, blockIn, blockPosIn, -1, true, worldRendererIn, list1, bitset);
            flag = true;
        }

        return flag;
    }

    private void fillQuadBounds(Block blockIn, int[] vertexData, EnumFacing facingIn, BitSet boundsFlags)
    {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; ++i)
        {
            float f6 = Float.intBitsToFloat(vertexData[i * 7]);
            float f7 = Float.intBitsToFloat(vertexData[i * 7 + 1]);
            float f8 = Float.intBitsToFloat(vertexData[i * 7 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        switch (facingIn)
        {
            case DOWN:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f1 < 1.0E-4F || blockIn.isFullCube()) && f1 == f4);
                break;
            case UP:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f4 > 0.9999F || blockIn.isFullCube()) && f1 == f4);
                break;
            case NORTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f2 < 1.0E-4F || blockIn.isFullCube()) && f2 == f5);
                break;
            case SOUTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f5 > 0.9999F || blockIn.isFullCube()) && f2 == f5);
                break;
            case WEST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f < 1.0E-4F || blockIn.isFullCube()) && f == f3);
                break;
            case EAST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f3 > 0.9999F || blockIn.isFullCube()) && f == f3);
        }
    }

    private void renderModelStandardQuads(IBlockAccess blockAccessIn, Block blockIn, BlockPos blockPosIn, int brightnessIn, boolean ownBrightness, WorldRenderer worldRendererIn, List<BakedQuad> listQuadsIn, BitSet boundsFlags)
    {
        double d0 = blockPosIn.getX();
        double d1 = blockPosIn.getY();
        double d2 = blockPosIn.getZ();
        Block.EnumOffsetType block$enumoffsettype = blockIn.getOffsetType();

        if (block$enumoffsettype != Block.EnumOffsetType.NONE)
        {
            int i = blockPosIn.getX();
            int j = blockPosIn.getZ();
            long k = (i * 3129871L) ^ (long)j * 116129781L;
            k = k * k * 42317861L + k * 11L;
            d0 += ((double)((float)(k >> 16 & 15L) / 15.0F) - 0.5D) * 0.5D;
            d2 += ((double)((float)(k >> 24 & 15L) / 15.0F) - 0.5D) * 0.5D;

            if (block$enumoffsettype == Block.EnumOffsetType.XYZ)
            {
                d1 += ((double)((float)(k >> 20 & 15L) / 15.0F) - 1.0D) * 0.2D;
            }
        }

        for (BakedQuad bakedquad : listQuadsIn)
        {
            if (ownBrightness)
            {
                this.fillQuadBounds(blockIn, bakedquad.getVertexData(), bakedquad.getFace(), boundsFlags);
                brightnessIn = boundsFlags.get(0) ? blockIn.getMixedBrightnessForBlock(blockAccessIn, blockPosIn.offset(bakedquad.getFace())) : blockIn.getMixedBrightnessForBlock(blockAccessIn, blockPosIn);
            }

            worldRendererIn.addVertexData(modifyVertexData(bakedquad.getVertexData()));
            worldRendererIn.putBrightness4(brightnessIn, brightnessIn, brightnessIn, brightnessIn);

            if (bakedquad.hasTintIndex())
            {
                int l = blockIn.colorMultiplier(blockAccessIn, blockPosIn, bakedquad.getTintIndex());

                if (EntityRenderer.anaglyphEnable)
                {
                    l = TextureUtil.anaglyphColor(l);
                }

                float f = (float)(l >> 16 & 255) / 255.0F;
                float f1 = (float)(l >> 8 & 255) / 255.0F;
                float f2 = (float)(l & 255) / 255.0F;
                worldRendererIn.putColorMultiplier(f, f1, f2, 4);
                worldRendererIn.putColorMultiplier(f, f1, f2, 3);
                worldRendererIn.putColorMultiplier(f, f1, f2, 2);
                worldRendererIn.putColorMultiplier(f, f1, f2, 1);
            }

            worldRendererIn.putPosition(d0, d1, d2);
        }
    }

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
