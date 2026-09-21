package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Model.class)
public abstract class ModelMixin {
    @Inject(method = "getLayer(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;",
            at = @At("RETURN"))
    private void emissivetexture$track(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        EmissiveModels.track((Model<?>) (Object) this, texture, cir.getReturnValue());
    }
}