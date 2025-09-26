package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.mixin.O.accessors.ShapeIndexBufferAccessor;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.GL;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder.DrawState;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({VertexBuffer.class})
public abstract class MixinVertexBuffer {
   @Shadow
   private int indexBufferId;

   @Inject(
      method = {"uploadIndexBuffer"},
      at = {@At("RETURN")}
   )
   private void onConfigureIndexBuffer(DrawState arg, ByteBuffer byteBuffer, CallbackInfoReturnable<AutoStorageIndexBuffer> info) {
      if (info.getReturnValue() == null) {
         GL.CURRENT_IBO = this.indexBufferId;
      } else {
         GL.CURRENT_IBO = ((ShapeIndexBufferAccessor)(Object)info.getReturnValue()).getId();
      }
   }
}
