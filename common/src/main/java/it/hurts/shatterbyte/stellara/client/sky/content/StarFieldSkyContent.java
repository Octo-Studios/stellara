package it.hurts.shatterbyte.stellara.client.sky.content;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
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

public class StarFieldSkyContent implements SkyboxContent {
    private final ResourceLocation id;
    private final int priority;
    private final List<ResourceLocation> textures;
    private final SkyColor color;
    private final List<Star> stars;

    public StarFieldSkyContent(ResourceLocation id, int priority, List<ResourceLocation> textures, int count, float minSize, float maxSize, SkyColor color, long seed) {
        this.id = id;
        this.priority = priority;
        this.textures = List.copyOf(textures);
        this.color = color;
        this.stars = makeStars(count, minSize, maxSize, textures.size(), seed);
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

        for (var textureIndex = 0; textureIndex < textures.size(); textureIndex++) {
            RenderSystem.setShaderTexture(0, textures.get(textureIndex));
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (var star : stars) {
                if (star.textureIndex() != textureIndex)
                    continue;

                var pulse = 0.78F + 0.22F * Mth.sin(context.renderTime() * star.pulseSpeed() + star.pulseOffset());

                SkyGeometry.addTexturedBillboard(buffer, matrix, star.point(), star.size() * pulse, star.angle() + context.renderTime() * star.spinSpeed(), color, alpha * pulse);
            }

            var mesh = buffer.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
        }
    }

    private static List<Star> makeStars(int count, float minSize, float maxSize, int textureCount, long seed) {
        var random = new LegacyRandomSource(seed);
        var clusters = StarClusters.make(count, random);
        var stars = new ArrayList<Star>();

        while (stars.size() < count) {
            var point = clusters.sample(random);

            if (point == null)
                continue;

            stars.add(new Star(
                    point,
                    minSize + random.nextFloat() * (maxSize - minSize),
                    random.nextFloat() * Mth.TWO_PI,
                    random.nextFloat() * Mth.TWO_PI,
                    0.025F + random.nextFloat() * 0.055F,
                    -0.014F + random.nextFloat() * 0.028F,
                    random.nextInt(Math.max(1, textureCount))
            ));
        }

        return stars;
    }

    private record Star(SkyPoint point, float size, float angle, float pulseOffset, float pulseSpeed, float spinSpeed, int textureIndex) {
    }
}
