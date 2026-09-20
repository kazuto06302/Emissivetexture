package net.kztmc.mc.emissivetexture.emissive;

import net.minecraft.resources.Identifier;

/** リソース走査の結果。spriteId は _e 側のスプライトID、translucent は _e に半透明ピクセルがあるか。 */
public record EmissiveTexture(Identifier spriteId, boolean translucent) {
}