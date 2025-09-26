package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "JumpRest",
        description = "by StarSky",
        category = Category.COMBAT
)
public class GrimReduce extends Module {

    private final BooleanValue debugMessages = ValueBuilder.create(this, "Debug")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    // 状态跟踪
    private boolean velocityTriggered = false;
    private int lastHurtTime = 0;
    private DamageSource lastDamageSource = null;
    private final Minecraft mc = Minecraft.getInstance();

    @EventTarget
    public void onVelocityPacket(ClientboundSetEntityMotionPacket event) {
        LocalPlayer player = mc.player;
        if (player == null || event.getId() != player.getId()) return;

        velocityTriggered = true;
        sendDebugMessage("Received the retreat package");
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 只在受伤状态处理
        if (player.hurtTime == 0) {
            velocityTriggered = false;
            lastDamageSource = null;
            return;
        }

        // 记录伤害来源（如果有）
        if (player.hurtTime == 10) { // hurtTime为10时是最新的伤害
            lastDamageSource = player.getLastDamageSource();
        }

        // 检查是否应该跳过处理（包括摔伤和火伤）
        if (shouldSkipVelocityReduction(player)) {
            sendDebugMessage("Skip" + getSkipReason(player));
            return;
        }

        // 跳跃逻辑 (原版Hylex)
        if (player.hurtTime > 5 && player.hurtTime != lastHurtTime && player.onGround()) {
            player.jumpFromGround();
            sendDebugMessage("Executing jump reset hurtTime=" + player.hurtTime);
        }

        // 速度减少逻辑 (原版Hylex乘数)
        if (velocityTriggered && player.hurtTime != lastHurtTime) {
            Vec3 velocity = player.getDeltaMovement();

            switch (player.hurtTime) {
                case 9:
                    player.setDeltaMovement(velocity.multiply(0.8, 1.0, 0.8));
                    sendDebugMessage("Apply hurtTime=9 multiplier (0.8)");
                    break;
                case 8:
                    player.setDeltaMovement(velocity.multiply(0.11, 1.0, 0.11));
                    sendDebugMessage("Apply hurtTime=8 multiplier (0.11)");
                    break;
                case 7:
                    player.setDeltaMovement(velocity.multiply(0.4, 1.0, 0.4));
                    sendDebugMessage("Apply hurtTime=7 multiplier (0.4)");
                    break;
                case 4:
                    player.setDeltaMovement(velocity.multiply(0.37, 1.0, 0.37));
                    sendDebugMessage("Apply hurtTime=4 multiplier (0.37)");
                    break;
            }
        }

        lastHurtTime = player.hurtTime;
    }

    /**
     * 检查是否应该跳过击退减少处理
     * 包括摔伤和火伤的情况
     */
    private boolean shouldSkipVelocityReduction(LocalPlayer player) {
        // 不在地面时不生效
        if (!player.onGround()) {
            return true;
        }

        // 检查伤害来源类型
        if (lastDamageSource != null) {
            // 火伤不生效
            if (lastDamageSource.is(DamageTypes.IN_FIRE) ||
                    lastDamageSource.is(DamageTypes.ON_FIRE) ||
                    lastDamageSource.is(DamageTypes.LAVA) ||
                    lastDamageSource.is(DamageTypes.HOT_FLOOR)) {
                return true;
            }

            // 摔伤不生效
            if (lastDamageSource.is(DamageTypes.FALL)) {
                return true;
            }

            // 其他环境伤害也不生效
            if (lastDamageSource.is(DamageTypes.CACTUS) ||
                    lastDamageSource.is(DamageTypes.DROWN) ||
                    lastDamageSource.is(DamageTypes.STARVE) ||
                    lastDamageSource.is(DamageTypes.WITHER) ||
                    lastDamageSource.is(DamageTypes.FLY_INTO_WALL) ||
                    lastDamageSource.is(DamageTypes.DRY_OUT) ||
                    lastDamageSource.is(DamageTypes.FREEZE)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取跳过原因（用于调试信息）
     */
    private String getSkipReason(LocalPlayer player) {
        if (!player.onGround()) {
            return "Not on ground";
        }

        if (lastDamageSource != null) {
            // 火伤相关
            if (lastDamageSource.is(DamageTypes.IN_FIRE)) {
                return "Fire damage";
            }
            if (lastDamageSource.is(DamageTypes.ON_FIRE)) {
                return "On fire damage";
            }
            if (lastDamageSource.is(DamageTypes.LAVA)) {
                return "Lava damage";
            }
            if (lastDamageSource.is(DamageTypes.HOT_FLOOR)) {
                return "Hot floor damage";
            }

            // 摔伤
            if (lastDamageSource.is(DamageTypes.FALL)) {
                return "Fall damage";
            }

            // 其他环境伤害
            if (lastDamageSource.is(DamageTypes.CACTUS)) {
                return "Cactus damage";
            }
            if (lastDamageSource.is(DamageTypes.DROWN)) {
                return "Drowning damage";
            }
            if (lastDamageSource.is(DamageTypes.STARVE)) {
                return "Starve damage";
            }
            if (lastDamageSource.is(DamageTypes.WITHER)) {
                return "Wither damage";
            }
            if (lastDamageSource.is(DamageTypes.FLY_INTO_WALL)) {
                return "Fly into wall damage";
            }
            if (lastDamageSource.is(DamageTypes.DRY_OUT)) {
                return "Dry out damage";
            }
            if (lastDamageSource.is(DamageTypes.FREEZE)) {
                return "Freeze damage";
            }
        }

        return "Unknown reason";
    }

    private void sendDebugMessage(String message) {
        if (debugMessages.getCurrentValue() && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§7[§b" + "NavenXD" + "§7] " + message));
        }
    }
}