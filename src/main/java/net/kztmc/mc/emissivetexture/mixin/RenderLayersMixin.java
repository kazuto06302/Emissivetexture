package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Model#getLayer を通らず、RenderLayers の生成メソッドを直接呼ぶレンダラー（ヴィレジャーの装い、アーマーなど）用。
 * 生成されたレイヤーとテクスチャを記録し、_e があれば同じメソッドで _e 用のレイヤーを作る。
 */
@Mixin(RenderLayers.class)
public abstract class RenderLayersMixin {
    private static final String D1 = "(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;";
    private static final String D2 = "(Lnet/minecraft/util/Identifier;Z)Lnet/minecraft/client/render/RenderLayer;";

    @Inject(method = {
            "armorCutoutNoCull" + D1,
            "armorDecalCutoutNoCull" + D1,
            "armorTranslucent" + D1,
            "entitySolid" + D1,
            "entitySolidZOffsetForward" + D1,
            "entityCutout" + D1,
            "entityCutoutNoCull" + D1,
            "entityCutoutNoCullZOffset" + D1,
            "entityDecal" + D1,
            "entityNoOutline" + D1,
            "entitySmoothCutout" + D1,
            "entityTranslucent" + D1,
            "entityTranslucentEmissive" + D1,
            "entityTranslucentEmissiveNoOutline" + D1,
            "entityAlpha" + D1,
            "itemEntityTranslucentCull" + D1
    }, at = @At("RETURN"))
    private static void emissivetexture$track(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        EmissiveModels.trackLayer(cir.getId(), texture, null, cir.getReturnValue());
    }

    @Inject(method = {
            "entityCutoutNoCull" + D2,
            "entityCutoutNoCullZOffset" + D2,
            "entityTranslucent" + D2,
            "entityTranslucentEmissive" + D2
    }, at = @At("RETURN"))
    private static void emissivetexture$trackWithFlag(Identifier texture, boolean affectsOutline,
                                                      CallbackInfoReturnable<RenderLayer> cir) {
        EmissiveModels.trackLayer(cir.getId(), texture, affectsOutline, cir.getReturnValue());
    }
}