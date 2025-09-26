//原作者RN_Random_Name
package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.awt.Color;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(
        name = "LagBase",
        category = Category.COMBAT,
        description = "LagBase port with BackTrack (clientbound delay) + LagRange (serverbound blink)"
)
public class LagBase extends Module {
    // Modes
    ModeValue mode = ValueBuilder.create(this, "LagServerPacketMode").setModes("ModernLag", "None", "Polar", "Lite").build().getModeValue();
    BooleanValue intave = ValueBuilder.create(this, "Intave").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue backTrack = ValueBuilder.create(this, "BackTrack").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue packetUpdateLength = ValueBuilder.create(this, "MaxPacketLength").setDefaultFloatValue(100f).setMinFloatValue(1f).setMaxFloatValue(200f).setFloatStep(1f).build().getFloatValue();
    FloatValue maxHitRange = ValueBuilder.create(this, "MaxHitRange").setDefaultFloatValue(4f).setMinFloatValue(2f).setMaxFloatValue(6f).setFloatStep(0.1f).build().getFloatValue(); // 未使用（保留）
    FloatValue hitRange = ValueBuilder.create(this, "HitRange").setDefaultFloatValue(3f).setMinFloatValue(1f).setMaxFloatValue(6f).setFloatStep(0.1f).build().getFloatValue(); // 未使用（保留）
    BooleanValue onlyAura = ValueBuilder.create(this, "OnlyAura").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue activity = ValueBuilder.create(this, "Activity").setDefaultBooleanValue(true).build().getBooleanValue(); // 未使用（保留）
    BooleanValue attackFix = ValueBuilder.create(this, "AttackFix").setDefaultBooleanValue(true).build().getBooleanValue();

    ModeValue updateMode = ValueBuilder.create(this, "UpdateMode").setModes("Combat", "Normal", "Dynamic", "Delay").build().getModeValue();
    FloatValue dynamicUpdateLength = ValueBuilder.create(this, "[Dynamic]ProcessPacketLength").setDefaultFloatValue(1f).setMinFloatValue(1f).setMaxFloatValue(10f).setFloatStep(1f).build().getFloatValue();
    FloatValue backTrackDelay = ValueBuilder.create(this, "[Normal]BackTrackDelay").setDefaultFloatValue(1400f).setMinFloatValue(1f).setMaxFloatValue(10000f).setFloatStep(50f).build().getFloatValue();
    FloatValue releaseDelay = ValueBuilder.create(this, "[Delay]ReleaseDelay").setDefaultFloatValue(20f).setMinFloatValue(1f).setMaxFloatValue(400f).setFloatStep(1f).build().getFloatValue();

    // LagRange
    BooleanValue lagRange = ValueBuilder.create(this, "LagRange").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue lagDistanceLimit = ValueBuilder.create(this, "lagDistance").setDefaultFloatValue(1.42f).setMinFloatValue(0f).setMaxFloatValue(6f).setFloatStep(0.01f).build().getFloatValue();
    FloatValue maxLagRange = ValueBuilder.create(this, "MaxLagRange").setDefaultFloatValue(5.8f).setMinFloatValue(1f).setMaxFloatValue(7f).setFloatStep(0.01f).build().getFloatValue();
    FloatValue minLagRange = ValueBuilder.create(this, "MinLagRange").setDefaultFloatValue(3.67f).setMinFloatValue(1f).setMaxFloatValue(7f).setFloatStep(0.01f).build().getFloatValue();
    BooleanValue onlyWhenNeed = ValueBuilder.create(this, "OnlyWhenNeed").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue comboLag = ValueBuilder.create(this, "ComboLag").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue stopOnBurning = ValueBuilder.create(this, "StopOnBurning").setDefaultBooleanValue(true).build().getBooleanValue();

    // Packet process toggles (placeholders)
    ModeValue processS12Mode = ValueBuilder.create(this, "ProcessS12Mode").setModes("Cancel", "InPut").setDefaultModeIndex(1).build().getModeValue(); // 未使用（保留）
    ModeValue processS27Mode = ValueBuilder.create(this, "ProcessS27Mode").setModes("Cancel", "InPut").setDefaultModeIndex(1).build().getModeValue(); // 未使用（保留）
    ModeValue processS18Mode = ValueBuilder.create(this, "ProcessS18Mode").setModes("Cancel", "InPut").setDefaultModeIndex(1).build().getModeValue(); // 未使用（保留）
    ModeValue processS08Mode = ValueBuilder.create(this, "ProcessS08Mode").setModes("Cancel", "InPut").setDefaultModeIndex(0).build().getModeValue(); // 未使用（保留）

