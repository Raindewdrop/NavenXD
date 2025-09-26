package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

public class ShadowUtils {
   private static final TimeHelper shadowTimer = new TimeHelper();
   private static Framebuffer fbo1;
   private static Framebuffer fbo2;
   private static Framebuffer render;
   private static Shader shader;

   public static void onRenderAfterWorld(EventRender2D e, float fps) {
      Window window = Minecraft.getInstance().getWindow();
      if (shader == null) {
         shader = new Shader("shadow.vert", "shadow.frag");
         fbo1 = new Framebuffer();
         fbo2 = new Framebuffer();
         render = new Framebuffer();
      }

      boolean shouldFresh = false;
      if (shadowTimer.delay((double)(1000.0F / fps))) {
         shouldFresh = true;
         shadowTimer.reset();
      }

      if (shouldFresh) {
         render.resize();
         fbo1.resize();
         fbo2.resize();
         render.bind();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         Naven.getInstance().getEventManager().call(new EventShader(e.getStack(), EventType.SHADOW));
         render.unbind();
      }

      GL.enableBlend();
      shader.bind();
      shader.set("u_Size", (double)window.getWidth(), (double)window.getHeight());
      PostProcessRenderer.beginRender(e.getStack());
      if (shouldFresh) {
         fbo1.bind();
         GL.bindTexture(render.texture);
         shader.set("u_Direction", 1.0, 0.0);
         PostProcessRenderer.render(e.getStack());
         fbo2.bind();
         GL.bindTexture(fbo1.texture);
         shader.set("u_Direction", 0.0, 1.0);
         PostProcessRenderer.render(e.getStack());
         fbo1.bind();
         GL.bindTexture(fbo2.texture);
         shader.set("u_Direction", 1.0, 0.0);
         PostProcessRenderer.render(e.getStack());
         fbo2.unbind();
      }

      GL.bindTexture(fbo1.texture);
      shader.set("u_Direction", 0.0, 1.0);
      PostProcessRenderer.render(e.getStack());
      GL.disableBlend();
      PostProcessRenderer.endRender();
   }
}
