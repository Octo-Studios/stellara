package it.hurts.shatterbyte.stellara.client.sky.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.hurts.shatterbyte.stellara.client.sky.content.TexturedPlanetSystemSkyContent;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static it.hurts.shatterbyte.stellara.client.sky.data.SkyboxJson.*;

class PlanetSystemParser {
    static TexturedPlanetSystemSkyContent parse(ResourceLocation id, int priority, JsonObject json) {
        return new TexturedPlanetSystemSkyContent(id, priority, planets(json));
    }

    private static List<TexturedPlanetSystemSkyContent.Planet> planets(JsonObject json) {
        if (!json.has("planets"))
            throw new JsonParseException("Missing required property: planets");

        var planets = new ArrayList<TexturedPlanetSystemSkyContent.Planet>();

        for (var element : json.getAsJsonArray("planets")) {
            var planet = element.getAsJsonObject();
            var texture = resourceLocation(requiredString(planet, "texture"));

            planets.add(new TexturedPlanetSystemSkyContent.Planet(
                    texture,
                    getFloat(planet, "yaw", 0F),
                    getFloat(planet, "pitch", 0F),
                    getFloat(planet, "size", 8F),
                    getFloat(planet, "roll", 0F),
                    getFloat(planet, "roll_speed", 0F),
                    color(planet, "color", new SkyColor(1F, 1F, 1F)),
                    getFloat(planet, "opacity", 1F),
                    satellites(planet)
            ));
        }

        return planets;
    }

    private static List<TexturedPlanetSystemSkyContent.Satellite> satellites(JsonObject planet) {
        if (!planet.has("satellites"))
            return List.of();

        var satellites = new ArrayList<TexturedPlanetSystemSkyContent.Satellite>();

        for (var element : planet.getAsJsonArray("satellites")) {
            var satellite = element.getAsJsonObject();
            var texture = resourceLocation(requiredString(satellite, "texture"));

            satellites.add(new TexturedPlanetSystemSkyContent.Satellite(
                    texture,
                    getFloat(satellite, "size", 2.5F),
                    getFloat(satellite, "orbit_radius", 5F),
                    getFloat(satellite, "orbit_speed", 0.01F),
                    getFloat(satellite, "orbit_offset", 0F),
                    getFloat(satellite, "roll", 0F),
                    color(satellite, "color", new SkyColor(1F, 1F, 1F)),
                    getFloat(satellite, "opacity", 1F)
            ));
        }

        return satellites;
    }
}
