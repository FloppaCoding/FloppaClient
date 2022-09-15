package floppaclient.mixins.Gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

@Mixin(value = {GuiScreen.class})
public abstract class GuiScreenMixin extends GuiMixin implements GuiYesNoCallback {
    @Shadow
    public Minecraft mc;
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Shadow
    public abstract void drawScreen(int mouseX, int mouseY, float partialTicks);

    @Shadow
    protected abstract void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException;

}
