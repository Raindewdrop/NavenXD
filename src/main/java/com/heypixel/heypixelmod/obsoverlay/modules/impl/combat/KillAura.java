package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Animations;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "KillAura",
   description = "Automatically attacks entities",
   category = Category.COMBAT
)
public class KillAura extends Module {

   private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
   private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
   public static Entity target;
   public static Entity aimingTarget;
   public static List<Entity> targets = new ArrayList<>();
   public static Vector2f rotation;
   BooleanValue targetHud = ValueBuilder.create(this, "Target HUD").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue preferBaby = ValueBuilder.create(this, "Prefer Baby").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue moreParticles = ValueBuilder.create(this, "More Particles").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue pvp19 = ValueBuilder.create(this, "1.9+ PvP").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue fluctuateCps = ValueBuilder.create(this, "Fluctuate CPS").setDefaultBooleanValue(false).build().getBooleanValue();

   FloatValue attackRange = ValueBuilder.create(this, "Attack Range")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(6.0F)
      .build()
      .getFloatValue();
   FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(6.0F)
      .build()
      .getFloatValue();
   FloatValue aps = ValueBuilder.create(this, "CPS")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setVisibility(() -> !pvp19.getCurrentValue()) // 当1.9+PvP关闭时显示
      .build()
      .getFloatValue();
   FloatValue minCps = ValueBuilder.create(this, "Min CPS")
      .setDefaultFloatValue(8.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue() && !pvp19.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue maxCps = ValueBuilder.create(this, "Max CPS")
      .setDefaultFloatValue(12.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue() && !pvp19.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue fluctuateAmount = ValueBuilder.create(this, "Fluctuate Amount")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue() && !pvp19.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue rampUpTime = ValueBuilder.create(this, "Ramp Up Time (s)")
      .setDefaultFloatValue(2.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(10.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue() && !pvp19.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> !this.infSwitch.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
      .setDefaultFloatValue(360.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(360.0F)
      .build()
      .getFloatValue();
   FloatValue rotationSpeed = ValueBuilder.create(this, "Rotation Speed")
      .setDefaultFloatValue(180.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(360.0F)
      .build()
      .getFloatValue();
   FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();
   RotationUtils.Data lastRotationData;
   RotationUtils.Data rotationData;
   Vector2f currentRotation;
   int attackTimes = 0;
   float attacks = 0.0F;
   private int index;
   private Vector4f blurMatrix;
   private float cooldownProgress = 0.0F;
   
   // CPS浮动相关变量
   private float currentCps = 0.0F;
   private long lastFluctuationTime = 0L;
   private boolean increasing = true;
   private Random random = new Random();
   private long rampUpStartTime = 0L;
   private boolean isRampingUp = false;

   @EventTarget
   public void onShader(EventShader e) {
      if (this.blurMatrix != null && this.targetHud.getCurrentValue()) {
         RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      this.blurMatrix = null;
      if (target instanceof LivingEntity && this.targetHud.getCurrentValue()) {
         LivingEntity living = (LivingEntity)target;
         e.getStack().pushPose();
         float x = (float)mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
         float y = (float)mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
         String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
         float nameWidth = Fonts.harmony.getWidth(targetName, 0.4F);
         float hpWidth = Fonts.harmony.getWidth("HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""), 0.35F);
         float textWidth = Math.max(nameWidth, hpWidth);
         
         boolean isPlayer = target instanceof Player;
         float avatarWidth = isPlayer ? 30.0F : 0.0F;
         float hudWidth = Math.max(textWidth + 15.0F, 80.0F) + avatarWidth;
         float hudHeight = 38.0F;
         
         this.blurMatrix = new Vector4f(x, y, hudWidth, hudHeight);
         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(e.getStack(), x, y, hudWidth, hudHeight, 5.0F, HUD.headerColor);
         StencilUtils.erase(true);
         
         RenderUtils.fillBound(e.getStack(), x, y, hudWidth, hudHeight, HUD.bodyColor);
         
         RenderUtils.fillBound(e.getStack(), x, y, hudWidth * (living.getHealth() / living.getMaxHealth()), 3.0F, HUD.headerColor);
         
         StencilUtils.dispose();
         
         if (isPlayer) {
            Player player = (Player)target;
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
               ResourceLocation skin = playerInfo.getSkinLocation();
               GuiGraphics guiGraphics = e.getGuiGraphics();
               
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               
               // 绘制头像（25x25，居中显示）
               int avatarX = (int)x + 5;
               int avatarY = (int)y + 8;
               guiGraphics.blit(skin, avatarX, avatarY, 25, 25, 8.0F, 8.0F, 8, 8, 64, 64);
               guiGraphics.blit(skin, avatarX, avatarY, 25, 25, 40.0F, 8.0F, 8, 8, 64, 64);
               
               RenderSystem.disableBlend();
            }
         }
         
         float textX = x + avatarWidth + 8.0F;
         float nameY = y + 8.0F;
         float hpY = y + 20.0F;
         
         Fonts.harmony.render(e.getStack(), targetName, (double)textX, (double)nameY, Color.WHITE, true, 0.4F);
         Fonts.harmony
               .render(
                  e.getStack(),
                  "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""),
                  (double)textX,
                  (double)hpY,
                  Color.WHITE,
                  true,
                  0.35F
               );
         
         // 在所有目标实体身体位置显示旋转RGB图片（在HUD之后渲染）
         float partialTicks = mc.getFrameTime();

         // 保存当前渲染状态
         PoseStack poseStack = e.getStack();
         poseStack.pushPose();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.depthMask(false);

         // 获取玩家眼睛位置（用于计算距离）
         Vec3 eyePos = mc.player.getEyePosition(partialTicks);

         // 为所有目标实体渲染图片
         for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
               Vec3 targetPos = entity.getPosition(partialTicks);
               Vector2f screenPos = ProjectionUtils.project(targetPos.x, targetPos.y + entity.getBbHeight() / 2, targetPos.z, partialTicks);
               
                  if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) { // 目标在屏幕内
                     // 根据距离动态调整大小 - 真正的近大远小
                     double distance = eyePos.distanceTo(targetPos);
                     
                     // 设置基准距离和大小
                     float baseDistance = 5.0f; // 基准距离（5格）
                     float baseSize = 80.0f;    // 基准大小（80像素）
                     float minSize = 20.0f;     // 最小大小
                     float maxSize = 120.0f;    // 最大大小
                     
                     // 计算基于距离的大小（反比例关系）
                     float size = baseSize * (float)(baseDistance / Math.max(1.0, distance));
                     
                     // 限制在最小和最大值之间
                     size = Math.max(minSize, Math.min(maxSize, size));
                     
                     float imageX = screenPos.x - size/2;
                     float imageY = screenPos.y - size/2;
                     // 计算旋转角度（随时间旋转）
                     float rotationAngle = (System.currentTimeMillis() % 10000) * 0.036f;
                     
                     // 计算柔和的RGB变色效果
                     long time = System.currentTimeMillis();
                     float r = (float)(Math.sin(time * 0.001) * 0.3 + 0.7);
                     float g = (float)(Math.sin(time * 0.001 + 2.0) * 0.3 + 0.7);
                     float b = (float)(Math.sin(time * 0.001 + 4.0) * 0.3 + 0.7);
                     float a = 0.7f; // 添加透明度
                     
                     // 设置颜色，保留透明度
                     RenderSystem.setShaderColor(r, g, b, a);
                     
                     // 加载并渲染图片
                     ResourceLocation renderImage = new ResourceLocation("navenxd:esp/rectangle.png");
                     RenderSystem.setShaderTexture(0, renderImage);
                     
                     poseStack.pushPose();
                     poseStack.translate(imageX + size/2, imageY + size/2, 0);
                     poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationAngle));
                     poseStack.translate(-(imageX + size/2), -(imageY + size/2), 0);
                     
                     // 使用blit渲染图片，保持固定大小
                     e.getGuiGraphics().blit(
                           renderImage, 
                           (int)imageX, 
                           (int)imageY, 
                           0, 
                           0, 
                           (int)size, 
                           (int)size, 
                           (int)size, 
                           (int)size
                     );
                     
                     poseStack.popPose();
               }
            }
         }

         // 恢复渲染状态
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         poseStack.popPose();
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      if (this.targetEsp.getCurrentValue()) {
         PoseStack stack = e.getPMatrixStack();
         float partialTicks = e.getRenderPartialTicks();
         stack.pushPose();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glEnable(2848);
         RenderSystem.setShader(GameRenderer::getPositionShader);
         RenderUtils.applyRegionalRenderOffset(stack);

         for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
               float[] color = target == living ? targetColorRed : targetColorGreen;
               stack.pushPose();
               RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
               double motionX = entity.getX() - entity.xo;
               double motionY = entity.getY() - entity.yo;
               double motionZ = entity.getZ() - entity.zo;
               AABB boundingBox = entity.getBoundingBox()
                  .move(-motionX, -motionY, -motionZ)
                  .move((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ);
               // 删除方块绘制代码
               stack.popPose();


            }
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glDisable(3042);
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDisable(2848);
         stack.popPose();
      }
   }

   @Override
public void onEnable() {
   rotation = null;
   this.index = 0;
   target = null;
   aimingTarget = null;
   targets.clear();
   this.cooldownProgress = 0.0F;
   this.currentCps = this.minCps.getCurrentValue();
   this.lastFluctuationTime = System.currentTimeMillis();
   this.rampUpStartTime = System.currentTimeMillis();
   this.isRampingUp = true;
}

   @Override
public void onDisable() {
   target = null;
   aimingTarget = null;
   super.onDisable();
}

   @EventTarget
   public void onRespawn(EventRespawn e) {
      target = null;
      aimingTarget = null;
   }

   @EventTarget
   public void onAttackSlowdown(EventAttackSlowdown e) {
      e.setCancelled(true);
   }

   @EventTarget
   public void onMotion(EventRunTicks event) {
      if (event.getType() == EventType.PRE && mc.player != null) {
         if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
            || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
            || InventoryUtils.shouldDisableFeatures()) {
            target = null;
            aimingTarget = null;
            this.rotationData = null;
            rotation = null;
            this.lastRotationData = null;
            targets.clear();
            return;
         }

         boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
         this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
         this.updateAttackTargets();
         aimingTarget = this.shouldPreAim();
         this.lastRotationData = this.rotationData;
         this.rotationData = null;
         if (aimingTarget != null) {
            this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
            if (this.rotationData.getRotation() != null) {
               // 初始化当前旋转角度（如果为空）
               if (this.currentRotation == null) {
                  this.currentRotation = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
               }
               
               // 获取目标旋转角度
               Vector2f targetRotation = this.rotationData.getRotation();
               
               // 使用插值实现平滑转头
               float rotationSpeed = this.rotationSpeed.getCurrentValue();
               float maxDelta = rotationSpeed * 0.05F; // 根据速度计算每帧最大旋转角度
               
               // 对yaw和pitch分别进行插值
               float newYaw = RotationUtils.updateRotation(this.currentRotation.x, targetRotation.x, maxDelta);
               float newPitch = RotationUtils.updateRotation(this.currentRotation.y, targetRotation.y, maxDelta);
               
               // 更新当前旋转角度
               this.currentRotation = new Vector2f(newYaw, newPitch);
               rotation = this.currentRotation;
            } else {
               rotation = null;
            }
         } else {
            // 没有目标时，将当前旋转角度设置为null
            this.currentRotation = null;
         }

         if (targets.isEmpty()) {
            target = null;
            return;
         }

         if (this.index > targets.size() - 1) {
            this.index = 0;
         }

         if (targets.size() > 1
            && ((float)this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > this.attackRange.getCurrentValue())) {
            this.attackTimes = 0;

            for (int i = 0; i < targets.size(); i++) {
               this.index++;
               if (this.index > targets.size() - 1) {
                  this.index = 0;
               }

               Entity nextTarget = targets.get(this.index);
               RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
               
               // 如果获取到有效的旋转数据，则使用插值更新当前旋转角度
               if (data != null && data.getRotation() != null && this.currentRotation != null) {
                  // 获取目标旋转角度
                  Vector2f targetRotation = data.getRotation();
                  
                  // 使用插值实现平滑转头
                  float rotationSpeed = this.rotationSpeed.getCurrentValue();
                  float maxDelta = rotationSpeed * 0.05F; // 根据速度计算每帧最大旋转角度
                  
                  // 对yaw和pitch分别进行插值
                  float newYaw = RotationUtils.updateRotation(this.currentRotation.x, targetRotation.x, maxDelta);
                  float newPitch = RotationUtils.updateRotation(this.currentRotation.y, targetRotation.y, maxDelta);
                  
                  // 更新当前旋转角度
                  this.currentRotation = new Vector2f(newYaw, newPitch);
               }
               
               // 使用BMWClient-nextgen的攻击范围检查逻辑
               Vec3 eyePos = mc.player.getEyePosition();
               Vec3 targetPos = nextTarget.getBoundingBox().getCenter();
               double distance = eyePos.distanceTo(targetPos);
               
               // 修复攻击距离超过3无法攻击的问题
               if (distance <= (double)this.attackRange.getCurrentValue()) {
                  break;
               }
            }
         }

         if (this.index > targets.size() - 1 || !isSwitch) {
            this.index = 0;
         }

         target = targets.get(this.index);
         
         // 更新CPS浮动
         if (this.fluctuateCps.getCurrentValue() && !this.pvp19.getCurrentValue()) {
             long currentTime = System.currentTimeMillis();
             
             // 处理CPS逐渐提升
             if (this.isRampingUp) {
                 float rampUpProgress = (float)(currentTime - this.rampUpStartTime) / (this.rampUpTime.getCurrentValue() * 1000.0F);
                 
                 if (rampUpProgress >= 1.0F) {
                     this.isRampingUp = false;
                     this.currentCps = this.minCps.getCurrentValue();
                 } else {
                     // 线性提升CPS从0到最小值
                     this.currentCps = this.minCps.getCurrentValue() * rampUpProgress;
                     return;
                 }
             }
             
             // 正常浮动逻辑
             if (currentTime - this.lastFluctuationTime > 1000) { // 每秒更新一次浮动方向
                 this.lastFluctuationTime = currentTime;
                 this.increasing = random.nextBoolean();
             }
             
             float fluctuation = this.fluctuateAmount.getCurrentValue() * (random.nextFloat() * 2.0F - 1.0F);
             
             if (this.increasing) {
                 this.currentCps += fluctuation;
                 if (this.currentCps > this.maxCps.getCurrentValue()) {
                     this.currentCps = this.maxCps.getCurrentValue();
                     this.increasing = false;
                 }
             } else {
                 this.currentCps -= fluctuation;
                 if (this.currentCps < this.minCps.getCurrentValue()) {
                     this.currentCps = this.minCps.getCurrentValue();
                     this.increasing = true;
                 }
             }
             
             // 确保CPS在最小和最大值之间
             this.currentCps = Math.max(this.minCps.getCurrentValue(), Math.min(this.maxCps.getCurrentValue(), this.currentCps));
         }
         
         // 更新攻击冷却进度
         if (this.pvp19.getCurrentValue()) {
            this.cooldownProgress = mc.player.getAttackStrengthScale(0.0F);
         } else {
            if (this.fluctuateCps.getCurrentValue()) {
                this.attacks = this.attacks + this.currentCps / 20.0F;
            } else {
                this.attacks = this.attacks + this.aps.getCurrentValue() / 20.0F;
            }
         }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (mc.player.getUseItem().isEmpty()
         && mc.screen == null
         && Naven.skipTasks.isEmpty()
         && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
         
         if (this.pvp19.getCurrentValue()) {
            // 1.9+ PvP模式：只在攻击冷却满时攻击
            if (this.cooldownProgress >= 1.0F) {
               this.doAttack();
            }
         } else {
            // 传统模式：按CPS攻击
            while (this.attacks >= 1.0F) {
               this.doAttack();
               this.attacks--;
            }
         }
      }
   }

   public Entity shouldPreAim() {
      Entity target = KillAura.target;
      if (target == null) {
         List<Entity> aimTargets = this.getTargets();
         if (!aimTargets.isEmpty()) {
            target = aimTargets.get(0);
         }
      }

      return target;
   }

   public void doAttack() {
      if (!targets.isEmpty()) {
         HitResult hitResult = mc.hitResult;
         if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            if (AntiBots.isBot(result.getEntity())) {
               ChatUtils.addChatMessage("Attacking Bot!");
               return;
            }
         }

         if (this.multi.getCurrentValue()) {
            int attacked = 0;

            for (Entity entity : targets) {
               // 使用BMWClient-nextgen的攻击范围检查逻辑
               Vec3 eyePos = mc.player.getEyePosition();
               Vec3 targetPos = entity.getBoundingBox().getCenter();
               double distance = eyePos.distanceTo(targetPos);
               
               // 修复攻击距离超过3无法攻击的问题
               if (distance <= (double)this.attackRange.getCurrentValue()) {
                  this.attackEntity(entity);
                  if (++attacked >= 2) {
                     break;
                  }
               }
            }
         } else if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            this.attackEntity(result.getEntity());
         }
      }
   }

   // 新的攻击方法，基于BMWClient-nextgen的实现
   public void attackEntity(Entity entity) {
      this.attackTimes++;
      
      // 保存当前旋转角度
      float currentYaw = mc.player.getYRot();
      float currentPitch = mc.player.getXRot();
      
      // 设置旋转角度到目标
      if (RotationManager.rotations != null) {
         mc.player.setYRot(RotationManager.rotations.x);
         mc.player.setXRot(RotationManager.rotations.y);
      }
      
      if (entity instanceof Player && !AntiBots.isBot(entity)) {
         KillSay.attackedPlayers.add(entity.getName().getString());
      }

      // 使用BMWClient-nextgen的攻击逻辑
      // 检查攻击范围
      Vec3 eyePos = mc.player.getEyePosition();
      Vec3 targetPos = entity.getBoundingBox().getCenter();
      double distance = eyePos.distanceTo(targetPos);
      
      // 修复攻击距离超过3无法攻击的问题
      if (distance <= (double)this.attackRange.getCurrentValue()) {
         // 执行攻击
         mc.gameMode.attack(mc.player, entity);
         mc.player.swing(InteractionHand.MAIN_HAND);
         
         if (this.moreParticles.getCurrentValue()) {
            mc.player.magicCrit(entity);
            mc.player.crit(entity);
         }
      }
      
      // 恢复原始旋转角度
      mc.player.setYRot(currentYaw);
      mc.player.setXRot(currentPitch);
   }

   public void updateAttackTargets() {
      targets = this.getTargets();
   }

   public boolean isValidTarget(Entity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         if (living instanceof BlinkingPlayer) {
            return false;
         } else {
            AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
               if (Teams.isSameTeam(living)) {
                  return false;
               } else if (FriendManager.isFriend(living)) {
                  return false;
               } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStand) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                     && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isValidAttack(Entity entity) {
      if (!this.isValidTarget(entity)) {
         return false;
      } else if (entity instanceof LivingEntity && (float)((LivingEntity)entity).hurtTime > this.hurtTime.getCurrentValue()) {
         return false;
      } else {
         // 使用BMWClient-nextgen的攻击范围检查逻辑
         Vec3 eyePos = mc.player.getEyePosition();
         Vec3 targetPos = entity.getBoundingBox().getCenter();
         double distance = eyePos.distanceTo(targetPos);
         
         // 修复攻击距离超过3无法攻击的问题
         return distance <= (double)this.attackRange.getCurrentValue()
            && RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
      }
   }

   

   private List<Entity> getTargets() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
         .filter(entity -> entity instanceof Entity)
         .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("Range")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("FoV")) {
         possibleTargets.sort(
            Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("Health")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
      }

      if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity)entity).isBaby())) {
         possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity)entity).isBaby());
      }

      possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
      return this.infSwitch.getCurrentValue()
         ? possibleTargets
         : possibleTargets.subList(0, (int)Math.min((float)possibleTargets.size(), this.switchSize.getCurrentValue()));
   }
}