package net.kztmc.mc.emissivetexture.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kztmc.mc.emissivetexture.emissive.EmissiveModels;
import net.kztmc.mc.emissivetexture.emissive.EmissiveTextureManager;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionMixin {
    /** 自分で追加したsubmitへの再入を防ぐ（インスタンスごと・単一スレッドで使われる前提）。 */
    @Unique
    private boolean essensivetexture$emitting;

    @Inject(method = "submitModel", at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void essensivetexture$submitEmissive(Model<?> model, Object state, PoseStack poseStack,
                                                 RenderType renderType, int lightCoords, int overlayCoords,
                                                 int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor,
                                                 ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
                                                 CallbackInfo ci) {
        if (this.essensivetexture$emitting || renderType.isOutline() || EmissiveTextureManager.snapshot().isEmpty()) {
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
                return;
            }
        }

        this.essensivetexture$emitting = true;
        try {
            OrderedSubmitNodeCollector self = (OrderedSubmitNodeCollector) (Object) this;
            self.submitModel((Model<Object>) model, state, poseStack, emissiveType, EmissiveModels.FULL_BRIGHT,
                    overlayCoords, -1, emissiveSprite, 0, null);
        } finally {
            this.essensivetexture$emitting = false;
        }
    }
}