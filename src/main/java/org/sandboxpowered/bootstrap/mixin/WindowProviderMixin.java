package org.sandboxpowered.bootstrap.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.WindowProvider;
import org.sandboxpowered.bootstrap.AutoUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(WindowProvider.class)
public class WindowProviderMixin {

    @Inject(method = "createWindow", at = @At("HEAD"))
    private void onCreateWindow(WindowSettings windowSettings, String string, String string2, CallbackInfoReturnable<Window> cir) {
        AutoUpdate.closeClientWindow();
    }
}
