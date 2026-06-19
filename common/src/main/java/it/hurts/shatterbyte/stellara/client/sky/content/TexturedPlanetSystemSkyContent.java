package it.hurts.shatterbyte.stellara.client.sky.content;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxContent;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxRenderContext;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import it.hurts.shatterbyte.stellara.client.sky.render.SkyGeometry;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

public class TexturedPlanetSystemSkyContent implements SkyboxContent {
    private final ResourceLocation id;
    private final int priority;
    private final List<Planet> planets;

    public TexturedPlanetSystemSkyContent(ResourceLocation id, int priority, List<Planet> planets) {
        this.id = id;
        this.priority = priority;
        this.planets = List.copyOf(planets);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public void render(SkyboxRenderContext context, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        var matrix = context.poseStack().last().pose();
        var tesselator = Tesselator.getInstance();
        var buffer = tesselator.getBuilder();

        for (var planet : planets) {
            var center = SkyGeometry.point(planet.yaw(), planet.pitch());

            RenderSystem.setShaderTexture(0, planet.texture());
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            SkyGeometry.addTexturedBillboard(buffer, matrix, center, planet.size(), planet.roll() + context.renderTime() * planet.rollSpeed(), planet.color(), alpha * planet.opacity());
            tesselator.end();

            for (var satellite : planet.satellites()) {
                var orbitAngle = satellite.orbitOffset() + context.renderTime() * satellite.orbitSpeed();
                var satelliteCenter = orbit(center, orbitAngle, satellite.orbitRadius());

                RenderSystem.setShaderTexture(0, satellite.texture());
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                SkyGeometry.addTexturedBillboard(buffer, matrix, satelliteCenter, satellite.size(), orbitAngle + satellite.roll(), satellite.color(), alpha * satellite.opacity());
                tesselator.end();
            }
        }
    }

    private static SkyPoint orbit(SkyPoint center, float angle, float radius) {
        var normalX = center.x() / SkyGeometry.SKY_RADIUS;
        var normalY = center.y() / SkyGeometry.SKY_RADIUS;
        var normalZ = center.z() / SkyGeometry.SKY_RADIUS;
        var tangentX = normalZ;
        var tangentY = 0F;
        var tangentZ = -normalX;
        var tangentLength = Mth.sqrt(tangentX * tangentX + tangentZ * tangentZ);

        if (tangentLength < 0.001F) {
            tangentX = 1F;
            tangentZ = 0F;
        } else {
            tangentX /= tangentLength;
            tangentZ /= tangentLength;
        }

        var bitangentX = normalY * tangentZ - normalZ * tangentY;
        var bitangentY = normalZ * tangentX - normalX * tangentZ;
        var bitangentZ = normalX * tangentY - normalY * tangentX;
        var offsetX = (tangentX * Mth.cos(angle) + bitangentX * Mth.sin(angle)) * radius;
        var offsetY = (tangentY * Mth.cos(angle) + bitangentY * Mth.sin(angle)) * radius;
        var offsetZ = (tangentZ * Mth.cos(angle) + bitangentZ * Mth.sin(angle)) * radius;
        var x = center.x() + offsetX;
        var y = center.y() + offsetY;
        var z = center.z() + offsetZ;
        var length = SkyGeometry.SKY_RADIUS / Mth.sqrt(x * x + y * y + z * z);

        return new SkyPoint(x * length, y * length, z * length);
    }

    public record Planet(ResourceLocation texture, float yaw, float pitch, float size, float roll, float rollSpeed, SkyColor color, float opacity, List<Satellite> satellites) {
    }

    public record Satellite(ResourceLocation texture, float size, float orbitRadius, float orbitSpeed, float orbitOffset, float roll, SkyColor color, float opacity) {
    }
}
