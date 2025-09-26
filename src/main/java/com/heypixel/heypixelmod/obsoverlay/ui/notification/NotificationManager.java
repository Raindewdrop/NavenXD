package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

public class NotificationManager {
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public void addNotification(Notification notification) {
      if (!this.notifications.contains(notification)) {
         this.notifications.add(notification);
      }
   }

   public void onRenderShadow(EventShader e) {
      for (Notification notification : this.notifications) {
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         Window window = Minecraft.getInstance().getWindow();
         notification.renderShader(
            e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F, (float)window.getGuiScaledHeight() - heightTimer.value
         );
      }
   }

   public void onRender(EventRender2D e) {
      float height = 5.0F;

      for (Notification notification : this.notifications) {
         e.getStack().pushPose();
         float width = notification.getWidth();
         height += notification.getHeight();
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
         if (lifeTime > (float)notification.getMaxAge()) {
            widthTimer.target = 0.0F;
            heightTimer.target = 0.0F;
            if (widthTimer.isAnimationDone(true)) {
               this.notifications.remove(notification);
            }
         } else {
            widthTimer.target = width;
            heightTimer.target = height;
         }

         widthTimer.update(true);
         heightTimer.update(true);
         Window window = Minecraft.getInstance().getWindow();
         notification.render(e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F, (float)window.getGuiScaledHeight() - heightTimer.value);
         e.getStack().popPose();
      }
   }
   
   public void collectBlurMatrices(List<Vector4f> blurMatrices) {
      float height = 5.0F;
      
      for (Notification notification : this.notifications) {
         float width = notification.getWidth();
         height += notification.getHeight();
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
         if (lifeTime > (float)notification.getMaxAge()) {
            widthTimer.target = 0.0F;
            heightTimer.target = 0.0F;
         } else {
            widthTimer.target = width;
            heightTimer.target = height;
         }
         
         widthTimer.update(true);
         heightTimer.update(true);
         Window window = Minecraft.getInstance().getWindow();
         float x = (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F;
         float y = (float)window.getGuiScaledHeight() - heightTimer.value;
         blurMatrices.add(new Vector4f(x + 2.0F, y + 4.0F, width, 20.0F));
      }
   }
}
