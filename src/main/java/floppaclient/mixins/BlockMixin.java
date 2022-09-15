package floppaclient.mixins;

import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Shadow public abstract void setBlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