    // Rendering toggles
    BooleanValue outline = ValueBuilder.create(this, "Outline").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue renderBox = ValueBuilder.create(this, "RenderBox").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue outlineWidth = ValueBuilder.create(this, "OutlineWidth").setDefaultFloatValue(2.0f).setMinFloatValue(0.1f).setMaxFloatValue(5f).setFloatStep(0.1f).build().getFloatValue(); // 未使用（保留）
    FloatValue renderSize = ValueBuilder.create(this, "RenderSize").setDefaultFloatValue(0.0f).setMinFloatValue(-1f).setMaxFloatValue(1f).setFloatStep(0.05f).build().getFloatValue();

    // Constants for Blink throttling
    private static final int BLINK_QUEUE_LIMIT = 700;
    private static final int BLINK_RELEASE_BURST = 64;

    // State queues（并发队列更安全）
    private final Queue<Packet<?>> savedClientbound = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> blinkPackets = new ConcurrentLinkedQueue<>();

    // Timers
    private final TimeHelper timer = new TimeHelper();
    private final TimeHelper releaseTimerT = new TimeHelper();
    private final TimeHelper attackingTimer = new TimeHelper(); // 未使用（保留）
    private final TimeHelper hurtTimer = new TimeHelper();
    private final TimeHelper blinkReleaseTick = new TimeHelper();

    // Flags
    private boolean resettingBacktrack = false;
    private boolean lagRangeActive = false;
    private boolean combo = false;

    // Counters
    private int updateLength = 0;

    // Accumulated distance proxy
    private double lagDistanceAccum = 0.0;

    @Override
    public void onEnable() {
        savedClientbound.clear();
        blinkPackets.clear();
        resettingBacktrack = false;
        lagRangeActive = false;
        combo = false;
        updateLength = 0;
        lagDistanceAccum = 0.0;
        timer.reset();
        releaseTimerT.reset();
        attackingTimer.reset();
        hurtTimer.reset();
        blinkReleaseTick.reset();
    }

    @Override
    public void onDisable() {
        // Flush outgoing blink (全量)
        flushBlink();
        // Release saved clientbound
        while (!savedClientbound.isEmpty()) {
            Packet<?> p = savedClientbound.poll();
            if (p != null && mc.getConnection() != null) {
                @SuppressWarnings("unchecked")
                Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> cp =
                        (Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>) p;
                cp.handle(mc.getConnection());
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE || mc.player == null) return;

        // 可选：基于受击窗口做 comboLag 标记
        if (comboLag.getCurrentValue()) {
            combo = !hurtTimer.delay(400.0);
        }

        // BackTrack release policy
        if (!savedClientbound.isEmpty()) {
            if (updateMode.isCurrentMode("Combat")) {
                Entity auraTarget = getAuraTarget();
                // 简单策略：受击窗口多放几个
                if (auraTarget instanceof LivingEntity && ((LivingEntity) auraTarget).invulnerableTime > 0) {
                    updateLength = Math.min(updateLength + 2, 10);
                }
            }
            if (updateMode.isCurrentMode("Dynamic")) {
                updateLength += (int) dynamicUpdateLength.getCurrentValue();
            }
            if (updateMode.isCurrentMode("Delay")) {
                if (releaseTimerT.delay(releaseDelay.getCurrentValue())) {
                    processOneSaved();
                    releaseTimerT.reset();
                }
            } else if (updateMode.isCurrentMode("Normal")) {
                if (timer.delay(backTrackDelay.getCurrentValue())) {
                    processOneSaved();
                    timer.reset();
                }
            }
            while (updateLength-- > 0 && !savedClientbound.isEmpty()) {
                processOneSaved();
            }
            if (updateLength < 0) updateLength = 0;
        }

        // LagRange：非激活时每 tick 分批释放（平滑）
        if (!lagRangeActive && !blinkPackets.isEmpty()) {
            flushBlinkBurst(false);
        }

        // 更新后缀
        setSuffix(String.format("%s | C:%d B:%d%s",
                mode.getCurrentMode(),
                savedClientbound.size(),
                blinkPackets.size(),
                lagRangeActive ? " (ACTIVE)" : ""));
    }

    private void processOneSaved() {
        Packet<?> p = savedClientbound.poll();
        if (p != null && mc.getConnection() != null) {
            @SuppressWarnings("unchecked")
            Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> cp =
                    (Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>) p;
            cp.handle(mc.getConnection());
        }
    }

    @EventTarget(4)
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.level == null || e.isCancelled()) return;

