package floppaclient.mixins.render;

import net.minecraft.client.renderer.chunk.VisGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VisGraph.class)
public interface VisGraphAccessor {
    @Accessor int getField_178611_f();
    @Accessor void setField_178611_f(int translucentBlockCount);
}
