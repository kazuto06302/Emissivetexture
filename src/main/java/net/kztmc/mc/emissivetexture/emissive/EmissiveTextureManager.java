package net.kztmc.mc.emissivetexture.emissive;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

/**
 * *_e.png の検出結果を保持する。走査はリロード開始時に1回だけ。
 * 描画中は、このクラスのマップ引きとキャッシュだけを使い、ResourceManagerやPNGには触れない。
 */
public final class EmissiveTextureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEXTURES_DIR = "textures/";
    private static final String PNG = ".png";
    private static final String EMISSIVE_PNG = "_e.png";

    /** アトラスID -> 最新のステッチ結果（SpriteLoader.StitchResult の生成時に登録される）。 */
    private static final Map<Identifier, SpriteLoader.StitchResult> ATLASES = new ConcurrentHashMap<>();
    private static volatile EmissiveSnapshot snapshot = EmissiveSnapshot.EMPTY;

    private EmissiveTextureManager() {
    }

    public static EmissiveSnapshot snapshot() {
        return snapshot;
    }

    /** BakedModelManager#reload の先頭。現在有効な ResourceManager から _e を走査し直す。 */
    public static void beginReload(ResourceManager manager) {
        EmissiveSnapshot next = scan(manager);
        snapshot = next;
        EmissiveModels.clear();
        LOGGER.info("[emissivetexture] Detected {} emissive texture(s).", next.size());
    }

    /** SpriteLoader.StitchResult が作られるたびに呼ばれる。アトラスIDは missing スプライトから取る。 */
    public static void registerAtlas(SpriteLoader.StitchResult result) {
        ATLASES.put(result.missing().getAtlasId(), result);
        EmissiveModels.clear();
    }

    /** 通常スプライトに対応する _e スプライトを同じアトラスから探す。無ければ null。 */
    public static Sprite emissiveSpriteOf(Sprite base) {
        EmissiveSnapshot current = snapshot;
        if (current.isEmpty()) {
            return null;
        }

        Identifier emissiveId = current.emissiveSpriteId(base.getContents().getId());
        if (emissiveId == null) {
            return null;
        }

        SpriteLoader.StitchResult atlas = ATLASES.get(base.getAtlasId());
        if (atlas == null) {
            return null;
        }

        Sprite sprite = atlas.getSprite(emissiveId);
        if (sprite == null || sprite == atlas.missing() || !sprite.getAtlasId().equals(base.getAtlasId())) {
            return null;
        }
        return sprite;
    }

    private static EmissiveSnapshot scan(ResourceManager manager) {
        var pngs = manager.findResources("textures", id -> id.getPath().endsWith(PNG));
        LOGGER.info("[emissivetexture] Scanning {} png file(s) under textures/.", pngs.size());

        Map<Identifier, Identifier> sprites = new HashMap<>();
        Map<Identifier, Identifier> files = new HashMap<>();
        boolean blockEmissive = false;

        for (Identifier file : pngs.keySet()) {
            String path = file.getPath();
            if (!path.startsWith(TEXTURES_DIR) || !path.endsWith(EMISSIVE_PNG)) {
                continue;
            }

            String base = path.substring(TEXTURES_DIR.length(), path.length() - EMISSIVE_PNG.length());
            if (base.isEmpty() || base.endsWith("/")) {
                continue;
            }

            // 通常テクスチャが存在しない _e.png は無視する
            Identifier normalFile = Identifier.of(file.getNamespace(), TEXTURES_DIR + base + PNG);
            if (!pngs.containsKey(normalFile)) {
                continue;
            }

            sprites.put(Identifier.of(file.getNamespace(), base), Identifier.of(file.getNamespace(), base + "_e"));
            files.put(normalFile, file);
            if (base.startsWith("block/")) {
                blockEmissive = true;
            }
        }

        if (files.isEmpty()) {
            return EmissiveSnapshot.EMPTY;
        }
        return new EmissiveSnapshot(Map.copyOf(sprites), Map.copyOf(files), blockEmissive);
    }
}