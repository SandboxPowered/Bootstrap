package org.sandboxpowered.bootstrap.mixin;

import net.minecraft.client.main.Main;
import org.sandboxpowered.bootstrap.AutoUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Mixin(value = Main.class, priority = 1)
public class MixinClientMain {
    @Inject(method = "main", at = @At("HEAD"))
    private static void main(String[] args, CallbackInfo info) throws IOException, ExecutionException, InterruptedException {
        CompletableFuture<AutoUpdate.Result> future = AutoUpdate.check();

        while (!future.isDone()) {
        }

        AutoUpdate.Result result = future.get();
        if (result == AutoUpdate.Result.UPDATED_TO_LATEST) {
            System.exit(5480);
        }
    }
}
