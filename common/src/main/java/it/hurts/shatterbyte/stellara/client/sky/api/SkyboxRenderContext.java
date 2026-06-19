package it.hurts.shatterbyte.stellara.client.sky.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public record SkyboxRenderContext(Minecraft minecraft, ClientLevel level, Camera camera, PoseStack poseStack, float partialTick, float renderTime, float skyAngle, float dayTime, float visibility, float nightFactor, float weatherFactor, Holder<Biome> biome, ResourceKey<Biome> biomeKey) {
}
