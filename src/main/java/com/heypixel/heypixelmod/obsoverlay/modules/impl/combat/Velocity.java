package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.LinkedBlockingQueue;
@ModuleInfo(
        name = "Velocity",
        description = "Reduce knockback",
        category = Category.COMBAT
)
public class Velocity extends Module {

   // ====== 配置 ======
   private final ModeValue mode = ValueBuilder.create(this, "Mode")
           .setModes("None", "Old", "GrimReduce", "Delay")
           .setDefaultModeIndex(1)
           .build()
           .getModeValue();

   private final BooleanValue sendAttackPackets = ValueBuilder.create(this, "Send Attack Packets")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final FloatValue attackCount = ValueBuilder.create(this, "Attack Count")
           .setDefaultFloatValue(5.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(16.0F)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getFloatValue();

   private final BooleanValue keepSprint = ValueBuilder.create(this, "Keep Sprint")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final BooleanValue onlySprint = ValueBuilder.create(this, "Only Sprint")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final BooleanValue onlyOnGround = ValueBuilder.create(this, "Only On Ground")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final BooleanValue antiBounce = ValueBuilder.create(this, "AntiMoreVl")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final BooleanValue attackCooldown = ValueBuilder.create(this, "Attack Cooldown")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final BooleanValue debugMessages = ValueBuilder.create(this, "Debug Messages")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> mode.isCurrentMode("Old") || mode.isCurrentMode("Delay"))
           .build()
           .getBooleanValue();

   // Delay模式配置
   private final FloatValue delayTime = ValueBuilder.create(this, "Delay Time")
           .setDefaultFloatValue(1000.0F)
           .setFloatStep(100.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(5000.0F)
           .setVisibility(() -> mode.isCurrentMode("Delay"))
           .build()
           .getFloatValue();

   private final FloatValue releaseCount = ValueBuilder.create(this, "Release Count")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .setVisibility(() -> mode.isCurrentMode("Delay"))
           .build()
           .getFloatValue();

   // 新增：目标检索配置
   private final BooleanValue multiTargetMode = ValueBuilder.create(this, "Multi Target")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final FloatValue targetRange = ValueBuilder.create(this, "Target Range")
           .setDefaultFloatValue(4.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(2.0F)
           .setMaxFloatValue(8.0F)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getFloatValue();

   private final BooleanValue prioritizeCrosshair = ValueBuilder.create(this, "Prioritize Crosshair")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   // 新增：Fov系统配置
   private final BooleanValue useFov = ValueBuilder.create(this, "Use Fov")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> mode.isCurrentMode("Old"))
           .build()
           .getBooleanValue();

   private final FloatValue fov = ValueBuilder.create(this, "Fov")
           .setDefaultFloatValue(120.0F)
           .setFloatStep(5.0F)
           .setMinFloatValue(10.0F)
           .setMaxFloatValue(360.0F)
           .setVisibility(() -> mode.isCurrentMode("Old") && useFov.getCurrentValue())
           .build()
           .getFloatValue();

   private final BooleanValue dynamicFov = ValueBuilder.create(this, "Dynamic Fov")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> mode.isCurrentMode("Old") && useFov.getCurrentValue())
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

   // GrimReduce模式状态变量
   public static int ticksSinceVelocity = Integer.MAX_VALUE;
   private boolean isKnockbacked = false;
   private int offGroundTicks = 0;
   private final List<Packet<?>> packets = new ArrayList<>();

   // Delay模式状态变量
   private boolean delayedMode = false;
   private long delayStartTime = 0L;
   private int receivedKnockbacks = 0;
   private int releasedKnockbacks = 0;
   private int packetsToRelease = 0;
   private final List<Integer> knockbackPositions = new ArrayList<>();
   private final LinkedBlockingQueue<Packet<?>> delayedPackets = new LinkedBlockingQueue<>();

   @Override
   public void onEnable() {
      resetState();

   }

   @Override
   public void onDisable() {
      resetState();
      releaseAllDelayedPackets();
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
      
      // GrimReduce模式状态重置
      isKnockbacked = false;
      offGroundTicks = 0;
      packets.clear();
      ticksSinceVelocity = Integer.MAX_VALUE;

      // Delay模式状态重置
      delayedMode = false;
      receivedKnockbacks = 0;
      releasedKnockbacks = 0;
      packetsToRelease = 0;
      knockbackPositions.clear();
      delayedPackets.clear();
   }

   @EventTarget
   public void onPacket(EventHandlePacket e) {
      LocalPlayer player = mc.player;
      if (player == null) return;

      // 模式检查：None模式不处理任何包
      if (mode.isCurrentMode("None")) {
         this.setSuffix("Disabled");
         return;
      }

      // GrimReduce模式处理
      if (mode.isCurrentMode("GrimReduce")) {
         this.setSuffix("GrimReduce");
         handleGrimReducePacket(e);
         return;
      }

      // Delay模式处理
      if (mode.isCurrentMode("Delay")) {
         this.setSuffix("Delay");
         handleDelayPacket(e);
         return;
      }

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
               sendDebugMessage("Stop 6tick velocity");
               return;
            }

