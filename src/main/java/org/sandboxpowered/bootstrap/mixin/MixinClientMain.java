package org.sandboxpowered.bootstrap.mixin;

import net.minecraft.client.main.Main;
import org.sandboxpowered.bootstrap.AutoUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Main.class, priority = 1)
public class MixinClientMain {
    @Inject(method = "main", at = @At("HEAD"))
    private static void main(String[] args, CallbackInfo info) {
        AutoUpdate.doStuff();
    }
}
