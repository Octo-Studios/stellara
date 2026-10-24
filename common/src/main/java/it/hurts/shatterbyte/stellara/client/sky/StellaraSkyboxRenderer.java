package it.hurts.shatterbyte.stellara.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxEntry;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxRegistry;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StellaraSkyboxRenderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Map<String, Float> ALPHAS = new HashMap<>();
    private static final float SKYBOX_SWITCH_DELAY_TICKS = 60F;

    private static Object lastLevel;
    private static Candidate activeCandidate;
    private static Candidate pendingCandidate;
    private static float pendingSince;

    private StellaraSkyboxRenderer() {
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        var context = makeContext(poseStack, camera, partialTick);

        if (context == null)
            return;

        render(poseStack, context);
    }

    public static boolean shouldSuppressVanillaStars(PoseStack poseStack, Camera camera, float partialTick) {
        var context = makeContext(poseStack, camera, partialTick);

        if (context == null)
            return false;

        var entries = SkyboxRegistry.entries();
        var selectedCandidate = selectedCandidate(entries, context);

        for (var entry : entries) {
            var id = entry.content().id().toString();

            if (matchesCandidate(entry, selectedCandidate) && entry.content().targetAlpha(context) > 0.003F)
                return true;

            if (ALPHAS.getOrDefault(id, 0F) > 0.003F)
                return true;
        }

        return false;
    }

    private static void render(PoseStack poseStack, SkyboxRenderContext context) {

        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90F));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(context.skyAngle()));

        var entries = SkyboxRegistry.entries();
        var selectedCandidate = updateSelectedCandidate(entries, context);

        for (var entry : entries) {
            var id = entry.content().id().toString();
            var currentAlpha = ALPHAS.getOrDefault(id, 0F);
            var targetAlpha = matchesCandidate(entry, selectedCandidate) ? entry.content().targetAlpha(context) : 0F;
            var speed = entry.fadeSpeed() * Math.max(context.partialTick(), 0.25F);
            var nextAlpha = Mth.lerp(speed, currentAlpha, targetAlpha);

            if (nextAlpha < 0.003F)
                nextAlpha = 0F;

            ALPHAS.put(id, nextAlpha);

            if (nextAlpha > 0F)
                entry.content().render(context, nextAlpha);
        }

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static Candidate updateSelectedCandidate(List<SkyboxEntry> entries, SkyboxRenderContext context) {
        if (lastLevel != context.level()) {
            lastLevel = context.level();
            activeCandidate = null;
            pendingCandidate = null;
            pendingSince = context.renderTime();
            ALPHAS.clear();
        }

        var candidate = rawCandidate(entries, context);

        if (candidate == null) {
            activeCandidate = null;
            pendingCandidate = null;
            return null;
        }

        if (candidate.equals(activeCandidate)) {
            pendingCandidate = null;
            return activeCandidate;
        }

        if (!candidate.equals(pendingCandidate)) {
            pendingCandidate = candidate;
            pendingSince = context.renderTime();
            return activeCandidate;
        }

        if (context.renderTime() - pendingSince >= SKYBOX_SWITCH_DELAY_TICKS) {
            activeCandidate = pendingCandidate;
            pendingCandidate = null;
        }

        return activeCandidate;
    }

    private static Candidate selectedCandidate(List<SkyboxEntry> entries, SkyboxRenderContext context) {
        return activeCandidate;
    }

    private static Candidate rawCandidate(List<SkyboxEntry> entries, SkyboxRenderContext context) {
        Candidate candidate = null;

        for (var entry : entries)
            if (entry.condition().matches(context) && entry.content().targetAlpha(context) > 0.003F)
                if (candidate == null || entry.weight() > candidate.weight())
                    candidate = new Candidate(skyboxKey(entry.content().id()), entry.weight());

        return candidate;
    }

    private static boolean matchesCandidate(SkyboxEntry entry, Candidate candidate) {
        return candidate != null && entry.weight() == candidate.weight() && skyboxKey(entry.content().id()).equals(candidate.key());
    }

    private static String skyboxKey(ResourceLocation id) {
        var path = id.getPath();
        var slashIndex = path.indexOf('/');

        if (slashIndex >= 0)
            path = path.substring(0, slashIndex);

        return id.getNamespace() + ":" + path;
    }

    private static SkyboxRenderContext makeContext(PoseStack poseStack, Camera camera, float partialTick) {
        if (MC.level == null || MC.player == null || !hasSupportedSky())
            return null;

        var occlusion = getSkyOcclusion(camera);

        if (occlusion <= 0F)
            return null;

        var position = BlockPos.containing(camera.getPosition());
        var biome = MC.level.getBiome(position);
        ResourceKey<Biome> biomeKey = biome.unwrapKey().orElse(null);
        var nightFactor = getNightFactor(partialTick);
        var weatherFactor = getWeatherFactor(partialTick);
        var visibility = Mth.clamp(nightFactor * weatherFactor * occlusion, 0F, 1F);
        var renderTime = MC.level.getGameTime() + partialTick;
        var timeOfDay = MC.level.getTimeOfDay(partialTick);
        var skyAngle = timeOfDay * 360F;
        var dayTime = Math.floorMod(MC.level.getDayTime(), 24000) + partialTick;

        if (dayTime >= 24000F)
            dayTime -= 24000F;

        return new SkyboxRenderContext(MC, MC.level, camera, poseStack, partialTick, renderTime, skyAngle, dayTime, visibility, nightFactor, weatherFactor, biome, biomeKey);
    }

    private static boolean hasSupportedSky() {
        return MC.level != null;
    }

    private static float getSkyOcclusion(Camera camera) {
        var fogType = camera.getFluidInCamera();

        if (fogType == FogType.LAVA || fogType == FogType.POWDER_SNOW || fogType == FogType.WATER)
            return 0F;

        if (camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS)))
            return 0F;

        return 1F;
    }

    private static float getNightFactor(float partialTick) {
        if (MC.level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL)
            return 1F;

        return Mth.clamp(MC.level.getStarBrightness(partialTick) * 2F, 0F, 1F);
    }

    private static float getWeatherFactor(float partialTick) {
        return 1F - MC.level.getRainLevel(partialTick) * 0.65F;
    }

    private record Candidate(String key, int weight) {

    }
}
