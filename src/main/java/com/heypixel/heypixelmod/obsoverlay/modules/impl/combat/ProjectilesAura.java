package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Iterator;
import java.util.Random;

@ModuleInfo(
   name = "ProjectilesAura",
   description = "Throw projectiles at other players.",
   category = Category.COMBAT
)
public class ProjectilesAura extends Module {
   private static final Random random = new Random();
   private final Minecraft mc = Minecraft.getInstance();
   private final BooleanValue onlyWhenCombat = ValueBuilder.create(this, "Only during combat").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue legitMode = ValueBuilder.create(this, "Legit").setDefaultBooleanValue(false).build().getBooleanValue();
   private final FloatValue prepareTimeMin = ValueBuilder.create(this, "Preparation time Min (ms)").setDefaultFloatValue(100.0F).setFloatStep(50.0F).setMinFloatValue(0.0F).setMaxFloatValue(1000.0F).build().getFloatValue();
   private final FloatValue prepareTimeMax = ValueBuilder.create(this, "Preparation time Max (ms)").setDefaultFloatValue(300.0F).setFloatStep(50.0F).setMinFloatValue(0.0F).setMaxFloatValue(1000.0F).build().getFloatValue();
   private final FloatValue throwWaitTimeMin = ValueBuilder.create(this, "Throw wait time Min (ms)").setDefaultFloatValue(50.0F).setFloatStep(20.0F).setMinFloatValue(0.0F).setMaxFloatValue(500.0F).build().getFloatValue();
   private final FloatValue throwWaitTimeMax = ValueBuilder.create(this, "Throw wait time Max (ms)").setDefaultFloatValue(200.0F).setFloatStep(20.0F).setMinFloatValue(0.0F).setMaxFloatValue(500.0F).build().getFloatValue();
   private final FloatValue switchBackTimeMin = ValueBuilder.create(this, "Switch back time Min (ms)").setDefaultFloatValue(300.0F).setFloatStep(50.0F).setMinFloatValue(10.0F).setMaxFloatValue(2000.0F).build().getFloatValue();
   private final FloatValue switchBackTimeMax = ValueBuilder.create(this, "Switch back time Max (ms)").setDefaultFloatValue(700.0F).setFloatStep(50.0F).setMinFloatValue(10.0F).setMaxFloatValue(2000.0F).build().getFloatValue();
   private final FloatValue cooldownTimeMin = ValueBuilder.create(this, "Cooldown time Min (ms)").setDefaultFloatValue(800.0F).setFloatStep(100.0F).setMinFloatValue(100.0F).setMaxFloatValue(5000.0F).build().getFloatValue();
   private final FloatValue cooldownTimeMax = ValueBuilder.create(this, "Cooldown time Max (ms)").setDefaultFloatValue(1500.0F).setFloatStep(100.0F).setMinFloatValue(100.0F).setMaxFloatValue(5000.0F).build().getFloatValue();
   private final TimeHelper timer = new TimeHelper();
   private final TimeHelper cooldownTimer = new TimeHelper();
   private int originalSlot = -1;
   private int throwableSlot = -1;
   private ProjectilesAura.State state;
   private Entity target;
   private long currentStageTime;

   public ProjectilesAura() {
      this.state = ProjectilesAura.State.IDLE;
      this.target = null;
   }

   public void onEnable() {
      this.state = ProjectilesAura.State.IDLE;
      this.originalSlot = -1;
      this.throwableSlot = -1;
      this.target = null;
      this.timer.reset();
      this.cooldownTimer.reset();
      this.currentStageTime = 0L;
      super.onEnable();
   }

   @EventTarget
   public void onMotion(EventMotion event) {
      LocalPlayer player = this.mc.player;
      if (event.getType() == EventType.PRE && player != null && this.mc.level != null && this.mc.gameMode != null) {
         if (this.cooldownTimer.delay((double)this.currentStageTime) || this.state != ProjectilesAura.State.COOLDOWN) {
            if (this.onlyWhenCombat.getCurrentValue() && !this.isCombatModuleActive()) {
               this.resetState();
            } else {
               this.target = this.getValidTarget();
               if (this.target == null) {
                  this.resetState();
               } else {
                  this.handleThrowing();
               }
            }
         }
      }
   }

