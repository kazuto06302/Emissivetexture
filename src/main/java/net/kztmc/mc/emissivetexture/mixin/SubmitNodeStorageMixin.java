package net.kztmc.mc.emissivetexture.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.kztmc.mc.emissivetexture.emissive.EmissiveTextureManager;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SubmitNodeStorage#submitModel は order(0) の SubmitNodeCollection に委譲する。
 * 発光パスはそれより後ろの order に積むことで、通常テクスチャの後に必ず描かれるようにする
 * （同じorder内ではRenderTypeごとの描画順が不定になるため）。
 */
@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageMixin {

    @Inject(method = "submitModel", at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void emissivetexture$submitEmissive(Model<?> model, Object state, PoseStack poseStack,
                                                RenderType renderType, int lightCoords, int overlayCoords,
                                                int tintedColor, TextureAtlasSprite sprite, int outlineColor,
                                                ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                                CallbackInfo ci) {
        if (renderType.isOutline() || EmissiveTextureManager.snapshot().isEmpty()) {
            return; // _e が無ければここで終了（追加コストはvolatile読み取り1回）
        }

        RenderType emissiveType = renderType;
        TextureAtlasSprite emissiveSprite = sprite;
        if (sprite != null) {
            emissiveSprite = EmissiveModels.emissiveSprite(sprite);
            if (emissiveSprite == null) {
                return;
            }
        } else {
            emissiveType = EmissiveModels.emissiveRenderType(renderType);
            if (emissiveType == null) {
                EmissiveModels.debugMiss("submitModel", model.getClass().getName(), renderType);
                return;
            }
        }

        SubmitNodeCollection queue = ((SubmitNodeStorage) (Object) this).order(EmissiveModels.EMISSIVE_ORDER);
        // ライト座標はフルブライト、ティント白、アウトラインとひび割れなし。オーバーレイ（被ダメージの赤み）は引き継ぐ
        queue.submitModel((Model<Object>) model, state, poseStack, emissiveType, EmissiveModels.FULL_BRIGHT,
                overlayCoords, -1, emissiveSprite, 0, null);
    }
}