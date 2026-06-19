package it.hurts.shatterbyte.stellara.client.sky.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

@FunctionalInterface
public interface SkyboxCondition {
    boolean matches(SkyboxRenderContext context);

    default SkyboxCondition and(SkyboxCondition other) {
        return context -> matches(context) && other.matches(context);
    }

    default SkyboxCondition or(SkyboxCondition other) {
        return context -> matches(context) || other.matches(context);
    }

    static SkyboxCondition always() {
        return context -> true;
    }

    static SkyboxCondition biome(ResourceKey<Biome> biome) {
        return context -> biome.equals(context.biomeKey());
    }

    static SkyboxCondition biomeTag(TagKey<Biome> tag) {
        return context -> context.biome().is(tag);
    }

    static SkyboxCondition dimension(ResourceKey<Level> dimension) {
        return context -> dimension.equals(context.level().dimension());
    }
}
