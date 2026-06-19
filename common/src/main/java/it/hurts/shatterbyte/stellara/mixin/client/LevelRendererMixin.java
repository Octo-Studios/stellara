package it.hurts.shatterbyte.stellara.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.hurts.shatterbyte.stellara.client.sky.StellaraSkyboxRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", ordinal = 1))
    private void stellara$suppressVanillaStars(VertexBuffer buffer, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, ShaderInstance shader, PoseStack poseStack, Matrix4f skyProjectionMatrix, float partialTick, Camera camera, boolean foggy, Runnable skyFogSetup) {
        if (!StellaraSkyboxRenderer.shouldSuppressVanillaStars(poseStack, camera, partialTick))
            buffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void stellara$renderSkybox(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean foggy, Runnable skyFogSetup, CallbackInfo callback) {
        StellaraSkyboxRenderer.render(poseStack, camera, partialTick);
    }
}
