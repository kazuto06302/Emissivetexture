package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Model#renderType を通らず、RenderTypes の生成メソッドを直接呼ぶレンダラー（ヴィレジャーの装い、アーマーなど）用。
 * 生成されたレイヤーとテクスチャを記録し、_e があれば同じメソッドで _e 用のレイヤーを作る。
 * 対象は Util.memoize されたもの（同じテクスチャなら同じレイヤーが返る）だけ。
 * createArmorDecalCutoutNoCull は呼ぶたびに新しいレイヤーを作るので含めない（記録が増え続けるため）。
 */
@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {
    private static final String D1 = "(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;";
    private static final String D2 = "(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;";

    @Inject(method = {
            "armorCutoutNoCull" + D1,
            "armorTranslucent" + D1,
            "entitySolid" + D1,
            "entitySolidZOffsetForward" + D1,
            "entityCutoutCull" + D1,
            "entityCutout" + D1,
            "entityCutoutZOffset" + D1,
            "entityTranslucentCullItemTarget" + D1,
            "entityTranslucent" + D1,
            "entityTranslucentEmissive" + D1,
            "breezeEyes" + D1
    }, at = @At("RETURN"))
    private static void emissivetexture$track(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        EmissiveModels.trackLayer(cir.getId(), texture, null, cir.getReturnValue());
    }

    @Inject(method = {
            "entityCutout" + D2,
            "entityCutoutZOffset" + D2,
            "entityTranslucent" + D2,
            "entityTranslucentEmissive" + D2
    }, at = @At("RETURN"))
    private static void emissivetexture$trackWithFlag(Identifier texture, boolean affectsOutline,
                                                      CallbackInfoReturnable<RenderType> cir) {
        EmissiveModels.trackLayer(cir.getId(), texture, affectsOutline, cir.getReturnValue());
    }
}