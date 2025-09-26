package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(
        name = "NewBackTrack",
        description = "Hold someone in the air",
        category = Category.COMBAT
)
public class NewBackTrack extends Module {
    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue OnGroundStop = ValueBuilder.create(this, "OnGroundStop")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public FloatValue maxpacket = ValueBuilder.create(this, "Max Packet number")
            .setDefaultFloatValue(1000F)
            .setFloatStep(5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(5000F)
            .build()
            .getFloatValue();
    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(3F)
            .setFloatStep(0.5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(6F)
            .build()
            .getFloatValue();
    FloatValue delay = ValueBuilder.create(this, "Delay(Tick)")
            .setDefaultFloatValue(20F)
            .setFloatStep(1F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(200F)
            .build()
            .getFloatValue();
    public BooleanValue btrender = ValueBuilder.create(this, "Render")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public ModeValue btrendermode = ValueBuilder.create(this, "Render Mode")
            .setVisibility(this.btrender::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Normal", "LingDong")
            .build()
            .getModeValue();
    // 新增：仅在Aura有目标时生效
    public BooleanValue onlyWhenAuraHasTarget = ValueBuilder.create(this, "Only When Aura Has Target")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    // 新增：智能释放选项
    public BooleanValue smartRelease = ValueBuilder.create(this, "Smart Release")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    // 新增：伤害触发释放
    public BooleanValue releaseOnDamage = ValueBuilder.create(this, "Release On Damage")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    // 新增：药水效果触发释放
    public BooleanValue releaseOnDebuff = ValueBuilder.create(this, "Release On Debuff")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    // 新增：动态范围调整
    public BooleanValue dynamicRange = ValueBuilder.create(this, "Dynamic Range")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    // 拦截队列和状态变量
    public boolean btwork = false;
    private final LinkedBlockingDeque<Packet<?>> airKBQueue = new LinkedBlockingDeque<>();
    private final List<Integer> knockbackPositions = new ArrayList<>();
    private boolean isInterceptingAirKB = false;
    private int interceptedPacketCount = 0;
    private int delayTicks = 0;
    private boolean shouldCheckGround = false;
    private long lastDamageTime = 0;
    private boolean wasInCombat = false;
    private int combatTicks = 0;

    // 进度条设置 - 修改颜色为 942A2BFF
    private static final float PROGRESS_BAR_WIDTH = 200.0f;  // 进度条总宽度
    private static final float PROGRESS_BAR_HEIGHT = 4.0f;  // 进度条高度
    private static final float PROGRESS_BAR_Y_OFFSET = 100.0f; // 在屏幕中心下方的偏移量
    private static final int BACKGROUND_COLOR = 0x80FFFFFF;  // 背景颜色 (半透明白色)
    private static final int PROGRESS_COLOR = 0xFF942A2B;    // 进度颜色 (修改为 942A2BFF)
    private static final int OVERFLOW_COLOR = 0xFF942A2B;    // 溢出部分颜色 (修改为 942A2BFF)
    private static final int COMBAT_COLOR = 0xFFFF0000;      // 战斗状态颜色
    private static final float CORNER_RADIUS = 3.0f;         // 圆角半径

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    public int getPacketCount() {
        return airKBQueue.size();
    }

    public void reset() {
        // 释放所有拦截的包
        releaseAirKBQueue();

        // 重置状态
        isInterceptingAirKB = false;
        interceptedPacketCount = 0;
        delayTicks = 0;
        shouldCheckGround = false;
        btwork = false;
        knockbackPositions.clear();
        combatTicks = 0;
        wasInCombat = false;
    }

    private void releaseAirKBQueue() {
        int packetCount = airKBQueue.size();
        while (!this.airKBQueue.isEmpty()) {
            try {
                Packet<?> packet = this.airKBQueue.poll();
                if (packet != null && mc.getConnection() != null) {
                    ((Packet<ClientGamePacketListener>) packet).handle(mc.getConnection());
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }

        // 记录日志
        if (packetCount > 0) {
            log("Released " + packetCount + " intercepted packets");
        }

        // 重置计数器
        interceptedPacketCount = 0;
        knockbackPositions.clear();
    }

    private boolean hasNearbyPlayers(float range) {
        if (mc.level == null || mc.player == null) return false;

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue; // 跳过自己
            if (player.isAlive() && mc.player.distanceTo(player) <= range) {
                return true;
            }
        }
        return false;
    }

    // 新增：检查Aura是否有目标
    private boolean auraHasTarget() {
        if (!onlyWhenAuraHasTarget.getCurrentValue()) return true;
        
        // 获取Aura模块实例
        KillAura auraModule = (KillAura) com.heypixel.heypixelmod.obsoverlay.Naven.getInstance()
                .getModuleManager()
                .getModule(KillAura.class);
        
        // 检查Aura模块是否启用且有目标
        return auraModule != null && auraModule.isEnabled() && KillAura.target != null;
    }

    // 新增：检查是否在战斗状态
    private boolean isInCombat() {
        // 如果Aura有目标，我们认为在战斗状态
        if (auraHasTarget()) return true;
        
        // 或者最近受到伤害（5秒内）
        return System.currentTimeMillis() - lastDamageTime < 5000;
    }

    // 新增：动态调整范围
    private float getEffectiveRange() {
        if (!dynamicRange.getCurrentValue()) return range.getCurrentValue();
        
        // 根据战斗状态调整范围
        if (isInCombat()) {
            return range.getCurrentValue() * 1.5f; // 战斗时增加范围
        }
        
        return range.getCurrentValue();
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("[Backtrack] " + message);
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null) return;
        
        // 更新战斗状态
        boolean inCombat = isInCombat();
        if (inCombat && !wasInCombat) {
            combatTicks = 0;
        }
        wasInCombat = inCombat;
        combatTicks++;
        
        // 新增：检查Aura是否有目标
        if (!auraHasTarget() && onlyWhenAuraHasTarget.getCurrentValue()) {
            if (isInterceptingAirKB || shouldCheckGround) {
                log("Aura has no target, stopping backtrack");
                reset();
            }
            return;
        }

        // 更新工作状态
        btwork = isInterceptingAirKB || shouldCheckGround;

        // 处理冷却延迟
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        // 检查是否应该开始拦截
        float effectiveRange = getEffectiveRange();
        if (!isInterceptingAirKB && hasNearbyPlayers(effectiveRange)) {
            isInterceptingAirKB = true;
            shouldCheckGround = false;
            interceptedPacketCount = 0;
            airKBQueue.clear();
            knockbackPositions.clear();
            log("Detected nearby players, starting packet interception");
        }

        // 智能释放检查
        if (isInterceptingAirKB && smartRelease.getCurrentValue()) {
            // 检查是否应该提前释放
            boolean shouldRelease = false;
            
            // 检查是否达到最大包数量
            if (interceptedPacketCount >= maxpacket.getCurrentValue()) {
                shouldRelease = true;
                log("Reached maximum packet count, preparing to release");
            }
            
            // 检查是否不再有附近玩家
            else if (!hasNearbyPlayers(effectiveRange)) {
                shouldRelease = true;
                log("No nearby players, preparing to release");
            }
            
            // 检查是否脱离战斗
            else if (!isInCombat()) {
                shouldRelease = true;
                log("Releasing packets immediately");
            }
            
            if (shouldRelease) {
                if (OnGroundStop.getCurrentValue()) {
                    shouldCheckGround = true;
                    log("Waiting for player to land on ground");
                } else {
                    log("Releasing packets immediately");
                    releaseAirKBQueue();
                    resetAfterRelease();
                }
            }
        }

        // 检查是否需要释放包（当需要等待落地时）
        if (shouldCheckGround && mc.player.onGround()) {
            log("Player has landed, releasing intercepted packets");
            releaseAirKBQueue();
            resetAfterRelease();
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled() && (!onlyWhenAuraHasTarget.getCurrentValue() || auraHasTarget())) {
            // 渲染进度条和文本
            this.render(event.getGuiGraphics());
        }
    }

    private void resetAfterRelease() {
        isInterceptingAirKB = false;
        shouldCheckGround = false;
        delayTicks = (int) delay.getCurrentValue();
        log("Entering cooldown delay: " + delayTicks + " ticks");
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.getConnection() == null || !isInterceptingAirKB) {
            return;
        }
        
        // 新增：检查Aura是否有目标
        if (!auraHasTarget() && onlyWhenAuraHasTarget.getCurrentValue()) {
            return;
        }

        // 只处理接收包
        if (event.getType() != EventType.RECEIVE) {
            return;
        }

        Packet<?> packet = event.getPacket();

        // 位置包会触发停止拦截并释放所有包
        if (packet instanceof ClientboundPlayerPositionPacket) {
            event.setCancelled(true);
            isInterceptingAirKB = false;
            shouldCheckGround = false;
            log("Received position packet, stopping interception and releasing all packets");
            releaseAirKBQueue();
            resetAfterRelease();
        }
        // 处理击退包
        else if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() == mc.player.getId()) {
                event.setCancelled(true);
                airKBQueue.add(packet);
                interceptedPacketCount++;
                knockbackPositions.add(airKBQueue.size() - 1);
                log("Intercepted knockback packet #" + interceptedPacketCount);
            }
        }
        // 处理伤害包
        else if (packet instanceof ClientboundSetHealthPacket healthPacket) {
            float health = healthPacket.getHealth();
            float lastHealth = mc.player.getHealth();
            
            if (health < lastHealth && releaseOnDamage.getCurrentValue()) {
                lastDamageTime = System.currentTimeMillis();
                log("Took damage, preparing to release packets");
                
                if (!OnGroundStop.getCurrentValue()) {
                    releaseAirKBQueue();
                    resetAfterRelease();
                } else {
                    shouldCheckGround = true;
                }
            }
            
            // 仍然拦截这个包
            event.setCancelled(true);
            airKBQueue.add(packet);
            interceptedPacketCount++;
            log("Intercepted health packet #" + interceptedPacketCount);
        }
        // 处理药水效果包
        else if (packet instanceof ClientboundUpdateMobEffectPacket effectPacket) {
            if (releaseOnDebuff.getCurrentValue() && 
                (effectPacket.getEffect() == MobEffects.MOVEMENT_SLOWDOWN || 
                 effectPacket.getEffect() == MobEffects.POISON || 
                 effectPacket.getEffect() == MobEffects.WITHER || 
                 effectPacket.getEffect() == MobEffects.BLINDNESS)) {
                log("Received negative effect, preparing to release packets");
                
                if (!OnGroundStop.getCurrentValue()) {
                    releaseAirKBQueue();
                    resetAfterRelease();
                } else {
                    shouldCheckGround = true;
                }
            }
            
            // 仍然拦截这个包
            event.setCancelled(true);
            airKBQueue.add(packet);
            interceptedPacketCount++;
            log("Intercepted effect packet #" + interceptedPacketCount);
        }
        // 拦截其他所有包
        else {
            event.setCancelled(true);
            airKBQueue.add(packet);
            interceptedPacketCount++;
            log("Intercepted regular packet #" + interceptedPacketCount);
        }
    }

    public void render(GuiGraphics guiGraphics) {
        if (!isInterceptingAirKB && !shouldCheckGround) return;
        
        // 新增：检查Aura是否有目标
        if (onlyWhenAuraHasTarget.getCurrentValue() && !auraHasTarget()) return;

        if (!btrendermode.isCurrentMode("Normal")){
            return;
        }
        if (!btrender.getCurrentValue()){
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float x = (screenWidth - PROGRESS_BAR_WIDTH) / 2.0f;
        float y = screenHeight / 2.0f + PROGRESS_BAR_Y_OFFSET;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float maxPacketValue = Math.max(1.0f, maxpacket.getCurrentValue());
        float progress = Math.min(1.0f, interceptedPacketCount / maxPacketValue);
        float progressWidth = PROGRESS_BAR_WIDTH * progress;

        // 根据战斗状态选择颜色
        int progressColor = isInCombat() ? COMBAT_COLOR : PROGRESS_COLOR;
        
        RenderUtils.drawRoundedRect(poseStack, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, BACKGROUND_COLOR);

        if (progressWidth > 0) {
            RenderUtils.drawRoundedRect(poseStack, x, y, progressWidth, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, progressColor);
        }

        if (OnGroundStop.getCurrentValue() && interceptedPacketCount > maxpacket.getCurrentValue()) {
            float overflowProgress = (interceptedPacketCount - maxpacket.getCurrentValue()) / maxPacketValue;
            float overflowWidth = Math.min(PROGRESS_BAR_WIDTH * overflowProgress, PROGRESS_BAR_WIDTH);
            RenderUtils.drawRoundedRect(poseStack,
                    x + PROGRESS_BAR_WIDTH - overflowWidth,
                    y,
                    overflowWidth,
                    PROGRESS_BAR_HEIGHT,
                    CORNER_RADIUS,
                    OVERFLOW_COLOR);
        }

        // 使用NewNotification的字体和样式渲染文本
        String trackingText = "Tracking...";
        String statusText = isInCombat() ? "COMBAT" : "IDLE";
        float textScale = 0.35f;
        float trackingTextWidth = Fonts.harmony.getWidth(trackingText, textScale);
        float statusTextWidth = Fonts.harmony.getWidth(statusText, textScale);
        float trackingTextX = (screenWidth - trackingTextWidth) / 2.0f;
        float statusTextX = (screenWidth - statusTextWidth) / 2.0f;
        float trackingTextY = y - 25f; // 稍微上移避免重叠
        float statusTextY = y - 40f;

        // 渲染跟踪文本
        Fonts.harmony.render(
                poseStack,
                trackingText,
                (double) trackingTextX,
                (double) trackingTextY,
                Color.WHITE,
                false,
                textScale
        );

        // 渲染状态文本
        Fonts.harmony.render(
                poseStack,
                statusText,
                (double) statusTextX,
                (double) statusTextY,
                isInCombat() ? Color.RED : Color.GREEN,
                false,
                textScale
        );

        // 渲染包数量
        String packetText = interceptedPacketCount + "/" + (int)maxpacket.getCurrentValue();
        float packetTextWidth = Fonts.harmony.getWidth(packetText, textScale);
        float packetTextX = (screenWidth - packetTextWidth) / 2.0f;
        float packetTextY = y - 55f;
        
        Fonts.harmony.render(
                poseStack,
                packetText,
                (double) packetTextX,
                (double) packetTextY,
                Color.WHITE,
                false,
                textScale
        );

        poseStack.popPose();
    }
}