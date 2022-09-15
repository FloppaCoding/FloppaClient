package floppaclient.mixins.Gui;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = {Gui.class})
public abstract class GuiMixin {
    @Shadow
    public static void drawRect(int left, int top, int right, int bottom, int color) {}
}
