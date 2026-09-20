package net.kztmc.mc.emissivetexture.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.kztmc.mc.emissivetexture.emissive.EmissiveTextureManager;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.LoadedBlockModels;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {
    /** リロード開始：現在有効な ResourceManager を控え、古い検出結果を捨てる。 */
    @Inject(method = "reload", at = @At("HEAD"))
    private void essensivetexture$beginReload(PreparableReloadListener.SharedState currentReload,
                                              Executor taskExecutor,
                                              PreparableReloadListener.PreparationBarrier preparationBarrier,
                                              Executor reloadExecutor,
                                              CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        EmissiveTextureManager.beginReload(currentReload.resourceManager());
    }

    /** アトラスのスプライトが揃った直後、モデルをベイクする前に _e を走査してスナップショットを作る。 */
    @Inject(method = "loadModels", at = @At("HEAD"))
    private static void essensivetexture$atlasesReady(SpriteLoader.Preparations blockAtlas,
                                                      SpriteLoader.Preparations itemAtlas,
                                                      ModelBakery bakery,
                                                      LoadedBlockModels blockModels,
                                                      Object2IntMap<BlockState> modelGroups,
                                                      EntityModelSet entityModelSet,
                                                      Executor taskExecutor,
                                                      CallbackInfoReturnable<CompletableFuture<?>> cir) {
        EmissiveTextureManager.onAtlasesReady(blockAtlas, itemAtlas);
    }
}