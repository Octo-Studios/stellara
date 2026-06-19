package it.hurts.shatterbyte.stellara.client.sky.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

class SkyboxJson {
    static float getFloat(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    static long getLong(JsonObject json, String key, long fallback) {
        return json.has(key) ? json.get(key).getAsLong() : fallback;
    }

    static String getString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    static String requiredString(JsonObject json, String key) {
        if (!json.has(key))
            throw new JsonParseException("Missing required property: " + key);

        return json.get(key).getAsString();
    }

    static ResourceLocation resourceLocation(String value) {
        var id = ResourceLocation.tryParse(value);

        if (id == null)
            throw new JsonParseException("Invalid resource location: " + value);

        return id;
    }

    static ResourceLocation optionalResourceLocation(JsonObject json, String key) {
        return json.has(key) ? resourceLocation(json.get(key).getAsString()) : null;
    }

    static List<ResourceLocation> resourceLocationList(JsonObject json, String key) {
        if (!json.has(key))
            return List.of();

        return requiredResourceLocationList(json, key);
    }

    static List<ResourceLocation> requiredResourceLocationList(JsonObject json, String key) {
        var values = new ArrayList<ResourceLocation>();

        for (var element : json.getAsJsonArray(key))
            values.add(resourceLocation(element.getAsString()));

        return values;
    }

    static SkyColor color(JsonObject json, String key, SkyColor fallback) {
        if (!json.has(key))
            return fallback;

        var array = json.getAsJsonArray(key);

        return new SkyColor(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    static List<SkyColor> colors(JsonObject json, List<SkyColor> fallback) {
        if (!json.has("colors"))
            return fallback;

        var colors = new ArrayList<SkyColor>();

        for (var element : json.getAsJsonArray("colors")) {
            var array = element.getAsJsonArray();

            colors.add(new SkyColor(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()));
        }

        return colors.isEmpty() ? fallback : colors;
    }

}
