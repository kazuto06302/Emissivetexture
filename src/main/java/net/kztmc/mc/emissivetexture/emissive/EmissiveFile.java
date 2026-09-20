package net.kztmc.mc.emissivetexture.emissive;

import net.minecraft.resources.Identifier;

/** ファイル単位のペア。textureId は _e 側のテクスチャファイルID（例: minecraft:textures/entity/foo_e.png）。 */
public record EmissiveFile(Identifier textureId, boolean translucent) {
}