        // Aura 安全获取
        Module auraModule = Naven.getInstance().getModuleManager().getModule(KillAura.class);
        boolean auraOn = auraModule instanceof KillAura && auraModule.isEnabled();
        Entity target = getAuraTarget();

        if (e.getType() == EventType.RECEIVE) {
            // 受击窗口刷新（以运动包为参考）
            String name = e.getPacket().getClass().getName();
            if (name.contains("ClientboundSetEntityMotion") && target != null) {
                hurtTimer.reset();
            }

            // BackTrack：优先处理重置阶段
            if (resettingBacktrack && !savedClientbound.isEmpty()) {
                processOneSaved();
                if (savedClientbound.isEmpty()) resettingBacktrack = false;
            }

            if (backTrack.getCurrentValue() && (!onlyAura.getCurrentValue() || auraOn)) {
                if (shouldBufferClientbound(e.getPacket())) {
                    if (savedClientbound.size() < (int) packetUpdateLength.getCurrentValue()) {
                        savedClientbound.offer(e.getPacket());
                        e.setCancelled(true);
                    } else {
                        // Queue 满：释放一个避免死锁
                        processOneSaved();
                    }
                }

                // Attack fix：关键包到来强制释放
                if (attackFix.getCurrentValue()) {
                    if (name.contains("ClientboundRemoveEntities") || name.contains("ClientboundSetEntityData")) {
                        drainSaved();
                        resettingBacktrack = true;
                    }
                }
            }
            return;
        }

