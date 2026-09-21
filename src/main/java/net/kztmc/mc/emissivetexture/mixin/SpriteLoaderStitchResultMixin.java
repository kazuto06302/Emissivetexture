package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveTextureManager;
import net.minecraft.client.texture.SpriteLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteLoader.StitchResult.class)
public abstract class SpriteLoaderStitchResultMixin {
    /** 各アトラスのステッチ結果を捕まえる。モデルのベイクはこの後に走る。 */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void emissivetexture$register(CallbackInfo ci) {
        EmissiveTextureManager.registerAtlas((SpriteLoader.StitchResult) (Object) this);
    }
}