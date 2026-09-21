package net.kztmc.mc.emissivetexture;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;

public class Emissivetexture implements ClientModInitializer {
    public static final String MOD_ID = "emissivetexture";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        LOGGER.info("[{}] loaded. *_e.png are detected on every resource reload.", MOD_ID);
    }
}