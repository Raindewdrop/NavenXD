package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.MinecraftAccessor;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.HitResult.Type;
import java.util.Random;

@ModuleInfo(
   name = "AutoClicker",
   description = "Automatically clicks for you",
   category = Category.COMBAT
)
public class AutoClicker extends Module {
   private final FloatValue cps = ValueBuilder.create(this, "CPS")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(5.0F)
      .setMaxFloatValue(20.0F)
      .build()
      .getFloatValue();
   private final BooleanValue itemCheck = ValueBuilder.create(this, "Item Check").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue fluctuateCps = ValueBuilder.create(this, "Fluctuate CPS").setDefaultBooleanValue(false).build().getBooleanValue();
   private final FloatValue minCps = ValueBuilder.create(this, "Min CPS")
      .setDefaultFloatValue(8.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(5.0F)
      .setMaxFloatValue(20.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue())
      .build()
      .getFloatValue();
   private final FloatValue maxCps = ValueBuilder.create(this, "Max CPS")
      .setDefaultFloatValue(12.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(5.0F)
      .setMaxFloatValue(20.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue())
      .build()
      .getFloatValue();
   private final FloatValue fluctuateAmount = ValueBuilder.create(this, "Fluctuate Amount")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue())
      .build()
      .getFloatValue();
   private final FloatValue rampUpTime = ValueBuilder.create(this, "Ramp Up Time (s)")
      .setDefaultFloatValue(2.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(10.0F)
      .setVisibility(() -> fluctuateCps.getCurrentValue())
      .build()
      .getFloatValue();
   
   private float counter = 0.0F;
   
   // CPS浮动相关变量
   private float currentCps = 0.0F;
   private long lastFluctuationTime = 0L;
   private boolean increasing = true;
   private Random random = new Random();
   private long rampUpStartTime = 0L;
   private boolean isRampingUp = false;

   @Override
   public void onEnable() {
      super.onEnable();
      this.currentCps = this.minCps.getCurrentValue();
      this.lastFluctuationTime = System.currentTimeMillis();
      this.rampUpStartTime = System.currentTimeMillis();
      this.isRampingUp = true;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         // 更新CPS浮动
         if (this.fluctuateCps.getCurrentValue()) {
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
                 }
             } else {
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
         }
         
         MinecraftAccessor accessor = (MinecraftAccessor)mc;
         Item item = mc.player.getMainHandItem().getItem();
         if (mc.options.keyAttack.isDown()
            && (item instanceof SwordItem || item instanceof AxeItem || !this.itemCheck.getCurrentValue())
            && mc.hitResult.getType() != Type.BLOCK) {
            
            // 使用浮动CPS或固定CPS
            float effectiveCps = this.fluctuateCps.getCurrentValue() ? this.currentCps : this.cps.getCurrentValue();
            this.counter = this.counter + effectiveCps / 20.0F;
            
            if (this.counter >= 1.0F) {
               accessor.setMissTime(0);
               KeyMapping.click(mc.options.keyAttack.getKey());
               this.counter--;
            }
         } else {
            this.counter = 0.0F;
         }
      }
   }
}