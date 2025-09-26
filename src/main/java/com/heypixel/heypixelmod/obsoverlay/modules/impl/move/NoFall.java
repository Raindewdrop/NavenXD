// Decompiled with: CFR 0.152
// Class Version: 17
package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStrafe;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@ModuleInfo(name = "NoFall", category = Category.MOVEMENT, description = "Prevent fall damage")
public class NoFall extends Module {
    private final FloatValue fallDistance = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(3.0f)
            .setFloatStep(0.1f)
            .setMinFloatValue(3.0f)
            .setMaxFloatValue(15.0f)
            .build()
            .getFloatValue();

    private double previousFallDistance;
    private boolean isLagged = false;
    private boolean shouldHandleFall = false;
    private boolean shouldSendLagPacket = false;
    private boolean shouldJump = false;

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        isLagged = false;
        shouldHandleFall = false;
        shouldSendLagPacket = false;
        shouldJump = false;
    }

    private boolean shouldBlockJump() {
        return shouldHandleFall || shouldJump;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() == EventType.POST || mc.player == null) {
            return;
        }

        if (shouldBlockJump()) {
            mc.options.keyJump.setDown(false);
        }

        previousFallDistance = mc.player.onGround() ? 0.0 : mc.player.fallDistance;

        if (isLagged && shouldHandleFall) {
            shouldJump = true;
            shouldHandleFall = false;
            isLagged = false;
        }
    }

    @EventTarget
    public void onLivingUpdate(EventUpdate event) {
        if (shouldBlockJump() && mc.options != null) {
            mc.options.keyJump.setDown(false);
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (mc.player.onGround() && shouldJump) {
            mc.player.jumpFromGround();
            shouldJump = false;
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (shouldBlockJump()) {
            event.setJump(false);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() == EventType.POST) {
            return;
        }

        if (!shouldHandleFall &&
                mc.player.fallDistance > fallDistance.getCurrentValue() &&
                !event.isOnGround()) {
            shouldHandleFall = true;
            isLagged = false;
            shouldSendLagPacket = false;
        }

        if (shouldHandleFall && mc.player.fallDistance < 3.0f) {
            event.setOnGround(false);

            if (!shouldSendLagPacket) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                        event.getX() - 1000.0,
                        event.getY(),
                        event.getZ(),
                        false
                ));
                shouldSendLagPacket = true;
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND) {
            if (shouldHandleFall &&
                    shouldSendLagPacket &&
                    !isLagged &&
                    event.getPacket() instanceof ServerboundMovePlayerPacket) {
                event.setCancelled(true);
            }
        } else if (shouldHandleFall &&
                event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            isLagged = true;
        }
    }
}