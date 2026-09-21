package net.kztmc.mc.emissivetexture.emissive;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** BlockEntity/エンティティのモデル描画用。_e に対応する RenderType / スプライトの解決とキャッシュ。 */
public final class EmissiveModels {
    /** sky=15 / block=15。バニラのSubmitNodeCollection内でも同じ値(15728880)が使われている。 */
    public static final int FULL_BRIGHT = 0xF000F0;
    /** 発光パスを積むキューのorder。バニラが使う小さい値より後ろに描かれる。 */
    public static final int EMISSIVE_ORDER = 1000;

    private static final Logger LOGGER = LogUtils.getLogger();
    /** JVM引数 -Demissivetexture.debug=true で有効になる診断ログ */
    private static final boolean DEBUG = Boolean.getBoolean("emissivetexture.debug");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private static final Map<RenderType, RenderType> EMISSIVE_RENDER_TYPES = new ConcurrentHashMap<>();
    private static final Map<TextureAtlasSprite, TextureAtlasSprite> EMISSIVE_SPRITES = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Method>> FACTORIES = new ConcurrentHashMap<>();
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
            if (DEBUG && REPORTED.add("tracked model " + texture)) {
                LOGGER.info("[emissivetexture][debug] tracked via Model#renderType: {} -> {}", texture, file.textureId());
            }
        }
    }

    /**
     * RenderTypes の生成メソッドの戻り値を受け取り、_e があれば同じメソッドに _e のテクスチャを渡して記録する。
     * flag は (Identifier, boolean) 版の第2引数。1引数版なら null。
     */
    public static void trackLayer(String factory, Identifier texture, @Nullable Boolean flag, RenderType base) {
        EmissiveTextureManager.Snapshot snapshot = EmissiveTextureManager.snapshot();
        if (snapshot.isEmpty() || EMISSIVE_RENDER_TYPES.containsKey(base)) {
            return;
        }
        EmissiveFile file = snapshot.findFile(texture);
        if (file == null) {
            return;
        }

        RenderType emissive = createLayer(factory, file.textureId(), flag);
        if (emissive != null && emissive != base) {
            EMISSIVE_RENDER_TYPES.put(base, emissive);
            if (DEBUG && REPORTED.add("tracked " + factory + " " + texture)) {
                LOGGER.info("[emissivetexture][debug] tracked via RenderTypes.{}: {} -> {}", factory, texture, file.textureId());
            }
        }
    }

    private static @Nullable RenderType createLayer(String factory, Identifier texture, @Nullable Boolean flag) {
        String key = factory + (flag == null ? "/1" : "/2");
        Method method = FACTORIES.computeIfAbsent(key, k -> Optional.ofNullable(findFactory(factory, flag != null))).orElse(null);
        if (method == null) {
            return null;
        }
        try {
            Object result = flag == null ? method.invoke(null, texture) : method.invoke(null, texture, flag);
            return (RenderType) result;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (DEBUG && REPORTED.add("failed " + key)) {
                LOGGER.info("[emissivetexture][debug] failed to create emissive layer via {}", key, e);
            }
            return null;
        }
    }

    private static @Nullable Method findFactory(String factory, boolean withFlag) {
        try {
            return withFlag
                    ? RenderTypes.class.getMethod(factory, Identifier.class, boolean.class)
                    : RenderTypes.class.getMethod(factory, Identifier.class);
        } catch (NoSuchMethodException e) {
            return null;
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

    /** 診断用：_e があるのに対応レイヤーが見つからなかった描画を、組み合わせごとに1回だけ記録する。 */
    public static void debugMiss(String path, String modelClass, RenderType layer) {
        if (DEBUG && REPORTED.add(path + " " + modelClass + " " + layer)) {
            LOGGER.info("[emissivetexture][debug] no emissive layer: path={} model={} layer={}", path, modelClass, layer);
        }
    }
}