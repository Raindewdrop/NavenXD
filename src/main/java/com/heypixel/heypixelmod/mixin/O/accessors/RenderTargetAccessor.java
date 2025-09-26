package com.heypixel.heypixelmod.mixin.O.accessors;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderTarget.class})
public interface RenderTargetAccessor {
   @Accessor
   void setDepthBufferId(int var1);
}
