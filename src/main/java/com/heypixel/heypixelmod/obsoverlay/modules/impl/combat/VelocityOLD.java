package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(
        name = "SkyVelocity",
        description = "by StarSky",
        category = Category.COMBAT
)
public class VelocityOLD extends Module {

   // ====== 配置 ======
   private final BooleanValue sendAttackPackets = ValueBuilder.create(this, "Send Attack Packets")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   private final FloatValue attackCount = ValueBuilder.create(this, "Attack Count")
           .setDefaultFloatValue(5.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(16.0F)
           .build()
           .getFloatValue();

   private final BooleanValue keepSprint = ValueBuilder.create(this, "Keep Sprint")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private final BooleanValue onlySprint = ValueBuilder.create(this, "Only Sprint")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   private final BooleanValue onlyOnGround = ValueBuilder.create(this, "Only On Ground")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   private final BooleanValue antiBounce = ValueBuilder.create(this, "AntiMoreVl")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   private final BooleanValue attackCooldown = ValueBuilder.create(this, "Attack Cooldown")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   private final BooleanValue debugMessages = ValueBuilder.create(this, "Debug Messages")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   // 新增：目标检索配置
   private final BooleanValue multiTargetMode = ValueBuilder.create(this, "Multi Target")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   private final FloatValue targetRange = ValueBuilder.create(this, "Target Range")
           .setDefaultFloatValue(4.0F)
           .setFloatStep(0.5F)
           .setMinFloatValue(2.0F)
           .setMaxFloatValue(8.0F)
           .build()
           .getFloatValue();

   private final BooleanValue prioritizeCrosshair = ValueBuilder.create(this, "Prioritize Crosshair")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   // 新增：Fov系统配置
   private final BooleanValue useFov = ValueBuilder.create(this, "Use Fov")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private final FloatValue fov = ValueBuilder.create(this, "Fov")
           .setDefaultFloatValue(90.0F)
           .setFloatStep(5.0F)
           .setMinFloatValue(10.0F)
           .setMaxFloatValue(360.0F)
           .setVisibility(useFov::getCurrentValue)
           .build()
           .getFloatValue();

   private final BooleanValue dynamicFov = ValueBuilder.create(this, "Dynamic Fov")
           .setDefaultBooleanValue(true)
           .setVisibility(useFov::getCurrentValue)
           .build()
           .getBooleanValue();

   // 状态变量
   private boolean velocityInput = false;
   private LivingEntity target = null;
   private boolean wasSprinting;
   private int attackCounter = 0;

   // 回弹检测
   private int bounceCooldown = 0;
   private double lastVelocityX = 0;
   private double lastVelocityZ = 0;

   // 攻击冷却
   private int attackCooldownTicks = 0;

   // 目标缓存
   private long lastTargetFindTime = 0;
   private static final long TARGET_FIND_COOLDOWN = 50; // 毫秒

   @Override
   public void onEnable() {
      resetState();
   }

   @Override
   public void onDisable() {
      resetState();
   }

   private void resetState() {
      velocityInput = false;
      target = null;
      attackCounter = 0;
      bounceCooldown = 0;
      lastVelocityX = 0;
      lastVelocityZ = 0;
      attackCooldownTicks = 0;
      lastTargetFindTime = 0;
   }

   @EventTarget
   public void onPacket(EventHandlePacket e) {
      LocalPlayer player = mc.player;
      if (player == null) return;

      Packet<?> packet = e.getPacket();

      // 处理击退包
      if (packet instanceof ClientboundSetEntityMotionPacket) {
         ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket) packet;

         if (velocityPacket.getId() != player.getId()) {
            return;
         }

         // 计算当前速度
         double currentX = velocityPacket.getXa() / 8000.0;
         double currentZ = velocityPacket.getZa() / 8000.0;

         // 检测回弹
         if (antiBounce.getCurrentValue() && bounceCooldown <= 0) {
            if (isBounce(currentX, currentZ)) {
               bounceCooldown = 6;
               sendDebugMessage("Bounce detected! Disabling velocity reduction for 6 ticks");
               return;
            }

            // 更新最后速度
            lastVelocityX = currentX;
            lastVelocityZ = currentZ;
         }

         // 检查攻击冷却
         if (attackCooldown.getCurrentValue() && attackCooldownTicks > 0) {
            sendDebugMessage("Skipping attack packets due to cooldown (" + attackCooldownTicks + " ticks remaining)");
            return;
         }

         // 检查疾跑条件
         if (onlySprint.getCurrentValue() && !player.isSprinting()) {
            sendDebugMessage("Skipping attack packets because player is not sprinting");
            return;
         }

         // 检查地面条件
         if (onlyOnGround.getCurrentValue() && !player.onGround()) {
            sendDebugMessage("Skipping attack packets because player is not on ground");
            return;
         }

         // 获取目标实体 - 使用增强的目标检索
         target = findBestAttackTarget();
         if (target == null) {
            sendDebugMessage("No valid target found");
            return;
         }

         sendDebugMessage("Target acquired: " + target.getName().getString() + " (Distance: " + String.format("%.1f", player.distanceTo(target)) + ")");

         // 保存原始疾跑状态
         wasSprinting = player.isSprinting();

         // 临时启用疾跑
         if (!wasSprinting) {
            sendSprintPacket(player, true);
         }

         // 设置攻击状态
         velocityInput = true;
         attackCounter = (int) attackCount.getCurrentValue();

         sendDebugMessage("Velocity triggered, will send " + attackCounter + " attack packets");
      }
   }

