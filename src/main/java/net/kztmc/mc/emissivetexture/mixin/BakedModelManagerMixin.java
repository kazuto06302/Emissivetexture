package net.kztmc.mc.emissivetexture.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.kztmc.mc.emissivetexture.emissive.EmissiveTextureManager;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.resource.ResourceReloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BakedModelManager.class)
public abstract class BakedModelManagerMixin {
    /** リロード開始：現在有効な ResourceManager から _e を走査し直す（F3+T のたびに再構築される）。 */
    @Inject(method = "reload", at = @At("HEAD"))
    private void emissivetexture$beginReload(ResourceReloader.Store store,
                                             Executor prepareExecutor,
                                             ResourceReloader.Synchronizer synchronizer,
                                             Executor applyExecutor,
                                             CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        EmissiveTextureManager.beginReload(store.getResourceManager());
    }
}