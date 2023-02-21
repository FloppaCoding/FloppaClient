package floppaclient.mixins;

import floppaclient.module.impl.player.FreeCam;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public abstract class MovementInputFromOptionsMixin extends MovementInput {

    @Inject(method = "updatePlayerMoveState", at = @At("HEAD"), cancellable = true)
    public void supressMovement(CallbackInfo ci) {
        if (FreeCam.INSTANCE.shouldTweakMovement()) {

            this.moveStrafe = 0.0F;
            this.moveForward = 0.0F;

            if (Keyboard.isKeyDown(Keyboard.KEY_UP))
            {
                ++this.moveForward;
            }

            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
            {
                --this.moveForward;
            }

            if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))
            {
                ++this.moveStrafe;
            }

            if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
            {
                --this.moveStrafe;
            }

            ci.cancel();
        }
    }
}
