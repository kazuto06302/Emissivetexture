package net.kztmc.mc.emissivetexture.emissive;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** アトラス上で解決済みの _e スプライト。 */
public record EmissiveSprite(TextureAtlasSprite sprite, boolean translucent) {
}