   private void handleThrowing() {
      LocalPlayer player = this.mc.player;
      if (player != null) {
         switch(this.state) {
         case IDLE:
            this.throwableSlot = this.findThrowableSlot();
            if (this.throwableSlot == -1 && !this.isThrowable(player.getOffhandItem())) {
               this.resetState();
            } else {
               this.originalSlot = player.getInventory().selected;
               this.currentStageTime = this.getRandomTime(this.prepareTimeMin.getCurrentValue(), this.prepareTimeMax.getCurrentValue());
               this.timer.reset();
               this.state = ProjectilesAura.State.PREPARING;
            }
            break;
         case PREPARING:
            if (this.timer.delay((double)this.currentStageTime)) {
               if (this.isThrowable(player.getOffhandItem())) {
                  this.currentStageTime = this.getRandomTime(this.throwWaitTimeMin.getCurrentValue(), this.throwWaitTimeMax.getCurrentValue());
                  this.timer.reset();
                  this.state = ProjectilesAura.State.SWITCHED;
               } else if (this.throwableSlot != -1 && this.throwableSlot != this.originalSlot) {
                  this.switchToThrowable(player);
                  this.state = ProjectilesAura.State.SWITCHING_TO;
                  this.timer.reset();
                  this.currentStageTime = 50L;
               } else {
                  this.resetState();
               }
            }
            break;
         case SWITCHING_TO:
            if (this.timer.delay((double)this.currentStageTime)) {
               this.state = ProjectilesAura.State.CHECKING_THROWABLE;
            }
            break;
         case CHECKING_THROWABLE:
            if (this.isHoldingThrowable(player)) {
               this.currentStageTime = this.getRandomTime(this.throwWaitTimeMin.getCurrentValue(), this.throwWaitTimeMax.getCurrentValue());
               this.timer.reset();
               this.state = ProjectilesAura.State.SWITCHED;
            } else {
               this.switchToThrowable(player);
               this.timer.reset();
               this.currentStageTime = 50L;
               this.state = ProjectilesAura.State.SWITCHING_TO;
            }
            break;
         case SWITCHED:
            if (this.timer.delay((double)this.currentStageTime)) {
               InteractionHand hand = this.isThrowable(player.getOffhandItem()) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
               if (this.legitMode.getCurrentValue()) {
                  this.simulateRightClick();
               } else {
                  PacketUtils.sendSequencedPacket((id) -> {
                     return new ServerboundUseItemPacket(hand, id);
                  });
                  player.swing(hand);
               }

               this.state = ProjectilesAura.State.THROWING;
               this.timer.reset();
               this.currentStageTime = 50L;
            }
            break;
         case THROWING:
            if (this.timer.delay((double)this.currentStageTime)) {
               if (this.originalSlot != -1 && this.originalSlot != player.getInventory().selected && !this.isThrowable(player.getOffhandItem())) {
                  this.currentStageTime = this.getRandomTime(this.switchBackTimeMin.getCurrentValue(), this.switchBackTimeMax.getCurrentValue());
                  this.timer.reset();
                  this.state = ProjectilesAura.State.SWITCHING_BACK;
               } else {
                  this.finishThrow();
               }
            }
            break;
         case SWITCHING_BACK:
            if (this.timer.delay((double)this.currentStageTime) && this.originalSlot != -1) {
               this.switchToOriginal(player);
               this.timer.reset();
               this.currentStageTime = 50L;
               this.state = ProjectilesAura.State.CHECKING_BACK;
            }
            break;
         case CHECKING_BACK:
            if (player.getInventory().selected == this.originalSlot) {
               this.finishThrow();
            } else {
               this.switchToOriginal(player);
               this.timer.reset();
               this.currentStageTime = 50L;
               this.state = ProjectilesAura.State.SWITCHING_BACK;
            }
            break;
         case COOLDOWN:
            this.state = ProjectilesAura.State.IDLE;
         }

      }
   }

   private void switchToThrowable(LocalPlayer player) {
      if (this.legitMode.getCurrentValue()) {
         this.simulateKeyPress(this.getSlotKey(this.throwableSlot));
      } else {
         player.getInventory().selected = this.throwableSlot;
      }

   }

