package net.kztmc.mc.emissivetexture.emissive;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.render.model.BakedGeometry;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.Direction;

public final class EmissiveQuads {
    private static final int FULL_EMISSION = 15;

    private EmissiveQuads() {
    }

    /**
     * BakedGeometry.Builder#build の結果に、_e が存在するクアッドの姉妹クアッドを足す。
     * 追加が無ければ元のインスタンスを返す（再ベイクされても二重に足さない）。
     */
    public static BakedGeometry augment(BakedGeometry original) {
        if (EmissiveTextureManager.snapshot().isEmpty() || original == BakedGeometry.EMPTY) {
            return original; // _e が1つも無ければここで終了
        }

        List<BakedQuad> all = original.getAllQuads();
        Map<BakedQuad, BakedQuad> siblings = null;
        for (BakedQuad quad : all) {
            BakedQuad sibling = createSibling(quad, all);
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

        BakedGeometry.Builder builder = new BakedGeometry.Builder();
        for (BakedQuad quad : original.getQuads(null)) {
            builder.add(quad);
            BakedQuad sibling = siblings.get(quad);
            if (sibling != null) {
                builder.add(sibling);
            }
        }
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : original.getQuads(direction)) {
                builder.add(direction, quad);
                BakedQuad sibling = siblings.get(quad);
                if (sibling != null) {
                    builder.add(direction, sibling);
                }
            }
        }
        return builder.build(); // Mixinが再度走るが、姉妹が既にあるので何も追加されず終わる
    }

    private static BakedQuad createSibling(BakedQuad quad, List<BakedQuad> all) {
        Sprite base = quad.sprite();
        Sprite target = EmissiveTextureManager.emissiveSpriteOf(base);
        if (target == null || target == base) {
            return null;
        }

        // 既に同位置・同スプライトの姉妹がある場合は追加しない
        for (BakedQuad other : all) {
            if (other != quad && other.sprite() == target && samePositions(other, quad)) {
                return null;
            }
        }

        float du = base.getMaxU() - base.getMinU();
        float dv = base.getMaxV() - base.getMinV();
        if (du == 0.0F || dv == 0.0F) {
            return null;
        }

        // 通常スプライト内の相対UV -> _e スプライト内の同じ相対UV
        long[] source = {quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3()};
        long[] uv = new long[4];
        for (int i = 0; i < 4; i++) {
            float relU = (Vector2f.getX(source[i]) - base.getMinU()) / du;
            float relV = (Vector2f.getY(source[i]) - base.getMinV()) / dv;
            uv[i] = Vector2f.toLong(
                    target.getMinU() + relU * (target.getMaxU() - target.getMinU()),
                    target.getMinV() + relV * (target.getMaxV() - target.getMinV()));
        }

        // tint=-1（_e のRGBをそのまま使う）、shade=false（面の向きで暗くしない）、発光レベル最大
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                uv[0], uv[1], uv[2], uv[3], -1, quad.face(), target, false, FULL_EMISSION);
    }

    private static boolean samePositions(BakedQuad a, BakedQuad b) {
        return a.position0().equals(b.position0()) && a.position1().equals(b.position1())
                && a.position2().equals(b.position2()) && a.position3().equals(b.position3());
    }
}