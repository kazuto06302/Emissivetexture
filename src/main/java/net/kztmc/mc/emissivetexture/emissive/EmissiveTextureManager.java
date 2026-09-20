package net.kztmc.mc.emissivetexture.emissive;

import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** *_e.png の検出結果を保持する。走査はリロード時に1回だけ行い、描画中の検索・PNG読み込みは一切しない。 */
public final class EmissiveTextureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEXTURES_DIR = "textures/";
    private static final String SUFFIX = "_e";
    private static final String PNG = ".png";

    private static volatile @Nullable ResourceManager pendingManager;
    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private EmissiveTextureManager() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    /** ModelManager#reload の先頭（メインスレッド）。古い結果は捨てる。 */
    public static void beginReload(ResourceManager manager) {
        pendingManager = manager;
        snapshot = Snapshot.EMPTY;
        EmissiveModels.clear();
    }

    /** ModelManager#loadModels の先頭（ワーカースレッド）。モデルのベイク前にスナップショットを確定する。 */
    public static void onAtlasesReady(SpriteLoader.Preparations blockAtlas, SpriteLoader.Preparations itemAtlas) {
        ResourceManager manager = pendingManager;
        if (manager == null) {
            snapshot = Snapshot.EMPTY;
            return;
        }

        Map<Identifier, EmissiveTexture> sprites = new HashMap<>();
        Map<Identifier, EmissiveFile> files = new HashMap<>();
        scan(manager, sprites, files);
        if (files.isEmpty()) {
            snapshot = Snapshot.EMPTY;
            return;
        }

        Set<Identifier> emissiveSpriteIds = new HashSet<>();
        for (Map.Entry<Identifier, EmissiveTexture> entry : sprites.entrySet()) {
            Identifier id = entry.getValue().spriteId();
            emissiveSpriteIds.add(id);
            // item/ block/ 以外（entity/ など）は別アトラスや直接ロードなので、ここでは警告しない
            String path = entry.getKey().getPath();
            boolean modelSprite = path.startsWith("item/") || path.startsWith("block/");
            if (modelSprite && blockAtlas.getSprite(id) == null && itemAtlas.getSprite(id) == null) {
                LOGGER.warn("[essensivetexture] {} exists but is not stitched into the block/item atlas; it will be ignored.", id);
            }
        }

        Set<Identifier> emissiveFileIds = new HashSet<>();
        for (EmissiveFile file : files.values()) {
            emissiveFileIds.add(file.textureId());
        }

        snapshot = new Snapshot(Map.copyOf(sprites), Set.copyOf(emissiveSpriteIds),
                Map.copyOf(files), Set.copyOf(emissiveFileIds), blockAtlas, itemAtlas);
        LOGGER.info("[essensivetexture] Detected {} emissive texture(s).", files.size());
    }

    /** ResourceManager（バニラ・リソースパック・Modの全namespace）から textures/** の *_e.png を探す。 */
    private static void scan(ResourceManager manager, Map<Identifier, EmissiveTexture> sprites,
                             Map<Identifier, EmissiveFile> files) {
        Map<Identifier, Resource> found = manager.listResources("textures", id -> id.getPath().endsWith(SUFFIX + PNG));

        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath();
            if (!path.startsWith(TEXTURES_DIR)) {
                continue;
            }

            String base = path.substring(TEXTURES_DIR.length(), path.length() - (SUFFIX.length() + PNG.length()));
            if (base.isEmpty() || base.endsWith("/")) {
                continue;
            }

            // 通常テクスチャが存在しない _e.png は無視する
            Identifier normalFile = Identifier.fromNamespaceAndPath(file.getNamespace(), TEXTURES_DIR + base + PNG);
            if (manager.getResource(normalFile).isEmpty()) {
                continue;
            }

            boolean translucent = hasTranslucentPixel(file, entry.getValue());
            Identifier normalSprite = Identifier.fromNamespaceAndPath(file.getNamespace(), base);
            Identifier emissiveSprite = Identifier.fromNamespaceAndPath(file.getNamespace(), base + SUFFIX);
            sprites.put(normalSprite, new EmissiveTexture(emissiveSprite, translucent));
            files.put(normalFile, new EmissiveFile(file, translucent));
        }
    }

    private static boolean hasTranslucentPixel(Identifier id, Resource resource) {
        try (InputStream in = resource.open()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return false;
            }
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    if (alpha != 0 && alpha != 255) {
                        return true;
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            LOGGER.warn("[essensivetexture] Failed to inspect {}", id, e);
        }
        return false;
    }

    /** 1回のリロード結果。不変オブジェクトなので複数スレッドから安全に読める。 */
    public static final class Snapshot {
        static final Snapshot EMPTY = new Snapshot(Map.of(), Set.of(), Map.of(), Set.of(), null, null);

        private final Map<Identifier, EmissiveTexture> sprites;
        private final Set<Identifier> emissiveSpriteIds;
        private final Map<Identifier, EmissiveFile> files;
        private final Set<Identifier> emissiveFileIds;
        private final SpriteLoader.@Nullable Preparations blockAtlas;
        private final SpriteLoader.@Nullable Preparations itemAtlas;

        private Snapshot(Map<Identifier, EmissiveTexture> sprites, Set<Identifier> emissiveSpriteIds,
                         Map<Identifier, EmissiveFile> files, Set<Identifier> emissiveFileIds,
                         SpriteLoader.@Nullable Preparations blockAtlas, SpriteLoader.@Nullable Preparations itemAtlas) {
            this.sprites = sprites;
            this.emissiveSpriteIds = emissiveSpriteIds;
            this.files = files;
            this.emissiveFileIds = emissiveFileIds;
            this.blockAtlas = blockAtlas;
            this.itemAtlas = itemAtlas;
        }

        public boolean isEmpty() {
            return this.files.isEmpty();
        }

        /** モデルのクアッド用：通常スプライトに対応する _e スプライトを同じアトラス内から探す。 */
        public @Nullable EmissiveSprite find(TextureAtlasSprite base) {
            Identifier id = base.contents().name();
            EmissiveTexture texture = this.sprites.get(id);
            if (texture == null || this.emissiveSpriteIds.contains(id)) {
                return null;
            }

            SpriteLoader.Preparations atlas =
                    base.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? this.blockAtlas : this.itemAtlas;
            if (atlas == null) {
                return null;
            }

            TextureAtlasSprite sprite = atlas.getSprite(texture.spriteId());
            if (sprite == null || !sprite.atlasLocation().equals(base.atlasLocation())) {
                return null;
            }
            return new EmissiveSprite(sprite, texture.translucent());
        }

        /** BlockEntity/エンティティのアトラス経路用：スプライトIDだけで対応を引く。 */
        public @Nullable EmissiveTexture findSprite(Identifier spriteId) {
            return this.emissiveSpriteIds.contains(spriteId) ? null : this.sprites.get(spriteId);
        }

        /** エンティティの直接テクスチャ経路用：テクスチャファイルIDで対応を引く。 */
        public @Nullable EmissiveFile findFile(Identifier textureFile) {
            return this.emissiveFileIds.contains(textureFile) ? null : this.files.get(textureFile);
        }
    }
}