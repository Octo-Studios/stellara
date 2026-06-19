package it.hurts.shatterbyte.stellara.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class StellaraFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricSkyboxDataReloadListener());
    }
}
