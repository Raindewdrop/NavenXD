package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import java.awt.Color;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketSnapshot;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.mixin.O.accessors.ClientboundMoveEntityPacketAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import java.awt.Color;
import java.lang.reflect.Field;

@ModuleInfo(
        name = "BackTrack",
        description = "Hold someone in the air",
        category = Category.COMBAT
)
public class BackTrack extends Module {
    public ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("The World", "None")
            .build()
            .getModeValue();
            
   public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    
    public FloatValue pauseDuration = ValueBuilder.create(this, "Pause Duration (Seconds)")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(10.0F)
            .setVisibility(() -> this.mode.isCurrentMode("The World"))
            .build()
            .getFloatValue();
            
    public FloatValue triggerInterval = ValueBuilder.create(this, "Trigger Interval (Seconds)")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(30.0F)
            .setVisibility(() -> this.mode.isCurrentMode("The World"))
            .build()
            .getFloatValue();
            
    public BooleanValue render = ValueBuilder.create(this, "Render Progress Bar")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("The World"))
            .build()
            .getBooleanValue();
            
    public FloatValue progressBarDistance = ValueBuilder.create(this, "Progress Bar Distance")
            .setDefaultFloatValue(15.0F)
            .setMaxFloatValue(50.0F)
            .setMinFloatValue(-50.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> this.render.getCurrentValue())
            .build()
            .getFloatValue();
            
    public BooleanValue showText = ValueBuilder.create(this, "Show Text")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.render.getCurrentValue())
            .build()
            .getBooleanValue();
            
    public FloatValue textDistance = ValueBuilder.create(this, "Text Distance")
            .setDefaultFloatValue(25.0F)
            .setMaxFloatValue(60.0F)
            .setMinFloatValue(-60.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> this.render.getCurrentValue() && this.showText.getCurrentValue())
            .build()
            .getFloatValue();
            
    public BooleanValue pauseSelfPackets = ValueBuilder.create(this, "Retard Self Packets")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> this.mode.isCurrentMode("The World"))
            .build()
            .getBooleanValue();
            
    public FloatValue packetDelayTicks = ValueBuilder.create(this, "Packet Delay (Ticks)")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F)
            .setVisibility(() -> this.mode.isCurrentMode("The World") && this.pauseSelfPackets.getCurrentValue())
            .build()
            .getFloatValue();
            
    private boolean isPausing = false;
    private long pauseStartTime = 0;
    private long lastTriggerTime = 0;
    private long lastAttackTime = 0;
    private int lastAttackTimes = 0;
    private final LinkedBlockingDeque<Packet<?>> packetQueue = new LinkedBlockingDeque<>();
    private final LinkedBlockingDeque<Packet<?>> sendPacketQueue = new LinkedBlockingDeque<>();
    private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);
    private final TimeHelper triggerTimer = new TimeHelper();
    private final ScheduledExecutorService packetExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final int MAIN_COLOR = new Color(150, 45, 45, 255).getRGB();
    
    @Override
    public void onEnable() {
        reset();
        lastTriggerTime = System.currentTimeMillis();
        triggerTimer.reset();
        // 重置攻击相关字段
        lastAttackTime = 0;
        lastAttackTimes = 0;
        
        log("BackTrack enabled, reset attack tracking fields");
    }

    @Override
    public void onDisable() {
        reset();
        packetExecutor.shutdown();
    }

    public void reset() {
        releasePacketQueue();
        releaseSendPacketQueue();
        isPausing = false;
        pauseStartTime = 0;
        lastTriggerTime = 0;
        progress.target = 0.0F;
        progress.value = 0.0F;
        triggerTimer.reset();
    }

    private void releasePacketQueue() {
        int packetCount = packetQueue.size();
        while (!this.packetQueue.isEmpty()) {
            try {
                Packet<?> packet = this.packetQueue.poll();
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
        if (packetCount > 0) {
            log("Released " + packetCount + " queued received packets");
        }
    }
    
    private void releaseSendPacketQueue() {
        int packetCount = sendPacketQueue.size();
        if (packetCount == 0) return;
        
        // 异步处理数据包发送，避免阻塞主线程
        packetExecutor.submit(() -> {
            // 每次只处理一个数据包，实现发一个包等一下的效果
            if (!sendPacketQueue.isEmpty()) {
                try {
                    Packet<?> packet = sendPacketQueue.poll();
                    if (packet != null && mc.getConnection() != null) {
                        mc.getConnection().send(packet);
                        log("Sent packet with " + packetDelayTicks.getCurrentValue() + " tick delay");
                        
                        // 根据配置的tick数计算延迟时间（1 tick = 50ms）
                        long delayMs = (long)(packetDelayTicks.getCurrentValue() * 50.0F);
                        
                        // 如果还有剩余数据包，安排下次处理
                        if (!sendPacketQueue.isEmpty()) {
                            packetExecutor.schedule(this::releaseSendPacketQueue, delayMs, TimeUnit.MILLISECONDS);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("[Backtrack] " + message);
        }
    }
    
    private boolean isPlayerMovementPacket(Packet<?> packet) {
        return packet instanceof ClientboundMoveEntityPacket || 
               packet instanceof ClientboundTeleportEntityPacket ||
               packet instanceof ClientboundPlayerPositionPacket;
    }
    
    private boolean isSelfMovementPacket(Packet<?> packet) {
        // Pause all sent packets
        return true;
    }
    
    private boolean canTrigger() {
        // Only trigger in "The World" mode
        if (!this.mode.isCurrentMode("The World")) {
            return false;
        }
        
        // Only trigger when KillAura has attack target
        boolean isAttacking = isKillAuraAttacking();
        if (!isAttacking) {
            if (log.getCurrentValue()) {
                log("Debug: canTrigger() returned false - KillAura is not attacking");
            }
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastTrigger = currentTime - lastTriggerTime;
        long intervalMs = (long)(triggerInterval.getCurrentValue() * 1000.0F);
        boolean canTrigger = timeSinceLastTrigger >= intervalMs;
        
        if (log.getCurrentValue()) {
            log("Debug: canTrigger() - timeSinceLastTrigger=" + timeSinceLastTrigger + ", intervalMs=" + intervalMs + ", result=" + canTrigger);
        }
        
        return canTrigger;
    }
    
    private boolean isKillAuraAttacking() {
        try {
           KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
           if (killAura != null && killAura.isEnabled()) {
              // 通过反射获取attackTimes字段
              Field attackTimesField = KillAura.class.getDeclaredField("attackTimes");
              attackTimesField.setAccessible(true);
              int currentAttackTimes = attackTimesField.getInt(killAura);
              
              // 调试日志
              if (log.getCurrentValue()) {
                 log("Debug: currentAttackTimes=" + currentAttackTimes + ", lastAttackTimes=" + lastAttackTimes + ", timeSinceLastAttack=" + (System.currentTimeMillis() - lastAttackTime));
              }
              
              // 检查攻击次数是否增加，或者最近500ms内有攻击记录
              boolean isAttacking = currentAttackTimes > this.lastAttackTimes || 
                                 (System.currentTimeMillis() - this.lastAttackTime < 500);
              
              // 更新记录
              if (currentAttackTimes > this.lastAttackTimes) {
                 this.lastAttackTimes = currentAttackTimes;
                 this.lastAttackTime = System.currentTimeMillis();
                 if (log.getCurrentValue()) {
                    log("Debug: Attack detected! Updated lastAttackTimes to " + currentAttackTimes);
                 }
              }
              
              return isAttacking;
           } else {
              if (log.getCurrentValue()) {
                 log("Debug: KillAura is null or disabled");
              }
           }
        } catch (Exception var3) {
           // 反射失败时回退到原逻辑
           if (log.getCurrentValue()) {
              log("Debug: Reflection failed, falling back to target check: " + var3.getMessage());
           }
           KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
           return killAura != null && killAura.isEnabled() && (killAura.target != null || !killAura.targets.isEmpty());
        }
        
        return false;
    }
    
    private void startPause() {
        isPausing = true;
        pauseStartTime = System.currentTimeMillis();
        lastTriggerTime = System.currentTimeMillis();
        progress.target = 100.0F;
        progress.value = 100.0F;
        log("Started pausing player movement packets for " + pauseDuration.getCurrentValue() + " seconds");
    }
    
    private void updatePauseState() {
        if (!isPausing) return;
        
        // If KillAura stops attacking, immediately end pause
        if (!isKillAuraAttacking()) {
            releasePacketQueue();
            // 异步释放所有发送数据包
            if (!sendPacketQueue.isEmpty()) {
                packetExecutor.submit(() -> {
                    int totalPackets = sendPacketQueue.size();
                    while (!sendPacketQueue.isEmpty()) {
                        releaseSendPacketQueue();
                        try {
                            Thread.sleep(5); // 给系统一些时间处理
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    log("KillAura stopped attacking, async released all " + totalPackets + " queued send packets");
                });
            }
            isPausing = false;
            progress.target = 0.0F;
            log("KillAura stopped attacking, released queued packets");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedMs = currentTime - pauseStartTime;
        long durationMs = (long)(pauseDuration.getCurrentValue() * 1000.0F);
        
        if (elapsedMs >= durationMs) {
            releasePacketQueue();
            // 异步释放所有发送数据包
            if (!sendPacketQueue.isEmpty()) {
                packetExecutor.submit(() -> {
                    int totalPackets = sendPacketQueue.size();
                    while (!sendPacketQueue.isEmpty()) {
                        releaseSendPacketQueue();
                        try {
                            Thread.sleep(5); // 给系统一些时间处理
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    log("Pause ended, async released all " + totalPackets + " queued send packets");
                });
            }
            isPausing = false;
            progress.target = 0.0F;
            log("Pause ended, released queued packets");
        } else {
            float remainingPercent = 1.0F - ((float)elapsedMs / (float)durationMs);
            progress.target = remainingPercent * 100.0F;
        }
        
        // 每次tick都异步处理一些发送数据包，避免积压
        if (!sendPacketQueue.isEmpty()) {
            releaseSendPacketQueue();
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null) return;
        
        // 设置模式后缀
        if (this.mode.isCurrentMode("The World")) {
            this.setSuffix("The World");
            // The World mode logic
            updatePauseState();
            progress.update(true);
            
            if (!isPausing && canTrigger()) {
                startPause();
            }
        } else {
            this.setSuffix("None");
            // Normal mode - ensure all packets are released
            if (isPausing) {
                releasePacketQueue();
                releaseSendPacketQueue();
                isPausing = false;
                progress.target = 0.0F;
                progress.value = 0.0F;
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled() && 
            this.mode.isCurrentMode("The World") && this.render.getCurrentValue()) {
            this.render(event.getGuiGraphics());
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        
        Packet<?> packet = event.getPacket();
        
        if (this.mode.isCurrentMode("The World")) {
            // The World mode packet handling
            if (event.getType() == EventType.RECEIVE) {
                if (isPausing && isPlayerMovementPacket(packet)) {
                    event.setCancelled(true);
                    packetQueue.add(packet);
                    if (log.getCurrentValue()) {
                        log("Queued received movement packet: " + packet.getClass().getSimpleName());
                    }
                }
            } else if (event.getType() == EventType.SEND && pauseSelfPackets.getCurrentValue()) {
                if (isPausing && isSelfMovementPacket(packet)) {
                    event.setCancelled(true);
                    sendPacketQueue.add(packet);
                    if (log.getCurrentValue()) {
                        log("Queued send movement packet: " + packet.getClass().getSimpleName());
                    }
                }
            }
        }
    }

    public void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        if (isPausing) {
            int centerX = mc.getWindow().getGuiScaledWidth() / 2;
            int centerY = mc.getWindow().getGuiScaledHeight() / 2;
            
            // 绘制进度条（使用Scaffold的样式）
            int progressBarY = centerY - (int)this.progressBarDistance.getCurrentValue();
            RenderUtils.drawRoundedRect(guiGraphics.pose(), (float)(centerX - 50), (float)progressBarY, 100.0F, 5.0F, 2.0F, Integer.MIN_VALUE);
            // Ensure progress value is not less than 0
            float progressValue = Math.max(0.0F, this.progress.value);
            RenderUtils.drawRoundedRect(guiGraphics.pose(), (float)(centerX - 50), (float)progressBarY, progressValue, 5.0F, 2.0F, MAIN_COLOR);
            
            // 绘制文字
            if (this.showText.getCurrentValue()) {
                long currentTime = System.currentTimeMillis();
                long elapsedMs = currentTime - pauseStartTime;
                long durationMs = (long)(pauseDuration.getCurrentValue() * 1000.0F);
                long remainingMs = durationMs - elapsedMs;
                float remainingSeconds = remainingMs / 1000.0F;
                
                int textY = progressBarY + (int)this.textDistance.getCurrentValue();
                String text = String.format("%.1fs", remainingSeconds);
                Fonts.harmony.render(guiGraphics.pose(), text, (double)(centerX - Fonts.harmony.getWidth(text, 0.35) / 2.0F), (double)textY, Color.WHITE, true, 0.35);
            }
        }
    }
}