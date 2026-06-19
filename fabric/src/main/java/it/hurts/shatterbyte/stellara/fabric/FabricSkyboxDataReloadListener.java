package it.hurts.shatterbyte.stellara.fabric;

import it.hurts.shatterbyte.stellara.client.sky.data.SkyboxDataReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class FabricSkyboxDataReloadListener extends SkyboxDataReloadListener implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