   @EventTarget
   public void onTick(EventRunTicks eventRunTicks) {
      if (mc.player == null || eventRunTicks.getType() != EventType.PRE) {
         return;
      }

      LocalPlayer player = mc.player;

      // 更新回弹冷却
      if (bounceCooldown > 0) {
         bounceCooldown--;
         if (bounceCooldown == 0) {
            sendDebugMessage("Bounce cooldown ended");
         }
      }

      // 更新攻击冷却
      if (attackCooldownTicks > 0) {
         attackCooldownTicks--;
         if (attackCooldownTicks == 0) {
            sendDebugMessage("Attack cooldown ended");
         }
      }

      // 当玩家受伤时间为0时重置状态
      if (player.hurtTime == 0) {
         velocityInput = false;
         target = null;
      }

      // 执行攻击和速度减少
      if (velocityInput && attackCounter > 0 && target != null && bounceCooldown <= 0) {
         // 检查目标是否仍然有效
         if (!isTargetValid(target)) {
            sendDebugMessage("Target became invalid, stopping attacks");
            resetState();
            return;
         }

         // 发送攻击包
         if (sendAttackPackets.getCurrentValue()) {
            player.connection.send(ServerboundInteractPacket.createAttackPacket(target, false));
            player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

            int currentAttack = (int) attackCount.getCurrentValue() - attackCounter + 1;
            sendDebugMessage("Sent attack packet " + currentAttack + "/" + (int) attackCount.getCurrentValue());
         }

         // 应用速度减少
         Vec3 velocity = player.getDeltaMovement();
         player.setDeltaMovement(
                 velocity.x * 0.6,
                 velocity.y,
                 velocity.z * 0.6
         );

         sendDebugMessage("Applied velocity reduction (Factor: 0.6)");

         attackCounter--;

         // 完成所有攻击后重置状态
         if (attackCounter == 0) {
            // 恢复原始疾跑状态
            if (!wasSprinting && keepSprint.getCurrentValue()) {
               sendSprintPacket(player, false);
            }

            // 设置攻击冷却
            if (attackCooldown.getCurrentValue()) {
               attackCooldownTicks = 5;
               sendDebugMessage("Attack cooldown started (5 ticks)");
            }

            velocityInput = false;
            target = null;
         }
      }
      // 如果处于回弹冷却期间，重置攻击状态但不应用减少
      else if (velocityInput && attackCounter > 0 && bounceCooldown > 0) {
         sendDebugMessage("Skipping velocity reduction due to bounce cooldown");
         attackCounter = 0;
         velocityInput = false;
         target = null;
      }
   }

   /**
    * 增强的目标检索机制
    */
   private LivingEntity findBestAttackTarget() {
      LocalPlayer player = mc.player;
      if (player == null) return null;

      // 冷却时间检查
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastTargetFindTime < TARGET_FIND_COOLDOWN) {
         return target; // 返回缓存的目标
      }
      lastTargetFindTime = currentTime;

      // 1. 优先检查准星目标
      if (prioritizeCrosshair.getCurrentValue()) {
         LivingEntity crosshairTarget = findCrosshairTarget();
         if (crosshairTarget != null && isTargetValid(crosshairTarget)) {
            return crosshairTarget;
         }
      }

      // 2. 多目标模式：寻找附近最佳目标
      if (multiTargetMode.getCurrentValue()) {
         return findNearestValidTarget();
      }

