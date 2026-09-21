package net.kztmc.mc.emissivetexture.mixin;

import net.kztmc.mc.emissivetexture.emissive.EmissiveTextureManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderLayers.class)
public abstract class BlockRenderLayersMixin {
    /**
     * SOLIDレイヤーはアルファを捨てないため、透明部分を持つ _e の姉妹クアッドが元の絵を黒く塗ってしまう。
     * textures/block/ に _e がある間だけ、SOLID を CUTOUT に格上げする（不透明テクスチャの見た目は変わらない）。
     */
    @Inject(method = "getBlockLayer", at = @At("RETURN"), cancellable = true)
    private static void emissivetexture$promoteSolid(BlockState state, CallbackInfoReturnable<BlockRenderLayer> cir) {
        if (cir.getReturnValue() == BlockRenderLayer.SOLID && EmissiveTextureManager.snapshot().hasBlockEmissive()) {
            cir.setReturnValue(BlockRenderLayer.CUTOUT);
        }
    }
}