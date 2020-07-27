package org.sandboxpowered.bootstrap.mixin;

import net.minecraft.server.Main;
import org.sandboxpowered.bootstrap.AutoUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MixinServerMain {
    @Inject(method = "main", at = @At("HEAD"))
    private static void main(String[] args, CallbackInfo callbackInfo) {
        AutoUpdate.updateServer();
    }
}
