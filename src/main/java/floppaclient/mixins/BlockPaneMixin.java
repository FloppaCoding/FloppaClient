package floppaclient.mixins;

import floppaclient.module.impl.misc.QOL;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlockPane.class)
abstract class BlockPaneMixin extends BlockMixin{
    @Shadow public abstract boolean canPaneConnectToBlock(Block blockIn);

    @Shadow public abstract boolean canPaneConnectTo(IBlockAccess world, BlockPos pos, EnumFacing dir);

    // This Inject seems to be not required for player collision with the block, but to be safe its here.
    @Inject(method = {"setBlockBoundsBasedOnState"}, at = @At("HEAD"), cancellable = true)
    public void setBounds(IBlockAccess worldIn, BlockPos pos, CallbackInfo ci){
        if (QOL.INSTANCE.modifyPane()) {
            float f =  QOL.minCoord;
            float f1 = QOL.maxCoord;
            float f2 = QOL.minCoord;
            float f3 = QOL.maxCoord;
            boolean flag = this.canPaneConnectToBlock(worldIn.getBlockState(pos.north()).getBlock());
            boolean flag1 = this.canPaneConnectToBlock(worldIn.getBlockState(pos.south()).getBlock());
            boolean flag2 = this.canPaneConnectToBlock(worldIn.getBlockState(pos.west()).getBlock());
            boolean flag3 = this.canPaneConnectToBlock(worldIn.getBlockState(pos.east()).getBlock());

            if ((!flag2 || !flag3) && (flag2 || flag3 || flag || flag1))
            {
                if (flag2)
                {
                    f = 0.0F;
                }
                else if (flag3)
                {
                    f1 = 1.0F;
                }
            }
            else
            {
                f = 0.0F;
                f1 = 1.0F;
            }

            if ((!flag || !flag1) && (flag2 || flag3 || flag || flag1))
            {
                if (flag)
                {
                    f2 = 0.0F;
                }
                else if (flag1)
                {
                    f3 = 1.0F;
                }
            }
            else
            {
                f2 = 0.0F;
                f3 = 1.0F;
            }

            this.setBlockBounds(f, 0.0F, f2, f1, 1.0F, f3);
            ci.cancel();
        }
    }

    @Inject(method = {"addCollisionBoxesToList"}, at = @At("HEAD"), cancellable = true)
    public void addBoundToLIst(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity, CallbackInfo ci) {
        if (QOL.INSTANCE.modifyPane()) {
            float f =  QOL.minCoord;
            float f1 = QOL.maxCoord;
            boolean flag = this.canPaneConnectTo(worldIn, pos, EnumFacing.NORTH);
            boolean flag1 = this.canPaneConnectTo(worldIn, pos, EnumFacing.SOUTH);
            boolean flag2 = this.canPaneConnectTo(worldIn, pos, EnumFacing.WEST);
            boolean flag3 = this.canPaneConnectTo(worldIn, pos, EnumFacing.EAST);

            if ((!flag2 || !flag3) && (flag2 || flag3 || flag || flag1))
            {
                if (flag2)
                {
                    this.setBlockBounds(0.0F, 0.0F, f, 0.5F, 1.0F, f1);
                    super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
                }
                else if (flag3)
                {
                    this.setBlockBounds(0.5F, 0.0F, f, 1.0F, 1.0F, f1);
                    super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
                }
            }
            else
            {
                this.setBlockBounds(0.0F, 0.0F, f, 1.0F, 1.0F, f1);
                super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
            }

            if ((!flag || !flag1) && (flag2 || flag3 || flag || flag1))
            {
                if (flag)
                {
                    this.setBlockBounds(f, 0.0F, 0.0F, f1, 1.0F, 0.5F);
                    super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
                }
                else if (flag1)
                {
                    this.setBlockBounds(f, 0.0F, 0.5F, f1, 1.0F, 1.0F);
                    super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
                }
            }
            else
            {
                this.setBlockBounds(f, 0.0F, 0.0F, f1, 1.0F, 1.0F);
                super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
            }
            ci.cancel();
        }
    }
}