      return null;
   }

   /**
    * 查找准星指向的目标
    */
   private LivingEntity findCrosshairTarget() {
      HitResult hitResult = mc.hitResult;
      if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
         return null;
      }

      if (hitResult instanceof EntityHitResult) {
         Entity entity = ((EntityHitResult) hitResult).getEntity();
         if (entity instanceof LivingEntity) {
            return (LivingEntity) entity;
         }
      }
      return null;
   }

   /**
    * 查找附近最近的有效目标
    */
   private LivingEntity findNearestValidTarget() {
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) return null;

      float range = targetRange.getCurrentValue();

      // 获取范围内所有活着的可攻击实体
      List<LivingEntity> potentialTargets = mc.level.getEntitiesOfClass(
              LivingEntity.class,
              player.getBoundingBox().inflate(range),
              entity -> isTargetValid(entity) && entity != player
      );

      if (potentialTargets.isEmpty()) {
         return null;
      }

      // 应用Fov筛选
      if (useFov.getCurrentValue()) {
         potentialTargets = filterByFov(potentialTargets);
         if (potentialTargets.isEmpty()) {
            return null;
         }
      }

      // 按距离排序，选择最近的目标
      return potentialTargets.stream()
              .min(Comparator.comparingDouble(entity -> player.distanceToSqr(entity)))
              .orElse(null);
   }

   /**
    * 根据Fov筛选目标
    */
   private List<LivingEntity> filterByFov(List<LivingEntity> targets) {
      if (targets.isEmpty() || !useFov.getCurrentValue()) {
         return targets;
      }

      LocalPlayer player = mc.player;
      if (player == null) return targets;

      float currentFov = getCurrentFov();

      return targets.stream()
              .filter(target -> isInFov(target, currentFov))
              .collect(Collectors.toList());
   }

   /**
    * 检查目标是否在Fov范围内
    */
   private boolean isInFov(LivingEntity target, float fov) {
      LocalPlayer player = mc.player;
      if (player == null) return false;

      // 计算目标相对于玩家的角度
      Vec3 playerPos = player.getEyePosition();
      Vec3 targetPos = target.getBoundingBox().getCenter();
      Vec3 directionToTarget = targetPos.subtract(playerPos).normalize();

      // 获取玩家当前视线方向
      Vec3 lookVec = player.getLookAngle();

      // 计算夹角（角度）
      double dotProduct = lookVec.dot(directionToTarget);
      double angle = Math.toDegrees(Math.acos(dotProduct));

      return angle <= fov / 2.0;
   }

   /**
    * 获取当前Fov值（支持动态Fov）
    */
   private float getCurrentFov() {
      if (!useFov.getCurrentValue()) {
         return 360.0f; // 禁用Fov时返回全向
      }

      if (dynamicFov.getCurrentValue()) {
         // 动态Fov：根据玩家移动状态调整
         LocalPlayer player = mc.player;
         if (player != null) {
            // 移动时使用较小Fov，静止时使用较大Fov
            boolean isMoving = player.input.left || player.input.right || player.input.up || player.input.down;
            return isMoving ? fov.getCurrentValue() * 0.8f : fov.getCurrentValue();
         }
      }

      return fov.getCurrentValue();
   }

   /**
    * 检查目标是否有效
    */
   private boolean isTargetValid(LivingEntity target) {
      if (target == null || !target.isAlive() || !target.isAttackable()) {
         return false;
      }

      LocalPlayer player = mc.player;
      if (player == null) return false;

      // 检查距离
      float maxRange = targetRange.getCurrentValue();
      if (player.distanceTo(target) > maxRange) {
         return false;
      }

      // 检查Fov（如果启用）
      if (useFov.getCurrentValue() && !isInFov(target, getCurrentFov())) {
         sendDebugMessage("Target not in Fov: " + target.getName().getString());
         return false;
      }

      // 检查视线（可选，避免穿墙攻击）
      if (!player.hasLineOfSight(target)) {
         sendDebugMessage("Target not in line of sight: " + target.getName().getString());
         // 这里可以选择是否要求视线，根据需求调整
         // return false;
      }

      return true;
   }

   // 检测回弹
   private boolean isBounce(double currentX, double currentZ) {
      double dotProduct = lastVelocityX * currentX + lastVelocityZ * currentZ;
      double magnitudeLast = Math.sqrt(lastVelocityX * lastVelocityX + lastVelocityZ * lastVelocityZ);
      double magnitudeCurrent = Math.sqrt(currentX * currentX + currentZ * currentZ);

      if (magnitudeLast < 0.001 || magnitudeCurrent < 0.001) {
         return false;
      }

      double similarity = dotProduct / (magnitudeLast * magnitudeCurrent);
      return similarity < -0.8;
   }

   // 发送疾跑状态包
   private void sendSprintPacket(LocalPlayer player, boolean startSprinting) {
      if (player.connection == null) return;

      ServerboundPlayerCommandPacket.Action action = startSprinting ?
              ServerboundPlayerCommandPacket.Action.START_SPRINTING :
              ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;

      player.connection.send(new ServerboundPlayerCommandPacket(player, action));
      sendDebugMessage("Sent " + (startSprinting ? "START_SPRINTING" : "STOP_SPRINTING") + " packet");
   }

   // 发送调试信息到聊天栏
   private void sendDebugMessage(String message) {
      if (mc.player != null && debugMessages.getCurrentValue()) {
         mc.player.sendSystemMessage(Component.literal("[Velocity] " + message));
      }
   }
}