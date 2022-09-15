package floppaclient.mixins;

import floppaclient.events.ExplosionHandledEvent;
import floppaclient.events.TeleportEventPost;
import floppaclient.events.VelocityUpdateEvent;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {NetHandlerPlayClient.class}, priority = 800)
public abstract class MixinPlayerHandler {
    @Inject(method = {"handleExplosion"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Explosion;doExplosionB(Z)V", shift = At.Shift.AFTER), cancellable = true)
    private void handleExplosionMomentum(S27PacketExplosion packet, CallbackInfo ci) {
        if(MinecraftForge.EVENT_BUS.post(new ExplosionHandledEvent(packet))) ci.cancel();
    }

    @Inject(method = "handleEntityVelocity", at = @At("HEAD"), cancellable = true)
    public void handleEntityVelocity(S12PacketEntityVelocity packet, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new VelocityUpdateEvent(packet))) ci.cancel();
    }

    @Inject(method = "handlePlayerPosLook", at = @At("RETURN"))
    public void handlePlayerPosLook(S08PacketPlayerPosLook packetIn, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new TeleportEventPost(packetIn));
    }
}