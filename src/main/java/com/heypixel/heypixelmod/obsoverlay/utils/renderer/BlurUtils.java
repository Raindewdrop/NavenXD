package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntDoubleImmutablePair;
import net.minecraft.client.Minecraft;

public class BlurUtils {
   private static Shader shaderDown;
   private static Shader shaderUp;
   private static final TimeHelper blurTimer = new TimeHelper();
   private static final Framebuffer[] fbos = new Framebuffer[6];
   private static final IntDoubleImmutablePair[] strengths = new IntDoubleImmutablePair[]{
      IntDoubleImmutablePair.of(1, 1.25),
      IntDoubleImmutablePair.of(1, 2.25),
      IntDoubleImmutablePair.of(2, 2.0),
      IntDoubleImmutablePair.of(2, 3.0),
      IntDoubleImmutablePair.of(2, 4.25),
      IntDoubleImmutablePair.of(3, 2.5),
      IntDoubleImmutablePair.of(3, 3.25),
      IntDoubleImmutablePair.of(3, 4.25),
      IntDoubleImmutablePair.of(3, 5.5),
      IntDoubleImmutablePair.of(4, 3.25),
      IntDoubleImmutablePair.of(4, 4.0),
      IntDoubleImmutablePair.of(4, 5.0),
      IntDoubleImmutablePair.of(4, 6.0),
      IntDoubleImmutablePair.of(4, 7.25),
      IntDoubleImmutablePair.of(4, 8.25),
      IntDoubleImmutablePair.of(5, 4.5),
      IntDoubleImmutablePair.of(5, 5.25),
      IntDoubleImmutablePair.of(5, 6.25),
      IntDoubleImmutablePair.of(5, 7.25),
      IntDoubleImmutablePair.of(5, 8.5)
   };

   public static void onRenderAfterWorld(EventRender2D e, float fps, int strengthIndex) {
      StencilUtils.write(false);
      Naven.getInstance().getEventManager().call(new EventShader(e.getStack(), EventType.BLUR));
      StencilUtils.erase(true);
      if (shaderDown == null) {
         shaderDown = new Shader("blur.vert", "blur_down.frag");
         shaderUp = new Shader("blur.vert", "blur_up.frag");

         for (int i = 0; i < fbos.length; i++) {
            if (fbos[i] == null) {
               fbos[i] = new Framebuffer(1.0 / Math.pow(2.0, (double)i));
            }
         }
      }

      IntDoubleImmutablePair strength = strengths[strengthIndex];
      int iterations = strength.leftInt();
      double offset = strength.rightDouble();
      if (blurTimer.delay((double)(1000.0F / fps))) {
         PostProcessRenderer.beginRender(e.getStack());
         renderToFbo(e.getStack(), fbos[0], Minecraft.getInstance().getMainRenderTarget().getColorTextureId(), shaderDown, offset);

         for (int ix = 0; ix < iterations; ix++) {
            renderToFbo(e.getStack(), fbos[ix + 1], fbos[ix].texture, shaderDown, offset);
         }

         for (int ix = iterations; ix >= 1; ix--) {
            renderToFbo(e.getStack(), fbos[ix - 1], fbos[ix].texture, shaderUp, offset);
         }

         Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
         blurTimer.reset();
         PostProcessRenderer.render(e.getStack());
         PostProcessRenderer.endRender();
      }

      RenderSystem.bindTexture(fbos[1].texture);
      shaderUp.bind();
      shaderUp.set("uTexture", 0);
      shaderUp.set("uHalfTexelSize", 0.5 / (double)fbos[1].width, 0.5 / (double)fbos[1].height);
      shaderUp.set("uOffset", offset);
      PostProcessRenderer.render(e.getStack());
      StencilUtils.dispose();
   }

   private static void renderToFbo(PoseStack stack, Framebuffer targetFbo, int sourceText, Shader shader, double offset) {
      targetFbo.bind();
      targetFbo.setViewport();
      shader.bind();
      GL.bindTexture(sourceText);
      shader.set("uTexture", 0);
      shader.set("uHalfTexelSize", 0.5 / (double)targetFbo.width, 0.5 / (double)targetFbo.height);
      shader.set("uOffset", offset);
      PostProcessRenderer.render(stack);
   }
}
