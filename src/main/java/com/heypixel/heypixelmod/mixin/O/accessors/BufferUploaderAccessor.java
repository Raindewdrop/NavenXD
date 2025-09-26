package com.heypixel.heypixelmod.mixin.O.accessors;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BufferUploader.class})
public interface BufferUploaderAccessor {
   @Accessor("lastImmediateBuffer")
   static void setCurrentVertexBuffer(VertexBuffer vertexBuffer) {
   }
}