            // 更新最后速度
            lastVelocityX = currentX;
            lastVelocityZ = currentZ;
         }

         // 检查攻击冷却
         if (attackCooldown.getCurrentValue() && attackCooldownTicks > 0) {
            sendDebugMessage("Stop the attack, because (" + attackCooldownTicks + " tick cooldown has not elapsed)");
            return;
         }

         // 检查疾跑条件
         if (onlySprint.getCurrentValue() && !player.isSprinting()) {
            sendDebugMessage("Stop the attack, because you are not sprinting");
            return;
         }

         // 检查地面条件
         if (onlyOnGround.getCurrentValue() && !player.onGround()) {
            sendDebugMessage("Stop attacking. Because you are not on the ground.");
            return;
         }

         // 获取目标实体 - 使用增强的目标检索
         target = findBestAttackTarget();
         if (target == null) {
            sendDebugMessage("No target entity found.");
            return;
         }

         sendDebugMessage("Target: " + target.getName().getString() + " (Distance: " + String.format("%.1f", player.distanceTo(target)) + ")");

         // 保存原始疾跑状态
         wasSprinting = player.isSprinting();

         // 临时启用疾跑
         if (!wasSprinting) {
            sendSprintPacket(player, true);
         }

         // 设置攻击状态
         velocityInput = true;
         attackCounter = (int) attackCount.getCurrentValue();

         sendDebugMessage("Counterback enabled! Will send " + attackCounter + " attack packets.");
      }
   }

   @EventTarget
   public void onTick(EventRunTicks eventRunTicks) {
      if (mc.player == null || eventRunTicks.getType() != EventType.PRE) {
         return;
      }

      // 模式检查：None模式不执行任何tick逻辑
      if (mode.isCurrentMode("None")) {
         this.setSuffix("Disabled");
         return;
      }

      // GrimReduce模式处理
      if (mode.isCurrentMode("GrimReduce")) {
         this.setSuffix("GrimReduce");
         handleGrimReduceTick(eventRunTicks);
         return;
      }

      // Delay模式处理
      if (mode.isCurrentMode("Delay")) {
         this.setSuffix("Delay");
         handleDelayTick(eventRunTicks);
         return;
      }

      LocalPlayer player = mc.player;

      // 设置Old模式后缀
      this.setSuffix("Old");

      // 更新回弹冷却
      if (bounceCooldown > 0) {
         bounceCooldown--;
         if (bounceCooldown == 0) {
            sendDebugMessage("Bounce cooldown has ended.");
         }
      }

      // 更新攻击冷却
      if (attackCooldownTicks > 0) {
         attackCooldownTicks--;
         if (attackCooldownTicks == 0) {
            sendDebugMessage("Attack cooldown has ended.");
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
            sendDebugMessage("Target not found. Stopping attack.");
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

         sendDebugMessage("Counterback effect applied! (Factor: 0.6)");

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
               sendDebugMessage("Attack cooldown started (5 ticks).");
            }

            velocityInput = false;
            target = null;
         }
      }
      // 如果处于回弹冷却期间，重置攻击状态但不应用减少
      else if (velocityInput && attackCounter > 0 && bounceCooldown > 0) {
         sendDebugMessage("Bounce cooldown active (remaining: " + bounceCooldown + " ticks).");
         attackCounter = 0;
         velocityInput = false;
         target = null;
      }
   }

   /**
    * GrimReduce模式tick处理
    */
   private void handleGrimReduceTick(EventRunTicks eventRunTicks) {
      LocalPlayer player = mc.player;
      if (player == null) return;

      // 更新地面状态
      if (player.onGround()) {
         offGroundTicks = 0;
      } else {
         offGroundTicks++;
      }

      // 更新速度计时器
      if (ticksSinceVelocity < Integer.MAX_VALUE) {
         ticksSinceVelocity++;
      }

      // 处理击退结束后的包发送
      if (eventRunTicks.getType() == EventType.POST && 
          (player.onGround() && isKnockbacked || isKnockbacked && offGroundTicks > 10)) {
         isKnockbacked = false;

         for(Packet<?> packet : packets) {
            player.connection.send(packet);
         }

         packets.clear();
      }
   }

   public static int getTicksSinceVelocity() {
      return ticksSinceVelocity;
   }

   @EventTarget
   public void onPre(EventMotion event) {
      if (event.getType() != EventType.PRE || mc.player == null) {
         return;
      }

      // GrimReduce模式处理
      if (mode.isCurrentMode("GrimReduce") && getTicksSinceVelocity() <= 14 && mc.player.onGround()) {
         mc.player.resetFallDistance();
      }
   }

   /**
    * Delay模式包处理
    */
   private void handleDelayPacket(EventHandlePacket e) {
      LocalPlayer player = mc.player;
      if (player == null) return;

      Packet<?> packet = e.getPacket();

      if (packet instanceof ClientboundSetEntityMotionPacket) {
         ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) packet;

         if (motionPacket.getId() != player.getId()) {
            return;
         }

         if (!delayedMode) {
             // 开始延迟模式
             delayedMode = true;
             receivedKnockbacks = 0;
             releasedKnockbacks = 0;
             knockbackPositions.clear();
             delayedPackets.clear();
             delayStartTime = System.currentTimeMillis();
             sendDebugMessage("§aDelay mode activated! Will delay knockback packets for " + delayTime.getCurrentValue() + "ms");
          }

          this.receivedKnockbacks++;
          this.knockbackPositions.add(this.delayedPackets.size());
          sendDebugMessage("§eReceived knockback #" + this.receivedKnockbacks + ", packet queued");
          e.setCancelled(true);
          this.delayedPackets.add(e.getPacket());
      }
   }

   /**
    * Delay模式tick处理
    */
   private void handleDelayTick(EventRunTicks eventRunTicks) {
      if (mc.player == null) return;

      // 检查是否需要重置延迟模式（如果受伤时间结束但还有未释放的包）
      if (delayedMode && mc.player.hurtTime == 0 && !delayedPackets.isEmpty()) {
         sendDebugMessage("§aHurt time ended, releasing all delayed packets.");
         releaseAllDelayedPackets();
         delayedMode = false;
      }

      // 处理延迟释放
       if (delayedMode) {
          long currentTime = System.currentTimeMillis();
          long elapsedTime = currentTime - delayStartTime;
          
          if (elapsedTime >= delayTime.getCurrentValue() && releasedKnockbacks < receivedKnockbacks) {
             packetsToRelease = (int) releaseCount.getCurrentValue();
             releaseDelayedPackets();
          }

          // 重置状态如果所有击退都已释放
          if (releasedKnockbacks >= receivedKnockbacks && receivedKnockbacks > 0) {
             delayedMode = false;
             sendDebugMessage("§aAll knockbacks released! Delay mode deactivated.");
          }
       }

      // 重置状态如果受伤时间结束
      if (delayedMode && mc.player.hurtTime == 0) {
         releaseAllDelayedPackets();
         delayedMode = false;
         sendDebugMessage("§aHurt time ended, releasing all delayed packets.");
      }
   }

   /**
    * 释放延迟的包
    */
   private void releaseDelayedPackets() {
      int packetsReleased = 0;
      
      while (!delayedPackets.isEmpty() && packetsReleased < packetsToRelease && releasedKnockbacks < receivedKnockbacks) {
         try {
            Packet<?> packet = delayedPackets.poll();
            if (packet != null && mc.getConnection() != null) {
               @SuppressWarnings("unchecked")
               Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> cp =
                       (Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>) packet;
               cp.handle(mc.getConnection());
               packetsReleased++;
            }
         } catch (Exception var6) {
            var6.printStackTrace();
         }
      }

      // 更新击退位置索引
      for (int i = 0; i < knockbackPositions.size(); i++) {
         knockbackPositions.set(i, Math.max(0, knockbackPositions.get(i) - packetsReleased));
      }

      // 移除已经释放的击退位置
      while (!knockbackPositions.isEmpty() && knockbackPositions.get(0) <= 0) {
         knockbackPositions.remove(0);
         releasedKnockbacks++;
      }

      sendDebugMessage("§aReleased " + packetsReleased + " packets, " + releasedKnockbacks + "/" + receivedKnockbacks + " knockbacks done");
   }

   /**
    * 释放所有延迟的包
    */
   private void releaseAllDelayedPackets() {
      while (!delayedPackets.isEmpty()) {
         try {
            Packet<?> packet = delayedPackets.poll();
            if (packet != null && mc.getConnection() != null) {
               @SuppressWarnings("unchecked")
               Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> cp =
                       (Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>) packet;
               cp.handle(mc.getConnection());
            }
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
      receivedKnockbacks = 0;
      releasedKnockbacks = 0;
      knockbackPositions.clear();
   }

   /**
    * GrimReduce模式包处理
    */
   private void handleGrimReducePacket(EventHandlePacket e) {
      LocalPlayer player = mc.player;
      if (player == null) return;

      Packet<?> packet = e.getPacket();

      if (packet instanceof ClientboundSetEntityMotionPacket) {
         ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) packet;

         if (motionPacket.getId() != player.getId()) {
            return;
         }

         ticksSinceVelocity = 0;

         if (!player.onGround()) {
            packets.add(e.getPacket());
            isKnockbacked = true;
            e.setCancelled(true);
         }
      }

      if (packet instanceof ClientboundPingPacket && isKnockbacked) {
         packets.add(e.getPacket());
         e.setCancelled(true);
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
         sendDebugMessage("Target not visible: " + target.getName().getString());
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
         mc.player.sendSystemMessage(Component.literal("§7[§b" + "NavenXD" + "§7] " + message));
      }
   }
}