   private void switchToOriginal(LocalPlayer player) {
      if (this.legitMode.getCurrentValue()) {
         this.simulateKeyPress(this.getSlotKey(this.originalSlot));
      } else {
         player.getInventory().selected = this.originalSlot;
      }

   }

   private void finishThrow() {
      this.currentStageTime = this.getRandomTime(this.cooldownTimeMin.getCurrentValue(), this.cooldownTimeMax.getCurrentValue());
      this.cooldownTimer.reset();
      this.state = ProjectilesAura.State.COOLDOWN;
   }

   private void resetState() {
      this.state = ProjectilesAura.State.IDLE;
      this.originalSlot = -1;
      this.throwableSlot = -1;
      this.target = null;
      this.timer.reset();
   }

   private long getRandomTime(float min, float max) {
      return min >= max ? (long)min : (long)(min + random.nextFloat() * (max - min));
   }

   private int getSlotKey(int slot) {
      return 49 + slot;
   }

   private void simulateKeyPress(int keyCode) {
      KeyMapping key = this.mc.options.keyHotbarSlots[keyCode - 49];
      if (key != null) {
         InputConstants.Key inputKey = key.getKey();
         int pressDelay = random.nextInt(20) + 10;
         this.mc.execute(() -> {
            try {
               KeyMapping.set(inputKey, true);
               KeyMapping.click(inputKey);
               long start = System.currentTimeMillis();

               while(System.currentTimeMillis() - start < (long)pressDelay) {
                  Thread.yield();
               }

               KeyMapping.set(inputKey, false);
            } catch (Exception var4) {
               var4.printStackTrace();
            }

         });
      }
   }

   private void simulateRightClick() {
      KeyMapping useKey = this.mc.options.keyUse;
      InputConstants.Key inputKey = useKey.getKey();
      int clickDelay = random.nextInt(30) + 20;
      this.mc.execute(() -> {
         try {
            KeyMapping.set(inputKey, true);
            KeyMapping.click(inputKey);
            long start = System.currentTimeMillis();

            while(System.currentTimeMillis() - start < (long)clickDelay) {
               Thread.yield();
            }

            KeyMapping.set(inputKey, false);
         } catch (Exception var4) {
            var4.printStackTrace();
         }

      });
   }

   private boolean isCombatModuleActive() {
      Module killaura = Naven.getInstance().getModuleManager().getModule(KillAura.class);
      Module aimAssist = Naven.getInstance().getModuleManager().getModule(AimAssist.class);
      return killaura != null && killaura.isEnabled() || aimAssist != null && aimAssist.isEnabled();
   }

   private Entity getValidTarget() {
      Module killaura = Naven.getInstance().getModuleManager().getModule(KillAura.class);
      if (killaura != null && killaura.isEnabled() && KillAura.targets != null && !KillAura.targets.isEmpty()) {
         Iterator<Entity> var2 = KillAura.targets.iterator();

         while(var2.hasNext()) {
            Entity entity = var2.next();
            if (this.isValidTarget(entity)) {
               return entity;
            }
         }
      }

      return null;
   }

   private boolean isValidTarget(Entity entity) {
      if (entity != null && entity != this.mc.player && entity.isAlive()) {
         double distance = entity.distanceTo(this.mc.player);
         return distance <= 10.0D && RotationUtils.inFoV(entity, 90.0F);
      } else {
         return false;
      }
   }

   private int findThrowableSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (this.isThrowable(stack)) {
            return i;
         }
      }

      return -1;
   }

   private boolean isThrowable(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         Item item = stack.getItem();
         return item == Items.SNOWBALL || item == Items.EGG || item == Items.FISHING_ROD;
      }
   }

   private boolean isHoldingThrowable(LocalPlayer player) {
      return this.isThrowable(player.getMainHandItem()) || this.isThrowable(player.getOffhandItem());
   }

   private enum State {
      IDLE,
      PREPARING,
      SWITCHING_TO,
      CHECKING_THROWABLE,
      SWITCHED,
      THROWING,
      SWITCHING_BACK,
      CHECKING_BACK,
      COOLDOWN
   }
}