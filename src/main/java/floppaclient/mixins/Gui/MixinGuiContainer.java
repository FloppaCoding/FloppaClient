package floppaclient.mixins.Gui;

import floppaclient.events.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {GuiContainer.class}, priority = 800)
public abstract class MixinGuiContainer extends GuiScreenMixin {

    private final GuiContainer gui = (GuiContainer) (Object) this;

    @Shadow
    public Container inventorySlots;

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(Slot slot, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.DrawSlotEvent(inventorySlots, gui, slot)))
            ci.cancel();
    }

    @Inject(method = "handleMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.SlotClickEvent(inventorySlots, gui, slot, slotId)))
            ci.cancel();
    }

    @Inject(method = {"drawScreen"}, at = @At("HEAD"), cancellable = true)
    public void drawContainer(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new DrawContainerEvent(mouseX, mouseY))){
            //At this point the screen will not be drawn
            // The forge event has to be forged so that auto terms still work
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.BackgroundDrawnEvent((GuiScreen) (Object) this));
            ci.cancel();
        }
    }

    @Inject(method = {"drawScreen"}, at = @At("RETURN"))
    public void drawContainerLast(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new DrawContainerLastEvent(mouseX, mouseY));
    }

    @Inject(method = {"mouseClicked"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;mouseClicked(III)V", shift = At.Shift.AFTER), cancellable = true)
    public void guiClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new ContainerMouseClickedEvent(mouseX, mouseY, mouseButton))) ci.cancel();
    }

    @Inject(method = {"checkHotbarKeys"}, at = @At("HEAD"), cancellable = true)
    public void guiTyped(int keyCode, CallbackInfoReturnable<Boolean> cir){
        if (MinecraftForge.EVENT_BUS.post(new ContainerKeyTypedEvent(keyCode))) cir.setReturnValue(true);
    }
}
