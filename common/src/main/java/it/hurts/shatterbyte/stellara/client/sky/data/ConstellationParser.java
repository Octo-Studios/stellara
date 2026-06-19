package it.hurts.shatterbyte.stellara.client.sky.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.hurts.shatterbyte.stellara.client.sky.content.ConstellationSkyContent;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static it.hurts.shatterbyte.stellara.client.sky.data.SkyboxJson.*;

class ConstellationParser {
    static ConstellationSkyContent parse(ResourceLocation id, int priority, JsonObject json) {
        var seed = getLong(json, "seed", id.hashCode());
        var color = color(json, "color", new SkyColor(0.62F, 1F, 0.74F));
        var centerYaw = getFloat(json, "yaw", Math.floorMod(seed, 6283L) / 1000F);
        var centerPitch = getFloat(json, "pitch", -0.05F + Math.floorMod(seed / 7L, 700L) / 1000F);
        var scale = getFloat(json, "scale", 1F);
        var twinkleSpeed = getFloat(json, "twinkle_speed", 0.025F);
        var lineWidth = getFloat(json, "line_width", 0.4F);
        var textures = textures(json);

        return new ConstellationSkyContent(id, priority, color, centerYaw, centerPitch, scale, twinkleSpeed, lineWidth, textures, shape(json), seed);
    }

    private static List<ResourceLocation> textures(JsonObject json) {
        if (json.has("textures"))
            return requiredResourceLocationList(json, "textures");

        if (json.has("texture"))
            return List.of(resourceLocation(requiredString(json, "texture")));

        return List.of();
    }

    private static ConstellationSkyContent.Shape shape(JsonObject json) {
        if (!json.has("stars"))
            throw new JsonParseException("Missing required property: stars");

        return customShape(json);
    }

    private static ConstellationSkyContent.Shape customShape(JsonObject json) {
        var nodes = new ArrayList<ConstellationSkyContent.Node>();

        for (var element : json.getAsJsonArray("stars")) {
            var star = element.getAsJsonObject();

            nodes.add(new ConstellationSkyContent.Node(
                    getFloat(star, "u", 0F),
                    getFloat(star, "v", 0F),
                    getFloat(star, "size", 1F)
            ));
        }

        var links = new ArrayList<ConstellationSkyContent.Link>();

        if (json.has("lines"))
            for (var element : json.getAsJsonArray("lines")) {
                var pair = element.getAsJsonArray();

                links.add(new ConstellationSkyContent.Link(pair.get(0).getAsInt(), pair.get(1).getAsInt()));
            }

        return new ConstellationSkyContent.Shape(nodes, links);
    }
}
