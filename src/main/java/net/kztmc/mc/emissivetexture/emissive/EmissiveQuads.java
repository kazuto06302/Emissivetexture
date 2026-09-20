package net.kztmc.mc.emissivetexture.emissive;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class EmissiveQuads {
    private static final int FULL_EMISSION = 15;

    private EmissiveQuads() {
    }

    /**
     * QuadCollection.Builder#build の結果に、_e が存在するクアッドの姉妹クアッドを足す。
     * 追加が無ければ元のインスタンスをそのまま返す（再ベイクされても二重追加しない）。
     */
    public static QuadCollection augment(QuadCollection original) {
        EmissiveTextureManager.Snapshot snapshot = EmissiveTextureManager.snapshot();
        if (snapshot.isEmpty() || original == QuadCollection.EMPTY) {
            return original; // _e が1つも無ければ即スキップ
        }

        List<BakedQuad> all = original.getAll();
        Map<BakedQuad, BakedQuad> siblings = null;
        for (BakedQuad quad : all) {
            BakedQuad sibling = createSibling(quad, all, snapshot);
            if (sibling != null) {
                if (siblings == null) {
                    siblings = new IdentityHashMap<>();
                }
                siblings.put(quad, sibling);
            }
        }
        if (siblings == null) {
            return original;
        }

        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : original.getQuads(null)) {
            builder.addUnculledFace(quad);
            BakedQuad sibling = siblings.get(quad);
            if (sibling != null) {
                builder.addUnculledFace(sibling);
            }
        }
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : original.getQuads(direction)) {
                builder.addCulledFace(direction, quad);
                BakedQuad sibling = siblings.get(quad);
                if (sibling != null) {
                    builder.addCulledFace(direction, sibling);
                }
            }
        }
        return builder.build(); // ここで再びMixinが走るが、姉妹が既にあるので何も追加されず終了する
    }

    private static @Nullable BakedQuad createSibling(BakedQuad quad, List<BakedQuad> all,
                                                     EmissiveTextureManager.Snapshot snapshot) {
        TextureAtlasSprite base = quad.materialInfo().sprite();
        EmissiveSprite emissive = snapshot.find(base);
        if (emissive == null) {
            return null;
        }
        TextureAtlasSprite target = emissive.sprite();

        // 既に同位置・同スプライトの姉妹がある場合は追加しない
        for (BakedQuad other : all) {
            if (other != quad && other.materialInfo().sprite() == target && samePositions(other, quad)) {
                return null;
            }
        }

        float du = base.getU1() - base.getU0();
        float dv = base.getV1() - base.getV0();
        if (du == 0.0F || dv == 0.0F) {
            return null;
        }

        // 通常スプライト内の相対UV -> _e スプライト内の同じ相対UV
        long[] uv = new long[BakedQuad.VERTEX_COUNT];
        for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
            long packed = quad.packedUV(i);
            float relU = (UVPair.unpackU(packed) - base.getU0()) / du;
            float relV = (UVPair.unpackV(packed) - base.getV0()) / dv;
            uv[i] = UVPair.pack(
                    target.getU0() + relU * (target.getU1() - target.getU0()),
                    target.getV0() + relV * (target.getV1() - target.getV0()));
        }

        boolean translucent = emissive.translucent();
        boolean blockAtlas = target.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS);
        ChunkSectionLayer layer = translucent ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT;
        RenderType itemRenderType = translucent
                ? (blockAtlas ? Sheets.translucentBlockItemSheet() : Sheets.translucentItemSheet())
                : (blockAtlas ? Sheets.cutoutBlockItemSheet() : Sheets.cutoutItemSheet());

        // tint=-1（_e のRGBをそのまま使う）、shade=false（面の向きで暗くしない）、発光レベル最大
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(target, layer, itemRenderType, -1, false, FULL_EMISSION);
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                uv[0], uv[1], uv[2], uv[3], quad.direction(), info);
    }

    private static boolean samePositions(BakedQuad a, BakedQuad b) {
        for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
            if (!a.position(i).equals(b.position(i))) {
                return false;
            }
        }
        return true;
    }
}