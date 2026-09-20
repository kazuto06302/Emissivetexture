package net.kztmc.mc.emissivetexture.emissive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** BlockEntity/エンティティのモデル描画用。_e に対応する RenderType / スプライトの解決とキャッシュ。 */
public final class EmissiveModels {
    /** sky=15 / block=15。バニラのSubmitNodeCollection内でも同じ値(15728880)が使われている。 */
    public static final int FULL_BRIGHT = 0xF000F0;

    private static final Map<RenderType, RenderType> EMISSIVE_RENDER_TYPES = new ConcurrentHashMap<>();
    private static final Map<TextureAtlasSprite, TextureAtlasSprite> EMISSIVE_SPRITES = new ConcurrentHashMap<>();
    private static volatile @Nullable SpriteGetter spriteGetter;

    private EmissiveModels() {
    }

    public static void setSpriteGetter(SpriteGetter getter) {
        spriteGetter = getter;
    }

    /** リロード開始時に呼ぶ。解決済みの対応表を捨てる。 */
    public static void clear() {
        EMISSIVE_RENDER_TYPES.clear();
        EMISSIVE_SPRITES.clear();
    }

    /** Model#renderType(Identifier) の戻り値を受け取り、_e があれば同じモデルの関数で _e 用 RenderType も作って記録する。 */
    public static void track(Model<?> model, Identifier texture, RenderType base) {
        EmissiveTextureManager.Snapshot snapshot = EmissiveTextureManager.snapshot();
        if (snapshot.isEmpty() || EMISSIVE_RENDER_TYPES.containsKey(base)) {
            return;
        }
        EmissiveFile file = snapshot.findFile(texture);
        if (file == null) {
            return;
        }
        RenderType emissive = model.renderType(file.textureId()); // 再入するが、_e 自身は findFile で除外される
        if (emissive != base) {
            EMISSIVE_RENDER_TYPES.put(base, emissive);
        }
    }

    /** sprite == null 経路：元の RenderType に対応する _e 用 RenderType。無ければ null。 */
    public static @Nullable RenderType emissiveRenderType(RenderType base) {
        return EMISSIVE_RENDER_TYPES.get(base);
    }

    /** sprite != null 経路：同じアトラス内の _e スプライト。アトラスにステッチされていなければ null。 */
    public static @Nullable TextureAtlasSprite emissiveSprite(TextureAtlasSprite base) {
        TextureAtlasSprite cached = EMISSIVE_SPRITES.get(base);
        if (cached != null) {
            return cached;
        }

        SpriteGetter getter = spriteGetter;
        if (getter == null) {
            return null;
        }
        EmissiveTexture pair = EmissiveTextureManager.snapshot().findSprite(base.contents().name());
        if (pair == null) {
            return null;
        }

        TextureAtlasSprite candidate;
        try {
            candidate = getter.get(new SpriteId(base.atlasLocation(), pair.spriteId()));
        } catch (RuntimeException e) {
            return null;
        }
        // 存在しないIDだと欠落テクスチャが返るはずなので、名前とアトラスが一致したものだけ採用する
        if (!candidate.contents().name().equals(pair.spriteId())
                || !candidate.atlasLocation().equals(base.atlasLocation())) {
            return null;
        }
        EMISSIVE_SPRITES.put(base, candidate);
        return candidate;
    }
}