package it.hurts.shatterbyte.stellara.client.sky.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.hurts.shatterbyte.stellara.Stellara;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxCondition;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxEntry;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxRegistry;
import it.hurts.shatterbyte.stellara.client.sky.content.*;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.hurts.shatterbyte.stellara.client.sky.data.SkyboxJson.*;

public class SkyboxDataReloadListener extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation ID = new ResourceLocation(Stellara.MODID, "skyboxes");
    private static final Gson GSON = new Gson();

    public SkyboxDataReloadListener() {
        super(GSON, ID.getPath());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        var entries = new ArrayList<SkyboxEntry>();

        for (var entry : elements.entrySet()) {
            try {
                entries.addAll(parseEntries(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (RuntimeException exception) {
                throw new JsonParseException("Failed to parse skybox '" + entry.getKey() + "'", exception);
            }
        }

        SkyboxRegistry.replaceDataEntries(entries);
    }

    private static List<SkyboxEntry> parseEntries(ResourceLocation id, JsonObject json) {
        if (!json.has("layers"))
            return List.of(parse(id, json));

        var entries = new ArrayList<SkyboxEntry>();
        var layers = json.getAsJsonArray("layers");

        for (var index = 0; index < layers.size(); index++) {
            var layer = layers.get(index).getAsJsonObject().deepCopy();

            inherit(json, layer, "biomes");
            inherit(json, layer, "biome_tags");
            inherit(json, layer, "dimensions");
            inherit(json, layer, "time");
            inherit(json, layer, "fade_speed");
            inherit(json, layer, "weight");

            entries.add(parse(new ResourceLocation(id.getNamespace(), id.getPath() + "/layer_" + index), layer));
        }

        return entries;
    }

    private static void inherit(JsonObject parent, JsonObject layer, String key) {
        if (parent.has(key) && !layer.has(key))
            layer.add(key, parent.get(key));
    }

    private static SkyboxEntry parse(ResourceLocation id, JsonObject json) {
        var type = resourceLocation(requiredString(json, "type"));
        var priority = getInt(json, "priority", 0);
        var weight = getInt(json, "weight", 0);
        var fadeSpeed = getFloat(json, "fade_speed", 0.08F);
        var condition = parseCondition(json);

        return new SkyboxEntry(switch (type.toString()) {
            case "stellara:shooting_stars" -> new ShootingStarSkyContent(
                    id,
                    priority,
                    getInt(json, "count", 4),
                    color(json, "color", new SkyColor(0.7F, 0.9F, 1F)),
                    getLong(json, "seed", id.hashCode()),
                    getInt(json, "interval_ticks", 100),
                    getInt(json, "duration_ticks", 24)
            );
            case "stellara:constellation" -> ConstellationParser.parse(id, priority, json);
            case "stellara:star_field" -> new StarFieldSkyContent(
                    id,
                    priority,
                    requiredResourceLocationList(json, "textures"),
                    getInt(json, "count", 500),
                    getFloat(json, "min_size", 0.5F),
                    getFloat(json, "max_size", 1.5F),
                    color(json, "color", new SkyColor(1F, 1F, 1F)),
                    getLong(json, "seed", id.hashCode())
            );
            case "stellara:planet_system" -> PlanetSystemParser.parse(id, priority, json);
            case "stellara:sky_tint" -> new SkyTintContent(
                    id,
                    priority,
                    color(json, "color", new SkyColor(0.45F, 0.78F, 1F)),
                    getFloat(json, "opacity", 0.075F)
            );
            case "stellara:rainbow" -> new RainbowSkyContent(
                    id,
                    priority,
                    colors(json, List.of(
                            new SkyColor(1F, 0.16F, 0.12F),
                            new SkyColor(1F, 0.55F, 0.08F),
                            new SkyColor(1F, 0.92F, 0.12F),
                            new SkyColor(0.16F, 0.82F, 0.24F),
                            new SkyColor(0.12F, 0.48F, 1F),
                            new SkyColor(0.44F, 0.22F, 0.92F)
                    )),
                    getFloat(json, "yaw", 3.14F),
                    getFloat(json, "base_pitch", -0.22F),
                    getFloat(json, "radius", 0.86F),
                    getFloat(json, "thickness", 0.08F),
                    getInt(json, "segments", 64),
                    getFloat(json, "arc", 3.14F),
                    getFloat(json, "opacity", 0.34F),
                    getFloat(json, "blur", 1F)
            );
            case "stellara:aurora" -> new AuroraSkyContent(
                    id,
                    priority,
                    colors(json, List.of(
                            color(json, "lower_color", new SkyColor(0.08F, 0.82F, 0.92F)),
                            color(json, "upper_color", new SkyColor(0.62F, 0.96F, 1F))
                    )),
                    getInt(json, "bands", 4),
                    getInt(json, "segments", 48),
                    getInt(json, "gradient_steps", 5),
                    getFloat(json, "wave_amplitude", 0.06F),
                    getFloat(json, "wave_frequency", 2.4F),
                    getFloat(json, "speed", 0.012F),
                    getFloat(json, "opacity", 0.32F),
                    getFloat(json, "center_yaw", 3.14F),
                    getFloat(json, "width", 1.9F),
                    getFloat(json, "min_pitch", 0.18F),
                    getFloat(json, "max_pitch", 1.12F),
                    getFloat(json, "ray_strength", 0.45F),
                    getFloat(json, "edge_softness", 0.35F),
                    getFloat(json, "depth_amplitude", 0.28F),
                    getFloat(json, "depth_offset", 0F),
                    getFloat(json, "sphere_radius", 100F)
            );
            case "stellara:ribbons" -> new SkyRibbonContent(
                    id,
                    priority,
                    colors(json, List.of(
                            new SkyColor(0.92F, 0.38F, 0.72F),
                            new SkyColor(0.72F, 0.46F, 1F),
                            new SkyColor(0.38F, 0.82F, 1F)
                    )),
                    getInt(json, "segments", 128),
                    getInt(json, "gradient_steps", 8),
                    getFloat(json, "center_yaw", 3.14F),
                    getFloat(json, "base_pitch", 0.28F),
                    getFloat(json, "spacing", 0.08F),
                    getFloat(json, "thickness", 0.12F),
                    getFloat(json, "arc", Mth.TWO_PI),
                    getFloat(json, "tilt", 0F),
                    getFloat(json, "wave_amplitude", 0.035F),
                    getFloat(json, "wave_frequency", 2.2F),
                    getFloat(json, "wobble_amplitude", 0.025F),
                    getFloat(json, "opacity", 0.28F),
                    getFloat(json, "speed", 0.012F),
                    getFloat(json, "edge_softness", 0.2F),
                    getFloat(json, "blur", 1F),
                    getLong(json, "seed", id.hashCode())
            );
            default -> throw new JsonParseException("Unknown skybox type: " + type);
        }, condition, fadeSpeed, weight);
    }

    private static SkyboxCondition parseCondition(JsonObject json) {
        var biomeKeys = resourceLocationList(json, "biomes").stream()
                .map(id -> ResourceKey.create(Registries.BIOME, id))
                .toList();
        var biomeTags = resourceLocationList(json, "biome_tags").stream()
                .map(id -> TagKey.create(Registries.BIOME, id))
                .toList();
        var dimensions = resourceLocationList(json, "dimensions").stream()
                .map(id -> ResourceKey.create(Registries.DIMENSION, id))
                .toList();
        var timeWindow = timeWindow(json);

        return context -> {
            var hasBiomeFilter = !biomeKeys.isEmpty() || !biomeTags.isEmpty();
            var biomeMatches = !hasBiomeFilter
                    || biomeKeys.contains(context.biomeKey())
                    || biomeTags.stream().anyMatch(tag -> context.biome().is(tag));
            var dimensionMatches = dimensions.isEmpty() || dimensions.contains(context.level().dimension());
            var timeMatches = timeWindow == null || timeWindow.matches(context.dayTime());

            return biomeMatches && dimensionMatches && timeMatches;
        };
    }

    private static TimeWindow timeWindow(JsonObject json) {
        if (!json.has("time"))
            return null;

        var time = json.getAsJsonObject("time");

        return new TimeWindow(normalizeDayTime(getFloat(time, "from", 0F)), normalizeDayTime(getFloat(time, "to", 24000F)));
    }

    private static float normalizeDayTime(float time) {
        return Math.floorMod((int) time, 24000);
    }

    private record TimeWindow(float from, float to) {
        private boolean matches(float time) {
            if (from == to)
                return true;

            if (from < to)
                return time >= from && time <= to;

            return time >= from || time <= to;
        }
    }
}
