package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

import com.heypixel.heypixelmod.obsoverlay.annotations.FlowExclude;
import com.heypixel.heypixelmod.obsoverlay.annotations.ParameterObfuscationExclude;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateFoV;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateHeldItem;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.FallingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RayTraceUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "Scaffold",
   description = "Automatically places blocks under you",
   category = Category.MOVEMENT
)
public class Scaffold extends Module {
   public static final List<Block> blacklistedBlocks = Arrays.asList(
      Blocks.AIR,
      Blocks.WATER,
      Blocks.LAVA,
      Blocks.ENCHANTING_TABLE,
      Blocks.GLASS_PANE,
      Blocks.GLASS_PANE,
      Blocks.IRON_BARS,
      Blocks.SNOW,
      Blocks.COAL_ORE,
      Blocks.DIAMOND_ORE,
      Blocks.EMERALD_ORE,
      Blocks.CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.TORCH,
      Blocks.ANVIL,
      Blocks.TRAPPED_CHEST,
      Blocks.NOTE_BLOCK,
      Blocks.JUKEBOX,
      Blocks.TNT,
      Blocks.GOLD_ORE,
      Blocks.IRON_ORE,
      Blocks.LAPIS_ORE,
      Blocks.STONE_PRESSURE_PLATE,
      Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
      Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
      Blocks.STONE_BUTTON,
      Blocks.LEVER,
      Blocks.TALL_GRASS,
      Blocks.TRIPWIRE,
      Blocks.TRIPWIRE_HOOK,
      Blocks.RAIL,
      Blocks.CORNFLOWER,
      Blocks.RED_MUSHROOM,
      Blocks.BROWN_MUSHROOM,
      Blocks.VINE,
      Blocks.SUNFLOWER,
      Blocks.LADDER,
      Blocks.FURNACE,
      Blocks.SAND,
      Blocks.CACTUS,
      Blocks.DISPENSER,
      Blocks.DROPPER,
      Blocks.CRAFTING_TABLE,
      Blocks.COBWEB,
      Blocks.PUMPKIN,
      Blocks.COBBLESTONE_WALL,
      Blocks.OAK_FENCE,
      Blocks.REDSTONE_TORCH,
      Blocks.FLOWER_POT
   );
   public Vector2f correctRotation = new Vector2f();
   public Vector2f rots = new Vector2f();
   public Vector2f lastRots = new Vector2f();
   private int offGroundTicks = 0;
   public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Normal", "Telly Bridge", "Keep Y").build().getModeValue();
   public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.mode.isCurrentMode("Normal"))
      .build()
      .getBooleanValue();
   public BooleanValue sneak = ValueBuilder.create(this, "Sneak").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue snap = ValueBuilder.create(this, "Snap")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.mode.isCurrentMode("Normal"))
      .build()
      .getBooleanValue();
   public BooleanValue hideSnap = ValueBuilder.create(this, "Hide Snap Rotation")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue())
      .build()
      .getBooleanValue();
   public BooleanValue renderItemSpoof = ValueBuilder.create(this, "Render Item Spoof").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue keepFoV = ValueBuilder.create(this, "Keep FoV").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
      .setDefaultFloatValue(1.15F)
      .setMaxFloatValue(2.0F)
      .setMinFloatValue(1.0F)
      .setFloatStep(0.05F)
      .setVisibility(() -> this.keepFoV.getCurrentValue())
      .build()
      .getFloatValue();
   public BooleanValue showBlockCount = ValueBuilder.create(this, "Show Block Count").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue progressBarDistance = ValueBuilder.create(this, "Progress Bar Distance")
      .setDefaultFloatValue(15.0F)
      .setMaxFloatValue(50.0F)
      .setMinFloatValue(-50.0F)
      .setFloatStep(1.0F)
      .setVisibility(() -> this.showBlockCount.getCurrentValue())
      .build()
      .getFloatValue();
   public BooleanValue showBlockCountText = ValueBuilder.create(this, "Show Block Count Text").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue blockCountTextDistance = ValueBuilder.create(this, "Block Count Text Distance")
      .setDefaultFloatValue(25.0F)
      .setMaxFloatValue(60.0F)
      .setMinFloatValue(-60.0F)
      .setFloatStep(1.0F)
      .setVisibility(() -> this.showBlockCount.getCurrentValue() && this.showBlockCountText.getCurrentValue())
      .build()
      .getFloatValue();
   // 副手支持选项
   public BooleanValue offhandSupport = ValueBuilder.create(this, "Offhand Support").setDefaultBooleanValue(true).build().getBooleanValue();
   // 放置时机优化选项
   public BooleanValue earlyPlacement = ValueBuilder.create(this, "Early Placement").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue placementTiming = ValueBuilder.create(this, "Placement Timing")
      .setDefaultFloatValue(0.95F)
      .setMaxFloatValue(1.0F)
      .setMinFloatValue(0.1F)
      .setFloatStep(0.05F)
      .setVisibility(() -> this.earlyPlacement.getCurrentValue())
      .build()
      .getFloatValue();
   // Telly Bridge Airrot选项
   public FloatValue airrotTicks = ValueBuilder.create(this, "AirRot Ticks")
      .setDefaultFloatValue(1.0F)
      .setMaxFloatValue(5.0F)
      .setMinFloatValue(0.0F)
      .setFloatStep(0.5F)
      .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
      .build()
      .getFloatValue();
   // 不渲染挥手动画选项
   public BooleanValue noSwing = ValueBuilder.create(this, "No Swing")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   
   int oldSlot;
   private Scaffold.BlockPosWithFacing pos;
   private int lastSneakTicks;
   public int baseY = -1;
   private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);
   private static final int mainColor = new Color(150, 45, 45, 255).getRGB();
   private int startBlockCount = 0;
   private int placedBlocks = 0;
   // 跟踪当前使用的手
   private InteractionHand currentHand = InteractionHand.MAIN_HAND;
   // 放置冷却
   private int placeCooldown = 0;
   // 记录是否使用副手
   private boolean usingOffhand = false;
   // 记录失败的放置尝试次数
   private int failedPlacementAttempts = 0;
   public static boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
         return false;
      } else if (!InventoryUtils.isItemValid(stack)) {
         return false;
      } else {
         String string = stack.getDisplayName().getString();
         if (string.contains("Click") || string.contains("点击")) {
            return false;
         } else if (stack.getItem() instanceof ItemNameBlockItem) {
            return false;
         } else {
            Block block = ((BlockItem)stack.getItem()).getBlock();
            if (block instanceof FlowerBlock) {
               return false;
            } else if (block instanceof BushBlock) {
               return false;
            } else if (block instanceof FungusBlock) {
               return false;
            } else if (block instanceof CropBlock) {
               return false;
            } else {
               return block instanceof SlabBlock ? false : !blacklistedBlocks.contains(block);
            }
         }
      }
   }

   public static boolean isOnBlockEdge(float sensitivity) {
      return !mc.level
         .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
         .iterator()
         .hasNext();
   }

   @EventTarget
   public void onFoV(EventUpdateFoV e) {
      if (this.keepFoV.getCurrentValue() && MoveUtils.isMoving()) {
         e.setFov(this.fov.getCurrentValue() + (float)PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
      }
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.oldSlot = mc.player.getInventory().selected;
         this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
         this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
         this.pos = null;
         this.baseY = 10000;
         this.startBlockCount = this.getHotbarBlockCount();
         this.placedBlocks = 0;
         this.placeCooldown = 0;
         this.currentHand = InteractionHand.MAIN_HAND;
         this.usingOffhand = false;
         this.failedPlacementAttempts = 0;
         
         // 设置模式后缀
         updateModeSuffix();
      }
   }
   
   private void updateModeSuffix() {
      String currentMode = this.mode.getCurrentMode();
      switch (currentMode) {
         case "Normal":
            this.setSuffix("Normal");
            break;
         case "Telly Bridge":
            this.setSuffix("Telly Bridge");
            break;
         case "Keep Y":
            this.setSuffix("KeepY");
            break;
         default:
            this.setSuffix("");
      }
   }
   @Override
   public void onDisable() {
      boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
      boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
      mc.options.keyJump.setDown(isHoldingJump);
      mc.options.keyShift.setDown(isHoldingShift);
      mc.options.keyUse.setDown(false);
      // 只在不是使用副手时才恢复原槽位
      if (!this.usingOffhand) {
         mc.player.getInventory().selected = this.oldSlot;
      }
   }

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      // 只在主手搭路且开启物品欺骗渲染时才进行欺骗
      if (this.renderItemSpoof.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND && !this.usingOffhand) {
         e.setItem(mc.player.getInventory().getItem(this.oldSlot));
      }
      // 当使用副手搭路时，主手物品正常渲染，不需要任何修改
   }

   @EventTarget
   public void onEventEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
         // 减少放置冷却
         if (this.placeCooldown > 0) {
            this.placeCooldown--;
         }
         
         // 检查主手和副手是否有方块
         boolean hasMainHandBlock = isValidStack(mc.player.getMainHandItem());
         boolean hasOffhandBlock = this.offhandSupport.getCurrentValue() && isValidStack(mc.player.getOffhandItem());
         
         // 决定使用哪只手
         this.usingOffhand = hasOffhandBlock && !hasMainHandBlock;
         this.currentHand = this.usingOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         
         // 只在不是使用副手时才切换主手槽位
         if (!this.usingOffhand) {
            int slotID = -1;
            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getItem(i);
               if (isValidStack(stack)) {
                  slotID = i;
                  break;
               }
            }
            
            if (slotID != -1 && mc.player.getInventory().selected != slotID) {
               mc.player.getInventory().selected = slotID;
            }
         }

         if (mc.player.onGround()) {
            this.offGroundTicks = 0;
            this.failedPlacementAttempts = 0; // 重置失败计数
         } else {
            this.offGroundTicks++;
         }

         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
         if (this.baseY == -1
            || this.baseY > (int)Math.floor(mc.player.getY()) - 1
            || mc.player.onGround()
            || !PlayerUtils.movementInput()
            || isHoldingJump
            || this.mode.isCurrentMode("Normal")) {
            this.baseY = (int)Math.floor(mc.player.getY()) - 1;
         }

         this.getBlockPos();
         if (this.pos != null) {
            this.correctRotation = this.getPlayerYawRotation();
            if (this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue()) {
               this.rots.setX(this.correctRotation.getX());
            } else {
               this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), this.correctRotation.getX()));
            }

            this.rots.setY(this.correctRotation.getY());
            
            // 早期放置检测
            if (this.earlyPlacement.getCurrentValue() && this.placeCooldown == 0) {
                FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
                fallingPlayer.calculate(2);
                
                // 计算预计落地位置
                double predictedY = fallingPlayer.y;
                double currentY = mc.player.getY();
                double fallDistance = currentY - predictedY;
                
                // 根据设置的时间阈值判断是否应该放置
                if (fallDistance > this.placementTiming.getCurrentValue()) {
                    this.placeBlock();
                }
            }
         }

         if (this.sneak.getCurrentValue()) {
            this.lastSneakTicks++;
            // 将蹲下时间从3tick缩短为1tick
            if (this.lastSneakTicks == 18) {
               if (mc.player.isSprinting()) {
                  mc.options.keySprint.setDown(false);
                  mc.player.setSprinting(false);
               }
               mc.options.keyShift.setDown(true);
            } else if (this.lastSneakTicks == 19) { // 立即在下一tick释放
               mc.options.keyShift.setDown(false);
               this.lastSneakTicks = 0; // 重置计数器
            }
         }

         if (this.mode.isCurrentMode("Telly Bridge")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            
            // 使用Airrot选项控制滞空tick数
            int airrotThreshold = (int) this.airrotTicks.getCurrentValue();
            if (this.offGroundTicks < airrotThreshold && PlayerUtils.movementInput()) {
               // 在达到滞空tick数之前，保持当前视角
               this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), mc.player.getYRot()));
               this.lastRots.set(this.rots.getX(), this.rots.getY());
               return;
            }
         } else if (this.mode.isCurrentMode("Keep Y")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
         } else {
            if (this.eagle.getCurrentValue()) {
               mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
            }

            if (this.snap.getCurrentValue() && !isHoldingJump) {
               this.doSnap();
            }
         }

         this.lastRots.set(this.rots.getX(), this.rots.getY());
      }
   }

   private void doSnap() {
       boolean shouldPlaceBlock = false;
       HitResult objectPosition = RayTraceUtils.rayCast(1.0F, this.rots);
       if (objectPosition.getType() == Type.BLOCK) {
          BlockHitResult position = (BlockHitResult)objectPosition;
          if (position.getBlockPos().equals(this.pos) && position.getDirection() != Direction.UP) {
             shouldPlaceBlock = true;
          }
       }

       if (!shouldPlaceBlock) {
          this.rots.setX(mc.player.getYRot() + RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F);
       }
    }

   @EventTarget
    public void onClick(EventClick e) {
       e.setCancelled(true);
       if (mc.screen == null && mc.player != null && this.pos != null) {
          // 修改：Telly Bridge模式下也需要检查Airrot选项
          if (this.mode.isCurrentMode("Telly Bridge")) {
             boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
             mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
             
             // 使用Airrot选项控制滞空tick数
             int airrotThreshold = (int) this.airrotTicks.getCurrentValue();
             if (this.offGroundTicks < airrotThreshold) {
                return; // 未达到滞空tick数，不放置方块
             }
          } else if (this.mode.isCurrentMode("Telly Bridge") && this.offGroundTicks < 1) {
             return;
          }
          
          if (!this.checkPlace(this.pos)) {
             this.failedPlacementAttempts++; // 增加失败计数
             return;
          }

          // 正常放置
          this.placeBlock();
       }
    }

   private boolean checkPlace(Scaffold.BlockPosWithFacing data) {
      Vec3 center = new Vec3((double)data.position.getX() + 0.5, (double)((float)data.position.getY() + 0.5F), (double)data.position.getZ() + 0.5);
      Vec3 hit = center.add(
         new Vec3((double)data.facing.getNormal().getX() * 0.5, (double)data.facing.getNormal().getY() * 0.5, (double)data.facing.getNormal().getZ() * 0.5)
      );
      Vec3 relevant = hit.subtract(mc.player.getEyePosition());
      return relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(data.facing.getNormal().multiply(-1)).normalize()) >= 0.0;
   }

   private void placeBlock() {
      if (this.pos != null && isValidStack(this.currentHand == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem())) {
         Direction sbFace = this.pos.facing();
         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
         if (sbFace != null
            && (sbFace != Direction.UP || mc.player.onGround() || !PlayerUtils.movementInput() || isHoldingJump || this.mode.isCurrentMode("Normal"))
            && this.shouldBuild()) {
            InteractionResult result = mc.gameMode
               .useItemOn(mc.player, this.currentHand, new BlockHitResult(getVec3(this.pos.position(), sbFace), sbFace, this.pos.position(), false));
            if (result == InteractionResult.SUCCESS) {
               // 如果不开启No Swing选项，则渲染挥手动画
               if (!this.noSwing.getCurrentValue()) {
                  mc.player.swing(this.currentHand);
               }
               this.pos = null;
               this.placedBlocks++;
               this.placeCooldown = 2; // 设置放置冷却
               this.failedPlacementAttempts = 0; // 重置失败计数
            } else {
               this.failedPlacementAttempts++; // 增加失败计数
            }
         } else {
            this.failedPlacementAttempts++; // 增加失败计数
         }
      } else {
         this.failedPlacementAttempts++; // 增加失败计数
      }
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private Vector2f getPlayerYawRotation() {
      return mc.player != null && this.pos != null
         ? new Vector2f(RotationUtils.getRotations(this.pos.position(), 0.0F).getYaw(), RotationUtils.getRotations(this.pos.position(), 0.0F).getPitch())
         : new Vector2f(0.0F, 0.0F);
   }

   private boolean shouldBuild() {
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
      return mc.level.isEmptyBlock(playerPos) && 
             isValidStack(this.currentHand == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem());
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private void getBlockPos() {
       Vec3 baseVec = mc.player.getEyePosition().add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
       if (mc.player.getDeltaMovement().y < 0.01) {
          FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
          fallingPlayer.calculate(2);
          baseVec = new Vec3(baseVec.x, Math.max(fallingPlayer.y + (double)mc.player.getEyeHeight(), baseVec.y), baseVec.z);
       }

       BlockPos base = BlockPos.containing(baseVec.x, (double)((float)this.baseY + 0.1F), baseVec.z);
       int baseX = base.getX();
       int baseZ = base.getZ();
       
       // 优化搜索算法：优先检查玩家正下方和移动方向的位置
       if (!mc.level.getBlockState(base).entityCanStandOn(mc.level, base, mc.player)) {
          // 优先检查正下方
          if (this.checkBlock(baseVec, base)) {
             return;
          }
          
          // 根据移动方向优先检查前方位置
          Vec3 movement = mc.player.getDeltaMovement();
          float moveX = (float) movement.x;
          float moveZ = (float) movement.z;
          
          // 计算移动方向角度
          double moveAngle = Math.atan2(moveZ, moveX) * 180 / Math.PI;
          
          // 优先检查移动方向上的位置（包括斜向）
          for (int i = 1; i <= 3; i++) {
             // 检查8个主要方向
             for (int dir = 0; dir < 8; dir++) {
                double angle = dir * 45;
                double rad = angle * Math.PI / 180;
                
                int offsetX = (int) Math.round(Math.cos(rad) * i);
                int offsetZ = (int) Math.round(Math.sin(rad) * i);
                
                // 优先检查与移动方向相近的位置
                double angleDiff = Math.abs(moveAngle - angle);
                if (angleDiff > 180) angleDiff = 360 - angleDiff;
                
                // 如果方向与移动方向相差较大，降低优先级
                if (angleDiff > 90 && i > 1) continue;
                
                BlockPos forwardPos = new BlockPos(baseX + offsetX, this.baseY, baseZ + offsetZ);
                if (this.checkBlock(baseVec, forwardPos)) {
                   return;
                }
                
                // 检查移动方向的下方位置
                BlockPos forwardDownPos = new BlockPos(baseX + offsetX, this.baseY - 1, baseZ + offsetZ);
                if (this.checkBlock(baseVec, forwardDownPos)) {
                   return;
                }
             }
          }
          
          // 如果优先位置没有找到，再按照原来的算法搜索周围
          for (int d = 1; d <= 6; d++) {
             // 优先检查同一高度的位置
             if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY, baseZ + d))) {
                return;
             }
             if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY, baseZ - d))) {
                return;
             }
             if (this.checkBlock(baseVec, new BlockPos(baseX + d, this.baseY, baseZ))) {
                return;
             }
             if (this.checkBlock(baseVec, new BlockPos(baseX - d, this.baseY, baseZ))) {
                return;
             }
             
             // 然后检查向下的位置
             if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY - d, baseZ))) {
                return;
             }
             
             // 最后检查对角线位置
             for (int x = 1; x <= d; x++) {
                for (int z = 0; z <= d - x; z++) {
                   int y = d - x - z;

                   for (int rev1 = 0; rev1 <= 1; rev1++) {
                      for (int rev2 = 0; rev2 <= 1; rev2++) {
                         if (this.checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), this.baseY - y, baseZ + (rev2 == 0 ? z : -z)))) {
                            return;
                         }
                      }
                   }
                }
             }
          }
       }
    }

   private boolean checkBlock(Vec3 baseVec, BlockPos bp) {
      // 快速检查：如果方块不是空气，直接返回false
      if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
         return false;
      }
      
      // 优化：预计算中心点
      double centerX = bp.getX() + 0.5;
      double centerY = bp.getY() + 0.5;
      double centerZ = bp.getZ() + 0.5;
      Vec3 center = new Vec3(centerX, centerY, centerZ);

      // 优化：按方向优先级检查（先检查上方，然后是侧面）
      Direction[] priorityDirections = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
      
      // 根据移动方向调整优先级
      Vec3 movement = mc.player.getDeltaMovement();
      float moveX = (float) movement.x;
      float moveZ = (float) movement.z;
      
      // 如果玩家正在移动，提高与移动方向相符的侧面的优先级
      if (Math.abs(moveX) > 0.1 || Math.abs(moveZ) > 0.1) {
         // 确定主要移动方向
         if (Math.abs(moveX) > Math.abs(moveZ)) {
            // 主要移动方向为东西
            if (moveX > 0) {
               priorityDirections = new Direction[]{Direction.WEST, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.DOWN};
            } else {
               priorityDirections = new Direction[]{Direction.EAST, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.DOWN};
            }
         } else {
            // 主要移动方向为南北
            if (moveZ > 0) {
               priorityDirections = new Direction[]{Direction.NORTH, Direction.UP, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.DOWN};
            } else {
               priorityDirections = new Direction[]{Direction.SOUTH, Direction.UP, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.DOWN};
            }
         }
      }
      
      for (Direction sbface : priorityDirections) {
         Vec3 hit = center.add(
            new Vec3(sbface.getNormal().getX() * 0.5, sbface.getNormal().getY() * 0.5, sbface.getNormal().getZ() * 0.5)
         );
         
         BlockPos neighborPos = bp.relative(sbface);
         
         // 快速检查：如果邻居方块不可站立，跳过
         if (!mc.level.getBlockState(neighborPos).entityCanStandOnFace(mc.level, neighborPos, mc.player, sbface)) {
            continue;
         }
         
         Vec3 relevant = hit.subtract(baseVec);
         double distanceSq = relevant.lengthSqr();
         
         // 优化：提前检查距离，避免不必要的归一化计算
         if (distanceSq <= 20.25) {
            Vec3 normalizedRelevant = relevant.normalize();
            Vec3 faceNormal = Vec3.atLowerCornerOf(sbface.getNormal()).normalize();
            
            if (normalizedRelevant.dot(faceNormal) >= 0.0) {
               this.pos = new Scaffold.BlockPosWithFacing(neighborPos, sbface.getOpposite());
               return true;
            }
         }
      }

      return false;
   }

   @FlowExclude
   @ParameterObfuscationExclude
   public static Vec3 getVec3(BlockPos pos, Direction face) {
      double x = (double)pos.getX() + 0.5;
      double y = (double)pos.getY() + 0.5;
      double z = (double)pos.getZ() + 0.5;
      if (face != Direction.UP && face != Direction.DOWN) {
         y += 0.08;
      } else {
         x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
         z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.WEST || face == Direction.EAST) {
         z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.SOUTH || face == Direction.NORTH) {
         x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      return new Vec3(x, y, z);
   }

   private int getHotbarBlockCount() {
      int count = 0;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
            count += stack.getCount();
         }
      }
      
      // 包括副手方块
      if (this.offhandSupport.getCurrentValue()) {
         ItemStack offhandStack = mc.player.getOffhandItem();
         if (offhandStack.getItem() instanceof BlockItem && isValidStack(offhandStack)) {
            count += offhandStack.getCount();
         }
      }
      
      return count;
   }

   private int getCurrentBlockCount() {
      int currentHotbarCount = this.getHotbarBlockCount();
      // 如果实时数量大于开启scaffold时的方块数量，就更新最大数量
      if (currentHotbarCount > this.startBlockCount) {
         this.startBlockCount = currentHotbarCount;
      }
      return currentHotbarCount;
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      if (this.showBlockCount.getCurrentValue()) {
         int centerX = mc.getWindow().getGuiScaledWidth() / 2;
         int centerY = mc.getWindow().getGuiScaledHeight() / 2;
         
         // 更新进度条
         this.progress.update(true);
         int currentCount = this.getCurrentBlockCount();
         float progressValue = this.startBlockCount > 0 ? (float)currentCount / this.startBlockCount * 100.0F : 0.0F;
         this.progress.target = Mth.clamp(progressValue, 0.0F, 100.0F);
         
         // 绘制进度条
         int progressBarY = centerY - (int)this.progressBarDistance.getCurrentValue();
         RenderUtils.drawRoundedRect(e.getStack(), (float)(centerX - 50), (float)progressBarY, 100.0F, 5.0F, 2.0F, Integer.MIN_VALUE);
         RenderUtils.drawRoundedRect(e.getStack(), (float)(centerX - 50), (float)progressBarY, this.progress.value, 5.0F, 2.0F, mainColor);
         
         // 绘制方块数量文字
         if (this.showBlockCountText.getCurrentValue()) {
            int textY = progressBarY + (int)this.blockCountTextDistance.getCurrentValue();
            String text = currentCount + "/" + this.startBlockCount + " Blocks";
            Fonts.harmony.render(e.getStack(), text, (double)(centerX - Fonts.harmony.getWidth(text, 0.35) / 2.0F), (double)textY, Color.WHITE, true, 0.35);
         }
      }
   }
   
   public static record BlockPosWithFacing(BlockPos position, Direction facing) {
   }
}