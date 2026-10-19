package it.hurts.shatterbyte.stellara.neoforge;

import it.hurts.shatterbyte.stellara.Stellara;
import it.hurts.shatterbyte.stellara.client.sky.data.SkyboxDataReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = Stellara.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class StellaraNeoForgeReloadEvents {
    private StellaraNeoForgeReloadEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SkyboxDataReloadListener());
    }
}
