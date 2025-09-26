package com.heypixel.heypixelmod.mixin.O.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.system.linux.XButtonEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({CustomButton.class})
public class CustomButton extends Button{
    private final ResourceLocation texture;
    private final Font font;

    public CustomButton(ResourceLocation texture,
                        int x, int y,
                        int width, int height,
                        Component message,
                        Button.OnPress onPress,
                        Font font) {
        super(x, y, width / 2, height, message, onPress, DEFAULT_NARRATION); // 宽度减半
        this.texture = texture;
        this.font = font;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 设置着色器
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);

        // 获取按钮状态对应的纹理Y偏移
        int textureY = getTextureY() * 20;

        // 渲染按钮背景
        guiGraphics.blit(texture,
                this.getX(), this.getY(),
                0, textureY,
                this.width, this.height,
                200, 20);

        // 渲染按钮文本
        int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
        guiGraphics.drawCenteredString(
                this.font,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor | Mth.ceil(this.alpha * 255.0F) << 24
        );
    }

    private int getTextureY() {
        if (!this.active) {
            return 0; // 禁用状态
        } else if (this.isHoveredOrFocused()) {
            return 2; // 悬停状态
        } else {
            return 1; // 正常状态
        }
    }
}