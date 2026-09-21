package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.kztmc.mc.emissivetexture.emissive.EmissiveTextureManager;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通常の描画は order(0) に積まれる。発光パスはそれより後ろの order に積むことで、
 * 通常テクスチャの後に必ず描かれるようにする（同じorder内ではRenderLayerごとの描画順が不定になるため）。
 */
@Mixin(OrderedRenderCommandQueueImpl.class)
public abstract class OrderedRenderCommandQueueMixin {

    @Inject(method = "submitModel", at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void emissivetexture$submitEmissive(Model<?> model, Object state, MatrixStack matrices,
                                                RenderLayer renderLayer, int light, int overlay,
                                                int tintedColor, Sprite sprite, int outlineColor,
                                                ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
                                                CallbackInfo ci) {
        if (EmissiveTextureManager.snapshot().isEmpty()) {
            return; // _e が無ければここで終了（追加コストはvolatile読み取り1回）
        }

        RenderLayer emissiveLayer = renderLayer;
        Sprite emissiveSprite = sprite;
        if (sprite != null) {
            emissiveSprite = EmissiveModels.emissiveSprite(sprite);
            if (emissiveSprite == null) {
                return;
            }
        } else {
            emissiveLayer = EmissiveModels.emissiveLayer(renderLayer);
            if (emissiveLayer == null) {
                EmissiveModels.debugMiss("submitModel", model.getClass().getName(), renderLayer);
                return;
            }
        }

        BatchingRenderCommandQueue queue =
                ((OrderedRenderCommandQueueImpl) (Object) this).getBatchingQueue(EmissiveModels.EMISSIVE_ORDER);
        // ライト座標はフルブライト、ティント白、アウトラインとひび割れなし。オーバーレイ（被ダメージの赤み）は引き継ぐ
        queue.submitModel((Model<Object>) model, state, matrices, emissiveLayer, EmissiveModels.FULL_BRIGHT,
                overlay, -1, emissiveSprite, 0, null);
    }

    /** モデルパーツ専用のキューを通る描画も同じ方法で覆う。 */
    @Inject(method = "submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V",
            at = @At("TAIL"))
    private void emissivetexture$submitEmissivePart(ModelPart part, MatrixStack matrices, RenderLayer renderLayer,
                                                    int light, int overlay, Sprite sprite, boolean sheeted,
                                                    boolean hasGlint, int tintedColor,
                                                    ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
                                                    int outlineColor, CallbackInfo ci) {
        if (EmissiveTextureManager.snapshot().isEmpty()) {
            return;
        }

        RenderLayer emissiveLayer = renderLayer;
        Sprite emissiveSprite = sprite;
        if (sprite != null) {
            emissiveSprite = EmissiveModels.emissiveSprite(sprite);
            if (emissiveSprite == null) {
                return;
            }
        } else {
            emissiveLayer = EmissiveModels.emissiveLayer(renderLayer);
            if (emissiveLayer == null) {
                EmissiveModels.debugMiss("submitModelPart", "ModelPart", renderLayer);
                return;
            }
        }

        BatchingRenderCommandQueue queue =
                ((OrderedRenderCommandQueueImpl) (Object) this).getBatchingQueue(EmissiveModels.EMISSIVE_ORDER);
        // エンチャントの光沢は二重にならないよう外す
        queue.submitModelPart(part, matrices, emissiveLayer, EmissiveModels.FULL_BRIGHT, overlay,
                emissiveSprite, sheeted, false, -1, null, 0);
    }
}