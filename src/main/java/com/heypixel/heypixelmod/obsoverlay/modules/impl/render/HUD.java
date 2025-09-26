package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.ValueManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;

@ModuleInfo(
   name = "HUD",
   description = "Displays information on your screen",
   category = Category.RENDER
)
public class HUD extends Module {
   public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
   public static final int bodyColor = new Color(21, 21, 23, 200).getRGB();
   public static final int backgroundColor = new Color(0, 0, 0, 60).getRGB(); // 加深背景，减少透明度
   private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
   public BooleanValue waterMark = ValueBuilder.create(this, "Water Mark").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue watermarkSize = ValueBuilder.create(this, "Watermark Size")
      .setVisibility(this.waterMark::getCurrentValue)
      .setDefaultFloatValue(0.4F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
   public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue notification = ValueBuilder.create(this, "Notification").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue arrayList = ValueBuilder.create(this, "").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue prettyModuleName = ValueBuilder.create(this, "Pretty Module Name")
      .setOnUpdate(value -> Module.update = true)
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   public BooleanValue hideRenderModules = ValueBuilder.create(this, "Hide Render Modules")
      .setOnUpdate(value -> Module.update = true)
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   public BooleanValue rainbow = ValueBuilder.create(this, "Rainbow")
      .setDefaultBooleanValue(true)
      .setVisibility(this.arrayList::getCurrentValue)
      .build()
      .getBooleanValue();
   public ModeValue rainbowMode = ValueBuilder.create(this, "Rainbow Mode")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue())
      .setDefaultModeIndex(0)
      .setModes("Gradient", "Solid", "Two Colors", "Three Colors")
      .build()
      .getModeValue();
   public FloatValue rainbowSpeed = ValueBuilder.create(this, "Rainbow Speed")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Gradient"))
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue rainbowOffset = ValueBuilder.create(this, "Rainbow Offset")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Gradient"))
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue solidColorRed = ValueBuilder.create(this, "Solid Color Red")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Solid"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue solidColorGreen = ValueBuilder.create(this, "Solid Color Green")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Solid"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue solidColorBlue = ValueBuilder.create(this, "Solid Color Blue")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Solid"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue twoColorsFirstRed = ValueBuilder.create(this, "Two Colors First Red")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Two Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(14.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue twoColorsFirstGreen = ValueBuilder.create(this, "Two Colors First Green")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Two Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(190.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue twoColorsFirstBlue = ValueBuilder.create(this, "Two Colors First Blue")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Two Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue twoColorsSecondRed = ValueBuilder.create(this, "Two Colors Second Red")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Two Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue twoColorsSecondGreen = ValueBuilder.create(this, "Two Colors Second Green")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Two Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(66.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue twoColorsSecondBlue = ValueBuilder.create(this, "Two Colors Second Blue")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Two Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(179.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsFirstRed = ValueBuilder.create(this, "Three Colors First Red")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsFirstGreen = ValueBuilder.create(this, "Three Colors First Green")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsFirstBlue = ValueBuilder.create(this, "Three Colors First Blue")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsSecondRed = ValueBuilder.create(this, "Three Colors Second Red")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsSecondGreen = ValueBuilder.create(this, "Three Colors Second Green")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsSecondBlue = ValueBuilder.create(this, "Three Colors Second Blue")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsThirdRed = ValueBuilder.create(this, "Three Colors Third Red")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsThirdGreen = ValueBuilder.create(this, "Three Colors Third Green")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue threeColorsThirdBlue = ValueBuilder.create(this, "Three Colors Third Blue")
      .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue() && this.rainbowMode.isCurrentMode("Three Colors"))
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(255.0F)
      .setDefaultFloatValue(255.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public ModeValue arrayListDirection = ValueBuilder.create(this, "ArrayList Direction")
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultModeIndex(0)
      .setModes("Right", "Left")
      .build()
      .getModeValue();
   public FloatValue xOffset = ValueBuilder.create(this, "X Offset")
      .setVisibility(this.arrayList::getCurrentValue)
      .setMinFloatValue(-100.0F)
      .setMaxFloatValue(100.0F)
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue yOffset = ValueBuilder.create(this, "Y Offset")
      .setVisibility(this.arrayList::getCurrentValue)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(100.0F)
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue arrayListSize = ValueBuilder.create(this, "ArrayList Size")
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultFloatValue(0.4F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
   List<Module> renderModules;
   float width;
   float watermarkHeight;
   List<Vector4f> blurMatrices = new ArrayList<>();
   List<Vector4f> watermarkBlurMatrices = new ArrayList<>();
   List<Vector4f> notificationBlurMatrices = new ArrayList<>();

   public String getModuleDisplayName(Module module) {
      String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
      

      String modeSuffix = "";
      
      return name + modeSuffix + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
   }

   @EventTarget
   public void notification(EventRender2D e) {
      if (this.notification.getCurrentValue()) {
         Naven.getInstance().getNotificationManager().onRender(e);
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (this.notification.getCurrentValue() && e.getType() == EventType.SHADOW) {
         Naven.getInstance().getNotificationManager().onRenderShadow(e);
      }

      if (this.waterMark.getCurrentValue()) {
         RenderUtils.drawRoundedRect(e.getStack(), 5.0F, 5.0F, this.width, this.watermarkHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
         for (Vector4f blurMatrix : this.watermarkBlurMatrices) {
            RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
         }
      }

      if (this.arrayList.getCurrentValue()) {
         for (Vector4f blurMatrix : this.blurMatrices) {
            RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
         }
      }
      
      if (this.notification.getCurrentValue()) {
         for (Vector4f blurMatrix : this.notificationBlurMatrices) {
            RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      CustomTextRenderer font = Fonts.opensans;
      if (this.waterMark.getCurrentValue()) {
         e.getStack().pushPose();
         String text = "NavenXD | " + Version.getVersion() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());
         this.width = font.getWidth(text, (double)this.watermarkSize.getCurrentValue()) + 14.0F;
         this.watermarkHeight = (float)font.getHeight(true, (double)this.watermarkSize.getCurrentValue());
         
         // 计算居中位置
         float screenWidth = (float)mc.getWindow().getGuiScaledWidth();
         float centeredX = (screenWidth - this.width) / 2.0F;
         
         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(e.getStack(), centeredX, 5.0F, this.width, this.watermarkHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
         StencilUtils.erase(true);
         RenderUtils.fill(e.getStack(), centeredX, 5.0F, centeredX + 4.0F + this.width, 8.0F, headerColor);
         RenderUtils.fill(e.getStack(), centeredX, 8.0F, centeredX + 4.0F + this.width, 16.0F + this.watermarkHeight, bodyColor);
         font.render(e.getStack(), text, centeredX + 7.0, 10.0, Color.WHITE, true, (double)this.watermarkSize.getCurrentValue());
         StencilUtils.dispose();
         e.getStack().popPose();
      }

      this.blurMatrices.clear();
      this.watermarkBlurMatrices.clear();
      this.notificationBlurMatrices.clear();
      
      if (this.waterMark.getCurrentValue()) {
         e.getStack().pushPose();
         String text = "NavenXD | " + Version.getVersion() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());
         this.width = font.getWidth(text, (double)this.watermarkSize.getCurrentValue()) + 14.0F;
         this.watermarkHeight = (float)font.getHeight(true, (double)this.watermarkSize.getCurrentValue());
         
         // 计算居中位置
         float screenWidth = (float)mc.getWindow().getGuiScaledWidth();
         float centeredX = (screenWidth - this.width) / 2.0F;
         
         this.watermarkBlurMatrices.add(new Vector4f(centeredX, 5.0F, this.width, this.watermarkHeight + 8.0F));
         e.getStack().popPose();
      }
      
      if (this.notification.getCurrentValue()) {
         Naven.getInstance().getNotificationManager().collectBlurMatrices(this.notificationBlurMatrices);
      }
      
      if (this.arrayList.getCurrentValue()) {
         e.getStack().pushPose();
         ModuleManager moduleManager = Naven.getInstance().getModuleManager();
         if (update || this.renderModules == null) {
            this.renderModules = new ArrayList<>(moduleManager.getModules());
            if (this.hideRenderModules.getCurrentValue()) {
               this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
            }
            this.renderModules.removeIf(modulex -> modulex.getHideInArrayList().getCurrentValue());

            this.renderModules.sort((o1, o2) -> {
               float o1Width = font.getWidth(this.getModuleDisplayName(o1), (double)this.arrayListSize.getCurrentValue());
               float o2Width = font.getWidth(this.getModuleDisplayName(o2), (double)this.arrayListSize.getCurrentValue());
               return Float.compare(o2Width, o1Width);
            });
         }

         float maxWidth = this.renderModules.isEmpty()
            ? 0.0F
            : font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), (double)this.arrayListSize.getCurrentValue());
         float arrayListX = this.arrayListDirection.isCurrentMode("Right")
            ? (float)mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F + this.xOffset.getCurrentValue()
            : 3.0F + this.xOffset.getCurrentValue();
         float arrayListY = this.yOffset.getCurrentValue();
         float height = 0.0F;
         double fontHeight = font.getHeight(true, (double)this.arrayListSize.getCurrentValue());

         for (Module module : this.renderModules) {
            SmoothAnimationTimer animation = module.getAnimation();
            if (module.isEnabled()) {
               animation.target = 100.0F;
            } else {
               animation.target = 0.0F;
            }

            animation.update(true);
            if (animation.value > 0.0F) {
               String displayName = this.getModuleDisplayName(module);
               float stringWidth = font.getWidth(displayName, (double)this.arrayListSize.getCurrentValue());
               float left = -stringWidth * (1.0F - animation.value / 100.0F);
               float right = maxWidth - stringWidth * (animation.value / 100.0F);
               float innerX = this.arrayListDirection.isCurrentMode("Left") ? left : right;
               RenderUtils.drawRoundedRect(
                  e.getStack(),
                  arrayListX + innerX,
                  arrayListY + height + 2.0F,
                  stringWidth + 5.0F, // 增加2像素宽度以包含竖线
                  (float)((double)(animation.value / 100.0F) * fontHeight),
                  5.0F, // 圆角半径
                  backgroundColor
               );
               this.blurMatrices
                  .add(
                     new Vector4f(arrayListX + innerX, arrayListY + height + 2.0F, stringWidth + 5.0F, (float)((double)(animation.value / 100.0F) * fontHeight))
                  );
               int color = -1;
               if (this.rainbow.getCurrentValue()) {
                  String mode = this.rainbowMode.getCurrentMode();
                  float speed = (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F;
                  int index = (int)(-height * this.rainbowOffset.getCurrentValue());
                  
                  switch (mode) {
                     case "Gradient":
                        color = RenderUtils.getCustomGradientOpaque(index, speed);
                        break;
                     case "Solid":
                        color = RenderUtils.getSolidColor(
                           (int)this.solidColorRed.getCurrentValue(),
                           (int)this.solidColorGreen.getCurrentValue(),
                           (int)this.solidColorBlue.getCurrentValue()
                        );
                        break;
                     case "Two Colors":
                        Color color1 = new Color(
                           (int)this.twoColorsFirstRed.getCurrentValue(),
                           (int)this.twoColorsFirstGreen.getCurrentValue(),
                           (int)this.twoColorsFirstBlue.getCurrentValue()
                        );
                        Color color2 = new Color(
                           (int)this.twoColorsSecondRed.getCurrentValue(),
                           (int)this.twoColorsSecondGreen.getCurrentValue(),
                           (int)this.twoColorsSecondBlue.getCurrentValue()
                        );
                        color = RenderUtils.getTwoColorsGradient(index, speed, color1, color2);
                        break;
                     case "Three Colors":
                        Color color1_3 = new Color(
                           (int)this.threeColorsFirstRed.getCurrentValue(),
                           (int)this.threeColorsFirstGreen.getCurrentValue(),
                           (int)this.threeColorsFirstBlue.getCurrentValue()
                        );
                        Color color2_3 = new Color(
                           (int)this.threeColorsSecondRed.getCurrentValue(),
                           (int)this.threeColorsSecondGreen.getCurrentValue(),
                           (int)this.threeColorsSecondBlue.getCurrentValue()
                        );
                        Color color3_3 = new Color(
                           (int)this.threeColorsThirdRed.getCurrentValue(),
                           (int)this.threeColorsThirdGreen.getCurrentValue(),
                           (int)this.threeColorsThirdBlue.getCurrentValue()
                        );
                        color = RenderUtils.getThreeColorsGradient(index, speed, color1_3, color2_3, color3_3);
                        break;
                     default:
                        color = RenderUtils.getCustomGradientOpaque(index, speed);
                        break;
                  }
               }

               float alpha = animation.value / 100.0F;
               font.setAlpha(alpha);
               font.render(
                  e.getStack(),
                  displayName,
                  (double)(arrayListX + innerX + 1.5F),
                  (double)(arrayListY + height + 1.0F),
                  new Color(color),
                  true,
                  (double)this.arrayListSize.getCurrentValue()
               );
               
               // 在右侧添加竖线 - 修改为更粗、更短、有圆角的竖线
               float lineWidth = 2.0F; // 更粗的竖线
               float lineX = arrayListX + innerX + stringWidth + 5.0F - lineWidth; // 调整X坐标以匹配新的背景宽度
               float lineY = arrayListY + height + 2.0F + 2.0F; // 从顶部向下偏移2像素
               float lineHeight = (float)((double)(animation.value / 100.0F) * fontHeight) - 4.0F; // 更短的竖线
               
               // 绘制圆角竖线
               if (lineHeight > 0) {
                  RenderUtils.drawRoundedRect(e.getStack(), lineX, lineY, lineWidth, lineHeight, 1.0F, color);
               }
               
               height += (float)((double)(animation.value / 100.0F) * fontHeight);
               // 添加模块之间的空隙
               height += 2.0F;
            }
         }

         font.setAlpha(1.0F);
         e.getStack().popPose();
      }
   }
}
