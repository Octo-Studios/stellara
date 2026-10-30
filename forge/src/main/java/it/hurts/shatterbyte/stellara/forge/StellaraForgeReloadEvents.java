package it.hurts.shatterbyte.stellara.forge;

import it.hurts.shatterbyte.stellara.Stellara;
import it.hurts.shatterbyte.stellara.client.sky.data.SkyboxDataReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stellara.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StellaraForgeReloadEvents {
    private StellaraForgeReloadEvents() {
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SkyboxDataReloadListener());
    }
}
