package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveQuads;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(QuadCollection.Builder.class)
public abstract class QuadCollectionBuilderMixin {
    @Inject(method = "build", at = @At("RETURN"), cancellable = true)
    private void essensivetexture$addEmissiveQuads(CallbackInfoReturnable<QuadCollection> cir) {
        QuadCollection original = cir.getReturnValue();
        QuadCollection augmented = EmissiveQuads.augment(original);
        if (augmented != original) {
            cir.setReturnValue(augmented);
        }
    }
}