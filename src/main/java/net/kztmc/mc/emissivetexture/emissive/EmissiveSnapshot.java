package net.kztmc.mc.emissivetexture.emissive;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Identifier;

/** 1回のリロード結果。不変なので複数スレッドから安全に読める。 */
public final class EmissiveSnapshot {
    public static final EmissiveSnapshot EMPTY = new EmissiveSnapshot(Map.of(), Map.of(), false);

    private final Map<Identifier, Identifier> sprites;
    private final Map<Identifier, Identifier> files;
    private final Set<Identifier> emissiveSprites;
    private final Set<Identifier> emissiveFiles;
    private final boolean blockEmissive;

    public EmissiveSnapshot(Map<Identifier, Identifier> sprites, Map<Identifier, Identifier> files, boolean blockEmissive) {
        this.sprites = sprites;
        this.files = files;
        this.emissiveSprites = Set.copyOf(new HashSet<>(sprites.values()));
        this.emissiveFiles = Set.copyOf(new HashSet<>(files.values()));
        this.blockEmissive = blockEmissive;
    }

    public boolean isEmpty() {
        return this.files.isEmpty();
    }

    public int size() {
        return this.files.size();
    }

    /** textures/block/ 以下に _e が1つでもあるか（ブロックのレイヤー格上げの判断に使う）。 */
    public boolean hasBlockEmissive() {
        return this.blockEmissive;
    }

    /** スプライトID(例 minecraft:block/stone)の _e 側ID。_e 自身に対しては null。 */
    public Identifier emissiveSpriteId(Identifier normalSprite) {
        return this.emissiveSprites.contains(normalSprite) ? null : this.sprites.get(normalSprite);
    }

    /** テクスチャファイルID(例 minecraft:textures/entity/zombie/zombie.png)の _e 側ID。_e 自身に対しては null。 */
    public Identifier emissiveFileId(Identifier normalFile) {
        return this.emissiveFiles.contains(normalFile) ? null : this.files.get(normalFile);
    }
}