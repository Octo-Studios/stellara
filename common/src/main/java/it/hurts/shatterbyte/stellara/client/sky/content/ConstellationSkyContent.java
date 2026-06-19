package it.hurts.shatterbyte.stellara.client.sky.content;

import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import java.util.ArrayList;
import java.util.List;

public class ConstellationSkyContent implements SkyboxContent {
    private static final float STAR_CORE = 0.7F;
    private static final float STAR_HALO = 1.5F;

    private final ResourceLocation id;
    private final int priority;
    private final SkyColor color;
    private final SkyColor coreColor;
    private final float twinkleSpeed;
    private final float lineWidth;
    private final List<ResourceLocation> textures;
    private final List<Star> stars;
    private final List<Link> links;

    public ConstellationSkyContent(ResourceLocation id, int priority, SkyColor color, float centerYaw, float centerPitch, float scale, float twinkleSpeed, float lineWidth, List<ResourceLocation> textures, Shape shape, long seed) {
        this.id = id;
        this.priority = priority;
        this.color = color;
        this.coreColor = brighten(color, 0.55F);
        this.twinkleSpeed = twinkleSpeed;
        this.lineWidth = lineWidth;
        this.textures = List.copyOf(textures);
        this.links = shape.links();
        this.stars = makeStars(shape, centerYaw, centerPitch, scale, textures.size(), seed);
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
        var matrix = context.poseStack().last().pose();
        var tesselator = Tesselator.getInstance();
        var buffer = tesselator.getBuilder();
        var time = context.renderTime();
        var pulse = 0.7F + 0.3F * Mth.sin(time * twinkleSpeed * 0.5F);

        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (var link : links)
            SkyGeometry.addLine(buffer, matrix, stars.get(link.from()).point(), stars.get(link.to()).point(), lineWidth, color, alpha * pulse * 0.5F);

        tesselator.end();

        if (textures.isEmpty())
            renderGlowStars(matrix, buffer, tesselator, time, alpha);
        else
            renderTexturedStars(matrix, buffer, tesselator, time, alpha);

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableCull();
    }

    private void renderTexturedStars(org.joml.Matrix4f matrix, com.mojang.blaze3d.vertex.BufferBuilder buffer, Tesselator tesselator, float time, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        for (var textureIndex = 0; textureIndex < textures.size(); textureIndex++) {
            RenderSystem.setShaderTexture(0, textures.get(textureIndex));
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (var star : stars) {
                if (star.textureIndex() != textureIndex)
                    continue;

                var twinkle = 0.78F + 0.22F * Mth.sin(time * twinkleSpeed + star.phase());
                var angle = star.baseAngle() + time * star.spinSpeed();

                SkyGeometry.addTexturedBillboard(buffer, matrix, star.point(), star.size() * STAR_HALO, angle, color, alpha * twinkle * 0.5F);
                SkyGeometry.addTexturedBillboard(buffer, matrix, star.point(), star.size() * STAR_CORE, angle, coreColor, alpha * twinkle);
            }

            tesselator.end();
        }
    }

    private void renderGlowStars(org.joml.Matrix4f matrix, com.mojang.blaze3d.vertex.BufferBuilder buffer, Tesselator tesselator, float time, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (var star : stars) {
            var twinkle = 0.6F + 0.4F * Mth.sin(time * twinkleSpeed + star.phase());
            var angle = star.baseAngle() + time * star.spinSpeed();

            SkyGeometry.addBillboard(buffer, matrix, star.point(), star.size() * 1.7F, angle, color, alpha * twinkle * 0.32F);
            SkyGeometry.addBillboard(buffer, matrix, star.point(), star.size() * 0.85F, angle, coreColor, alpha * twinkle);
        }

        tesselator.end();
    }

    private static List<Star> makeStars(Shape shape, float centerYaw, float centerPitch, float scale, int textureCount, long seed) {
        var random = new LegacyRandomSource(seed);
        var stars = new ArrayList<Star>();
        var nodes = shape.nodes();

        for (var index = 0; index < nodes.size(); index++) {
            var node = nodes.get(index);
            var point = SkyGeometry.point(centerYaw + node.u() * scale, centerPitch + node.v() * scale);
            var phase = index * 1.7F + (Math.floorMod(seed, 628L) / 100F);
            var textureIndex = textureCount > 0 ? random.nextInt(textureCount) : 0;
            var baseAngle = random.nextFloat() * Mth.TWO_PI;
            var spinMagnitude = 0.008F + random.nextFloat() * 0.014F;
            var spinSpeed = random.nextBoolean() ? spinMagnitude : -spinMagnitude;

            stars.add(new Star(point, node.size(), phase, textureIndex, baseAngle, spinSpeed));
        }

        return stars;
    }

    private static SkyColor brighten(SkyColor color, float amount) {
        return new SkyColor(
                Mth.lerp(amount, color.red(), 1F),
                Mth.lerp(amount, color.green(), 1F),
                Mth.lerp(amount, color.blue(), 1F)
        );
    }

    private record Star(SkyPoint point, float size, float phase, int textureIndex, float baseAngle, float spinSpeed) {
    }

    public record Node(float u, float v, float size) {
    }

    public record Link(int from, int to) {
    }

    public record Shape(List<Node> nodes, List<Link> links) {
    }
}
