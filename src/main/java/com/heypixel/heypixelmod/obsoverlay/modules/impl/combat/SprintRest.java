package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.world.entity.player.Player;

@ModuleInfo(
        name = "SprintReset",
        description = "Cancel sprint when damaged and immediately resume",
        category = Category.COMBAT
)
public class SprintRest extends Module {
    private float lastHealth = 0.0F;
    private int sprintCooldown = 0;
    private boolean wasSprinting = false;

    public FloatValue delay = ValueBuilder.create(this, "Resume Delay")
            .setDefaultFloatValue(2.0F) // 默认2 ticks (100ms)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F) // 最大20 ticks (1秒)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    public FloatValue minDamage = ValueBuilder.create(this, "Min Damage")
            .setDefaultFloatValue(1.0F) // 默认至少受到1点伤害才触发
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F) // 最大20点伤害
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.lastHealth = mc.player.getHealth();
            this.sprintCooldown = 0;
            this.wasSprinting = false;
        }
    }

    @Override
    public void onDisable() {
        // 确保恢复正常的疾跑状态
        if (mc.player != null && this.wasSprinting) {
            mc.options.keySprint.setDown(true);
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        float currentHealth = mc.player.getHealth();

        // 检测是否受伤
        if (currentHealth < this.lastHealth) {
            float damageTaken = this.lastHealth - currentHealth;

            // 检查伤害是否达到最小触发值
            if (damageTaken >= this.minDamage.getCurrentValue()) {
                // 如果玩家正在疾跑，取消疾跑并设置冷却时间
                if (mc.player.isSprinting()) {
                    this.wasSprinting = true;
                    mc.options.keySprint.setDown(false);
                    mc.player.setSprinting(false);
                    this.sprintCooldown = (int) this.delay.getCurrentValue();
                }
            }
        }

        this.lastHealth = currentHealth;

        // 处理疾跑冷却
        if (this.sprintCooldown > 0) {
            this.sprintCooldown--;

            // 冷却结束后恢复疾跑
            if (this.sprintCooldown <= 0 && this.wasSprinting) {
                mc.options.keySprint.setDown(true);
                this.wasSprinting = false;
            }
        }
    }
}