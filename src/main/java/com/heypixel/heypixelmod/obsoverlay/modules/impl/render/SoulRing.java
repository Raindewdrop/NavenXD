package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.awt.Color;

@ModuleInfo(
    name = "SoulRing",
    description = "Render a soul ring under player's feet",
    category = Category.RENDER
)
public class SoulRing extends Module {
    private final ModeValue imageValue = ValueBuilder.create(this, "Image")
            .setModes("A", "B", "C", "D", "E", "F")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private final FloatValue sizeValue = ValueBuilder.create(this, "Size")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    private final FloatValue heightValue = ValueBuilder.create(this, "Height")
            .setDefaultFloatValue(0.0F)
            .setFloatStep(0.05F)
            .setMinFloatValue(-0.5F)
            .setMaxFloatValue(0.5F)
            .build()
            .getFloatValue();

    private final FloatValue autoRotateSpeedValue = ValueBuilder.create(this, "AutoRotateSpeed")
            .setDefaultFloatValue(0.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    private final FloatValue opacityValue = ValueBuilder.create(this, "Opacity")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();

    private long lastUpdateTime = 0;
    private float currentRotation = 0.0F;

    public SoulRing() {
        this.setEnabled(false);
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (Minecraft.getInstance().player == null) return;
        Player player = Minecraft.getInstance().player;
        PoseStack poseStack = event.getPMatrixStack();
        float partialTicks = event.getRenderPartialTicks();
        double playerX = player.xOld + (player.getX() - player.xOld) * partialTicks;
        double playerY = player.yOld + (player.getY() - player.yOld) * partialTicks + heightValue.getCurrentValue();
        double playerZ = player.zOld + (player.getZ() - player.zOld) * partialTicks;
        Vec3 cameraPos = RenderUtils.getCameraPos();
        playerX -= cameraPos.x;
        playerY -= cameraPos.y;
        playerZ -= cameraPos.z;
        
        // 更新旋转
        updateRotation();
        renderSoulRing(poseStack, playerX, playerY, playerZ);
    }

    private void updateRotation() {
        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            return;
        }
        
        float autoRotateSpeed = autoRotateSpeedValue.getCurrentValue();
        if (autoRotateSpeed > 0) {
            long deltaTime = currentTime - lastUpdateTime;
            currentRotation += (deltaTime * autoRotateSpeed / 1000.0F) % 360.0F;
            if (currentRotation >= 360.0F) {
                currentRotation -= 360.0F;
            }
        }
        lastUpdateTime = currentTime;
    }

    private void renderSoulRing(PoseStack poseStack, double x, double y, double z) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        
        // 使用计算出的旋转角度
        poseStack.mulPose(Axis.ZP.rotationDegrees(currentRotation));
        
        float size = sizeValue.getCurrentValue();
        poseStack.scale(size, size, size);
        String imageName = imageValue.getCurrentMode();
        ResourceLocation texture = new ResourceLocation("circle", imageName + ".png");
        RenderSystem.setShaderTexture(0, texture);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glDisable(2884);
        Matrix4f matrix = poseStack.last().pose();
        float opacity = opacityValue.getCurrentValue();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, -0.5F, -0.5F, 0.0F).uv(0.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix, 0.5F, -0.5F, 0.0F).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix, 0.5F, 0.5F, 0.0F).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix, -0.5F, 0.5F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glEnable(2884);
        poseStack.popPose();
    }

    @Override
    public String getSuffix() {
        return imageValue.getCurrentMode();
    }
}