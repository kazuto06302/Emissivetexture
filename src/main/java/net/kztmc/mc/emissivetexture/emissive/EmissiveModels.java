package net.kztmc.mc.emissivetexture.emissive;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

/** BlockEntity/エンティティのモデル描画用。_e に対応する RenderLayer / スプライトの解決とキャッシュ。 */
public final class EmissiveModels {
    /** sky=15 / block=15 */
    public static final int FULL_BRIGHT = 0xF000F0;
    /** 発光パスを積むキューのorder。バニラが使う小さい値より後ろに描かれる。 */
    public static final int EMISSIVE_ORDER = 1000;

    private static final Logger LOGGER = LogUtils.getLogger();
    /** JVM引数 -Demissivetexture.debug=true で有効になる診断ログ */
    private static final boolean DEBUG = Boolean.getBoolean("emissivetexture.debug");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private static final Map<RenderLayer, RenderLayer> LAYERS = new ConcurrentHashMap<>();
    private static final Map<Sprite, Optional<Sprite>> SPRITES = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Method>> FACTORIES = new ConcurrentHashMap<>();

    private EmissiveModels() {
    }

    /** リロード開始・アトラス再ステッチのたびに、解決済みの対応表を捨てる。 */
    public static void clear() {
        LAYERS.clear();
        SPRITES.clear();
    }

    /** Model#getLayer(Identifier) の戻り値を受け取り、_e があれば同じモデルの関数で _e 用 RenderLayer も作って記録する。 */
    public static void track(Model<?> model, Identifier texture, RenderLayer base) {
        EmissiveSnapshot snapshot = EmissiveTextureManager.snapshot();
        if (snapshot.isEmpty() || LAYERS.containsKey(base)) {
            return;
        }
        Identifier emissiveFile = snapshot.emissiveFileId(texture);
        if (emissiveFile == null) {
            return;
        }
        RenderLayer emissive = model.getLayer(emissiveFile); // 再入するが、_e 自身は emissiveFileId で除外される
        if (emissive != base) {
            LAYERS.put(base, emissive);
            if (DEBUG && REPORTED.add("tracked model " + texture)) {
                LOGGER.info("[emissivetexture][debug] tracked via Model#getLayer: {} -> {}", texture, emissiveFile);
            }
        }
    }

    /**
     * RenderLayers の生成メソッドの戻り値を受け取り、_e があれば同じメソッドに _e のテクスチャを渡して記録する。
     * flag は (Identifier, boolean) 版の第2引数。1引数版なら null。
     */
    public static void trackLayer(String factory, Identifier texture, Boolean flag, RenderLayer base) {
        EmissiveSnapshot snapshot = EmissiveTextureManager.snapshot();
        if (snapshot.isEmpty() || LAYERS.containsKey(base)) {
            return;
        }
        Identifier emissiveFile = snapshot.emissiveFileId(texture);
        if (emissiveFile == null) {
            return;
        }

        RenderLayer emissive = createLayer(factory, emissiveFile, flag);
        if (emissive != null && emissive != base) {
            LAYERS.put(base, emissive);
            if (DEBUG && REPORTED.add("tracked " + factory + " " + texture)) {
                LOGGER.info("[emissivetexture][debug] tracked via RenderLayers.{}: {} -> {}", factory, texture, emissiveFile);
            }
        }
    }

    private static RenderLayer createLayer(String factory, Identifier texture, Boolean flag) {
        String key = factory + (flag == null ? "/1" : "/2");
        Method method = FACTORIES.computeIfAbsent(key, k -> Optional.ofNullable(findFactory(factory, flag != null))).orElse(null);
        if (method == null) {
            return null;
        }
        try {
            Object result = flag == null ? method.invoke(null, texture) : method.invoke(null, texture, flag);
            return (RenderLayer) result;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (DEBUG && REPORTED.add("failed " + key)) {
                LOGGER.info("[emissivetexture][debug] failed to create emissive layer via {}", key, e);
            }
            return null;
        }
    }

    private static Method findFactory(String factory, boolean withFlag) {
        try {
            return withFlag
                    ? RenderLayers.class.getMethod(factory, Identifier.class, boolean.class)
                    : RenderLayers.class.getMethod(factory, Identifier.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /** sprite == null 経路：元の RenderLayer に対応する _e 用 RenderLayer。無ければ null。 */
    public static RenderLayer emissiveLayer(RenderLayer base) {
        return LAYERS.get(base);
    }

    /** sprite != null 経路：同じアトラス内の _e スプライト。無ければ null。 */
    public static Sprite emissiveSprite(Sprite base) {
        return SPRITES.computeIfAbsent(base, b -> Optional.ofNullable(EmissiveTextureManager.emissiveSpriteOf(b))).orElse(null);
    }

    /** 診断用：_e があるのに対応レイヤーが見つからなかった描画を、組み合わせごとに1回だけ記録する。 */
    public static void debugMiss(String path, String modelClass, RenderLayer layer) {
        if (DEBUG && REPORTED.add(path + " " + modelClass + " " + layer)) {
            LOGGER.info("[emissivetexture][debug] no emissive layer: path={} model={} layer={}", path, modelClass, layer);
        }
    }
}