package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveQuads;
import net.minecraft.client.render.model.BakedGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BakedGeometry.Builder.class)
public abstract class BakedGeometryBuilderMixin {
    @Inject(method = "build", at = @At("RETURN"), cancellable = true)
    private void emissivetexture$addEmissiveQuads(CallbackInfoReturnable<BakedGeometry> cir) {
        BakedGeometry original = cir.getReturnValue();
        BakedGeometry augmented = EmissiveQuads.augment(original);
        if (augmented != original) {
            cir.setReturnValue(augmented);
        }
    }
}