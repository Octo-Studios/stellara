package it.hurts.shatterbyte.stellara.client.sky.api;

import net.minecraft.resources.ResourceLocation;

public interface SkyboxContent {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    default float targetAlpha(SkyboxRenderContext context) {
        return context.visibility();
    }

    void render(SkyboxRenderContext context, float alpha);
}