        // SEND path (LagRange)
        if (e.getType() == EventType.SEND && lagRange.getCurrentValue()) {
            Packet<?> p = e.getPacket();

            // 关键 Serverbound 包直通
            if (isCriticalServerbound(p)) {
                // 顺带平滑释放一波，避免长期堆积
                flushBlinkBurst(false);
                return;
            }

            // 白名单检查（重用 Blink.whitelist）
            boolean allowed = shouldBlinkServerbound(p);
            if (!allowed) return;

            if (shouldActivateLagRange(target, auraOn)) {
                lagRangeActive = true;
                enqueueBlink(p);
                e.setCancelled(true);

                // 简单位移代理：按水平速度累计
                double vx = mc.player.getDeltaMovement().x;
                double vz = mc.player.getDeltaMovement().z;
                lagDistanceAccum += Math.sqrt(vx * vx + vz * vz);

                if (lagDistanceAccum > lagDistanceLimit.getCurrentValue() || beyondMaxRange(target)) {
                    deactivateLagRange();
                }
            } else if (lagRangeActive) {
                // 结束阶段：分批释放
                deactivateLagRange();
            }
        }
    }

    private void deactivateLagRange() {
        lagRangeActive = false;
        lagDistanceAccum = 0.0;
        // 先分批平滑释放一小段，再在 onMotion 继续释放；如需立即清空可调用 flushBlink()
        flushBlinkBurst(false);
    }

    private void enqueueBlink(Packet<?> p) {
        if (blinkPackets.size() >= BLINK_QUEUE_LIMIT) {
            blinkPackets.poll(); // 丢弃最老的
        }
        blinkPackets.offer(p);
    }

    private void flushBlinkBurst(boolean flushAll) {
        if (blinkPackets.isEmpty()) return;
        if (!flushAll) {
            // 每 ~1 tick 放一波，避免瞬时洪峰
            if (!blinkReleaseTick.delay(50.0)) return;
            blinkReleaseTick.reset();
        }
        int released = 0;
        while (!blinkPackets.isEmpty()) {
            Packet<?> pkt = blinkPackets.poll();
            if (pkt != null) NetworkUtils.sendPacketNoEvent(pkt);
            if (!flushAll && ++released >= BLINK_RELEASE_BURST) break;
        }
    }

    private void flushBlink() {
        // 全量释放（禁用/切世界等场景）
        while (!blinkPackets.isEmpty()) {
            Packet<?> pkt = blinkPackets.poll();
            if (pkt != null) NetworkUtils.sendPacketNoEvent(pkt);
        }
    }

    private void drainSaved() {
        while (!savedClientbound.isEmpty()) processOneSaved();
    }

    private boolean shouldBufferClientbound(Packet<?> p) {
        String n = p.getClass().getName();
        boolean keepAlive = n.contains("ClientboundKeepAlive");
        if (keepAlive && intave.getCurrentValue()) return false;
        if (mode.isCurrentMode("None")) return false;
        if (mode.isCurrentMode("ModernLag")) {
            return n.contains("ClientboundMoveEntity")
                    || n.contains("ClientboundTeleportEntity")
                    || n.contains("ClientboundSetEntityData")
                    || n.contains("ClientboundSetEntityMotion")
                    || n.contains("ClientboundExplode")
                    || n.contains("ClientboundSetTime")
                    || n.contains("ClientboundSound")
                    || n.contains("ClientboundLevelParticles")
                    || n.contains("ClientboundPlayerPosition")
                    || n.contains("ClientboundUpdateAttributes")
                    || n.contains("ClientboundRemoveEntities")
                    || n.contains("ClientboundForgetLevelChunk")
                    || n.contains("ClientboundLevelChunkWithLight")
                    || n.contains("ClientboundBlockChanged");
        } else if (mode.isCurrentMode("Polar")) {
            // 较轻：排除部分战斗相关/状态包
            return n.contains("ClientboundMoveEntity")
                    || n.contains("ClientboundTeleportEntity")
                    || n.contains("ClientboundSetEntityData")
                    || n.contains("ClientboundLevelParticles")
                    || n.contains("ClientboundPlayerPosition");
        } else if (mode.isCurrentMode("Lite")) {
            return n.contains("ClientboundSetTime")
                    || n.contains("ClientboundMoveEntity")
                    || n.contains("ClientboundTeleportEntity")
                    || n.contains("ClientboundSetEntityMotion")
                    || n.contains("ClientboundPlayerPosition");
        }
        return false;
    }

    private boolean shouldBlinkServerbound(Packet<?> p) {
        Set<Class<?>> white = Blink.whitelist;
        boolean isServerbound = p.getClass().getName().contains("net.minecraft.network.protocol.game.Serverbound")
                || p.getClass().getName().contains("net.minecraft.network.protocol.common.Serverbound")
                || p.getClass().getName().contains("net.minecraft.network.protocol.handshake.Serverbound")
                || p.getClass().getName().contains("net.minecraft.network.protocol.login.Serverbound")
                || p.getClass().getName().contains("net.minecraft.network.protocol.status.Serverbound");
        return isServerbound && (white == null || !white.contains(p.getClass()));
    }

    private boolean isCriticalServerbound(Packet<?> p) {
        String s = p.getClass().getSimpleName();
        return s.contains("KeepAlive") || s.contains("Pong")
                || s.contains("ConfigurationAck") || s.contains("ClientInformation")
                || s.contains("ChatSessionUpdate");
    }

    private boolean shouldActivateLagRange(Entity target, boolean auraOn) {
        if (target == null) return false;
        if (onlyAura.getCurrentValue() && !auraOn) return false;
        if (mc.player.isOnFire() && stopOnBurning.getCurrentValue()) return false;

        double d = mc.player.distanceTo(target);
        if (d >= maxLagRange.getCurrentValue()) return false;

        boolean need = !onlyWhenNeed.getCurrentValue()
                || (target instanceof LivingEntity && ((LivingEntity) target).hurtTime > 0)
                || isClosingDistance(target);

        boolean pastMin = d > minLagRange.getCurrentValue();
        return need && pastMin;
    }

    private boolean isClosingDistance(Entity target) {
        // 近一帧预测距离是否在缩小
        var p0 = mc.player.position(); var t0 = target.position();
        var p1 = p0.add(mc.player.getDeltaMovement());
        var t1 = t0.add(target.getDeltaMovement());
        return p1.distanceTo(t1) + 1e-3 < p0.distanceTo(t0);
    }

    // ==== 可视化（简化版 3D Box at target）====
    @EventTarget
    public void onRender3D(EventRender e) {
        if (!renderBox.getCurrentValue()) return;
        Entity t = getAuraTarget();
        if (!(t instanceof LivingEntity)) return;
        PoseStack stack = e.getPMatrixStack();
        AABB bb = t.getBoundingBox().inflate(0.02 + renderSize.getCurrentValue());
        stack.pushPose();
        // 实心
        RenderSystem.setShaderColor(1f, 0f, 0f, 0.25f);
        RenderUtils.drawSolidBox(bb, stack);
        // 轮廓
        if (outline.getCurrentValue()) {
            RenderSystem.setShaderColor(1f, 0f, 0f, 230f / 255f);
            RenderUtils.drawOutlinedBox(bb, stack);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        stack.popPose();
    }

    private boolean beyondMaxRange(Entity target) {
        if (target == null) return true;
        double dClient = mc.player.distanceTo(target);
        return dClient > maxLagRange.getCurrentValue();
    }

    private Entity getAuraTarget() {
        try {
            return KillAura.target; // 与 Aura 模块的集成点
        } catch (Throwable ignored) {
            return null;
        }
    }
}