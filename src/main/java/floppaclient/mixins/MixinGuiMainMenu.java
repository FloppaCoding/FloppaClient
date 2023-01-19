package floppaclient.mixins;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Calendar;
import java.util.Date;

@Mixin(value = {GuiMainMenu.class}, priority = 1001)
public abstract class MixinGuiMainMenu extends GuiScreen implements GuiYesNoCallback {
    @Shadow
    private String splashText;

    @Inject(method = {"<init>"}, at = {@At("RETURN")})
    public void initMainMenu(CallbackInfo callbackInfo) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        if (day == 6) {
            this.splashText = "Floppa Friday!";
        }else {
            this.splashText = "MILF (Man I love Floppa)";
        }
    }
}
