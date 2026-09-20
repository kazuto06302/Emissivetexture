package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow
    @Final
    private SpriteGetter sprites;

    /** BlockEntityやエンティティが使う SpriteGetter を控える（_e スプライトの解決に使う）。 */
    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void essensivetexture$captureSprites(ResourceManager resourceManager, CallbackInfo ci) {
        EmissiveModels.setSpriteGetter(this.sprites);
    }
}