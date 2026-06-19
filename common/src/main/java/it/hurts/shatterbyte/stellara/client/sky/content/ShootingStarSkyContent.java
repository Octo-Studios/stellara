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

public class ShootingStarSkyContent implements SkyboxContent {
    private static final SkyColor CORE_COLOR = new SkyColor(0.92F, 1F, 1F);
    private static final int TRAIL_SEGMENTS = 12;
    private static final float TRAIL_LOCAL_LENGTH = 0.28F;

    private final ResourceLocation id;
    private final int priority;
    private final SkyColor color;
    private final int intervalTicks;
    private final int durationTicks;
    private final long seed;
    private final List<ShootingStarSpawner> spawners;

    public ShootingStarSkyContent(ResourceLocation id, int priority, int count, SkyColor color, long seed, int intervalTicks, int durationTicks) {
        this.id = id;
        this.priority = priority;
        this.color = color;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.durationTicks = Math.max(1, durationTicks);
        this.seed = seed;
        this.spawners = makeSpawners(count, seed, this.intervalTicks, this.durationTicks);
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
        var visibleShootingStars = visibleShootingStars(context);

        if (visibleShootingStars.isEmpty())
            return;

        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        context.poseStack().pushPose();
        context.poseStack().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-context.skyAngle()));

        var matrix = context.poseStack().last().pose();
        var tesselator = Tesselator.getInstance();
        var buffer = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (var visibleShootingStar : visibleShootingStars)
            addTrail(buffer, matrix, visibleShootingStar, alpha);

        tesselator.end();

        context.poseStack().popPose();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableCull();
    }

    private List<VisibleShootingStar> visibleShootingStars(SkyboxRenderContext context) {
        if (spawners.isEmpty())
            return List.of();

        var visibleShootingStars = new ArrayList<VisibleShootingStar>();

        for (var index = 0; index < spawners.size(); index++) {
            var spawner = spawners.get(index);
            var cycleTime = context.renderTime() + spawner.offset();
            var cycleIndex = Mth.floor(cycleTime / spawner.period());
            var localTicks = cycleTime - cycleIndex * spawner.period();

            if (localTicks > spawner.lifetime())
                continue;

            var shootingStar = makeShootingStar(spawner, seed + index * 104729L + cycleIndex * 130363L);
            var local = localTicks / spawner.lifetime();
            var fade = Mth.sin(local * Mth.PI);
            var head = point(shootingStar, local);

            visibleShootingStars.add(new VisibleShootingStar(shootingStar, local, head, shootingStar.angle(), shootingStar.size(), shootingStar.brightness(), fade));
        }

        return visibleShootingStars;
    }

    private void addTrail(com.mojang.blaze3d.vertex.BufferBuilder buffer, org.joml.Matrix4f matrix, VisibleShootingStar visibleShootingStar, float alpha) {
        var shootingStar = visibleShootingStar.shootingStar();
        var currentLocal = visibleShootingStar.local();
        var startLocal = Math.max(0F, currentLocal - TRAIL_LOCAL_LENGTH);
        var length = Math.max(0.001F, currentLocal - startLocal);
        var previousPoint = point(shootingStar, currentLocal);

        for (var segment = 1; segment <= TRAIL_SEGMENTS; segment++) {
            var trailProgress = segment / (float) TRAIL_SEGMENTS;
            var sampleLocal = currentLocal - length * trailProgress;
            var point = point(shootingStar, sampleLocal);
            var age = trailProgress;
            var build = Mth.clamp(currentLocal / TRAIL_LOCAL_LENGTH, 0F, 1F);
            var taper = 1F - age;
            var width = (0.035F + taper * taper * 0.34F) * visibleShootingStar.size() * build;
            var segmentAlpha = alpha * visibleShootingStar.fade() * visibleShootingStar.brightness() * taper * taper * build;

            if (segmentAlpha > 0.002F) {
                SkyGeometry.addLine(buffer, matrix, previousPoint, point, width * 1.45F, color, segmentAlpha * 0.42F);
                SkyGeometry.addLine(buffer, matrix, previousPoint, point, width * 0.55F, CORE_COLOR, segmentAlpha * 0.86F);
            }

            previousPoint = point;
        }
    }

    private static SkyPoint point(ShootingStar shootingStar, float local) {
        var origin = SkyGeometry.point(shootingStar.yaw(), shootingStar.pitch());
        var unitX = origin.x() / SkyGeometry.SKY_RADIUS;
        var unitY = origin.y() / SkyGeometry.SKY_RADIUS;
        var unitZ = origin.z() / SkyGeometry.SKY_RADIUS;
        var sideX = Mth.cos(shootingStar.yaw());
        var sideZ = -Mth.sin(shootingStar.yaw());
        var downX = Mth.sin(shootingStar.yaw()) * Mth.sin(shootingStar.pitch());
        var downY = -Mth.cos(shootingStar.pitch());
        var downZ = Mth.cos(shootingStar.yaw()) * Mth.sin(shootingStar.pitch());
        var curve = shootingStar.curve() * local * local;
        var x = unitX + sideX * shootingStar.length() * local + downX * (shootingStar.drop() * local + curve);
        var y = unitY + downY * (shootingStar.drop() * local + curve);
        var z = unitZ + sideZ * shootingStar.length() * local + downZ * (shootingStar.drop() * local + curve);
        var scale = SkyGeometry.SKY_RADIUS / Mth.sqrt(x * x + y * y + z * z);

        return new SkyPoint(x * scale, y * scale, z * scale);
    }

    private static List<ShootingStarSpawner> makeSpawners(int count, long seed, int intervalTicks, int durationTicks) {
        var random = new LegacyRandomSource(seed);
        var spawners = new ArrayList<ShootingStarSpawner>();

        for (var i = 0; i < count; i++)
            spawners.add(new ShootingStarSpawner(
                    durationTicks * (0.45F + random.nextFloat() * 0.55F),
                    intervalTicks * (0.65F + random.nextFloat() * 0.9F),
                    random.nextFloat() * intervalTicks
            ));

        return spawners;
    }

    private static ShootingStar makeShootingStar(ShootingStarSpawner spawner, long seed) {
        var random = new LegacyRandomSource(seed);

        return new ShootingStar(
                random.nextFloat() * Mth.TWO_PI,
                0.72F + random.nextFloat() * 0.48F,
                0.98F + random.nextFloat() * 0.5F,
                0.56F + random.nextFloat() * 0.32F,
                random.nextFloat() * Mth.TWO_PI,
                0.55F + random.nextFloat() * 0.35F,
                0.82F + random.nextFloat() * 0.45F,
                spawner.lifetime(),
                spawner.period(),
                spawner.offset(),
                0.16F + random.nextFloat() * 0.16F
        );
    }

    private record ShootingStar(float yaw, float pitch, float length, float drop, float angle, float size, float brightness, float lifetime, float period, float offset, float curve) {
    }

    private record ShootingStarSpawner(float lifetime, float period, float offset) {
    }

    private record VisibleShootingStar(ShootingStar shootingStar, float local, SkyPoint head, float angle, float size, float brightness, float fade) {
    }
}
