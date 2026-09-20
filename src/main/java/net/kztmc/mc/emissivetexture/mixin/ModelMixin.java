package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Model.class)
public abstract class ModelMixin {
    // renderType() と renderType(Identifier) が両方あるので、記述子まで指定する
    @Inject(method = "renderType(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At("RETURN"))
    private void essensivetexture$track(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        EmissiveModels.track((Model<?>) (Object) this, texture, cir.getReturnValue());
    }
}