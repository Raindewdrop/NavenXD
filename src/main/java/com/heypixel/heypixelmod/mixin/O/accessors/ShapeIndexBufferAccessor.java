package com.heypixel.heypixelmod.mixin.O.accessors;

import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AutoStorageIndexBuffer.class})
public interface ShapeIndexBufferAccessor {
   @Accessor("name")
   int getId();
}
