package it.hurts.shatterbyte.stellara.forge;

import it.hurts.shatterbyte.stellara.Stellara;
import it.hurts.shatterbyte.stellara.client.sky.data.SkyboxDataReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stellara.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StellaraForgeReloadEvents {
    private StellaraForgeReloadEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SkyboxDataReloadListener());
    }
}
