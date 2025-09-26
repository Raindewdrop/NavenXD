package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

@ModuleInfo(
        name = "InventoryMove",

        description = "Allows movement while inventory is open",
        category = Category.MOVEMENT
)
public class InventoryMove extends Module {

    private final BooleanValue inventoryOnly = new BooleanValue(this, "Inventory Only", false, null, null);
    private final BooleanValue noChatGui = new BooleanValue(this, "No Chat GUI", true, null, null);
    private final BooleanValue noShift = new BooleanValue(this, "No Shift", true, null, null);
    private final BooleanValue noSprintInInventory = new BooleanValue(this, "No Sprint in Inventory", true, null, null);

    private final Minecraft mc = Minecraft.getInstance();
    private boolean wasInventoryOpen = false;
    private boolean wasSprinting = false;

    public InventoryMove() {
    }

    private boolean shouldHandleInputs() {
        if (!isEnabled()) return false;
        if (mc.screen == null) return false;
        if (noChatGui.getCurrentValue() && mc.screen instanceof ChatScreen) return false;
        if (inventoryOnly.getCurrentValue() && !(mc.screen instanceof InventoryScreen) && !(mc.screen instanceof ContainerScreen)) return false;
        return true;
    }

    private boolean isInventoryOpen() {
        return mc.screen instanceof InventoryScreen;
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (shouldHandleInputs()) {
            // 直接处理移动输入
            handleMovementInput(event);
        }

        // 处理背包内的疾跑禁用
        handleSprintInInventory();
    }

    private void handleMovementInput(EventMoveInput event) {
        // 重置移动输入
        event.setForward(0.0F);
        event.setStrafe(0.0F);
        event.setJump(false);

        // 检查按键状态并设置移动输入
        if (isKeyDown(mc.options.keyUp)) {
            event.setForward(event.getForward() + 1.0F);
        }
        if (isKeyDown(mc.options.keyDown)) {
            event.setForward(event.getForward() - 1.0F);
        }
        if (isKeyDown(mc.options.keyLeft)) {
            event.setStrafe(event.getStrafe() + 1.0F);
        }
        if (isKeyDown(mc.options.keyRight)) {
            event.setStrafe(event.getStrafe() - 1.0F);
        }
        if (isKeyDown(mc.options.keyJump)) {
            event.setJump(true);
        }

        // 处理潜行
        if (noShift.getCurrentValue()) {
            event.setSneak(false);
        } else if (isKeyDown(mc.options.keyShift)) {
            event.setSneak(true);
        }
    }

    private void handleSprintInInventory() {
        if (!noSprintInInventory.getCurrentValue() || mc.player == null) return;

        boolean isInventoryOpenNow = isInventoryOpen();

        if (isInventoryOpenNow && !wasInventoryOpen) {
            // 背包刚打开，保存当前疾跑状态并禁用疾跑
            wasSprinting = mc.player.isSprinting();
            if (wasSprinting) {
                mc.player.setSprinting(false);
                mc.options.keySprint.setDown(false);
            }
        } else if (!isInventoryOpenNow && wasInventoryOpen && wasSprinting) {
            // 背包刚关闭，恢复之前的疾跑状态
            mc.player.setSprinting(true);
            mc.options.keySprint.setDown(true);
        }

        wasInventoryOpen = isInventoryOpenNow;

        // 如果在背包内，强制禁用疾跑
        if (isInventoryOpenNow) {
            mc.player.setSprinting(false);
            mc.options.keySprint.setDown(false);
        }
    }

    private boolean isKeyDown(KeyMapping key) {
        // 直接检查物理按键状态
        int keyCode = key.getDefaultKey().getValue();
        return GLFW.glfwGetKey(mc.getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
    }

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        if (event.getType() == EventType.PRE && shouldHandleInputs()) {
            // 在GUI中保持按键状态
            if (noShift.getCurrentValue()) {
                mc.options.keyShift.setDown(false);
            }

            // 确保移动按键在GUI中保持激活状态
            handleKeyStates();
        }

        // 每tick检查背包内的疾跑状态
        handleSprintInInventory();
    }

    private void handleKeyStates() {
        // 确保移动按键在GUI中保持激活状态
        KeyMapping[] moveKeys = {
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
                mc.options.keyJump
        };

        for (KeyMapping key : moveKeys) {
            if (isKeyDown(key)) {
                key.setDown(true);
            }
        }

        // 如果在背包内，禁用疾跑按键
        if (noSprintInInventory.getCurrentValue() && isInventoryOpen()) {
            mc.options.keySprint.setDown(false);
        }
    }

    @Override
    public void onDisable() {
        // 释放所有按键
        KeyMapping.releaseAll();

        // 恢复疾跑状态如果之前是开启的
        if (wasSprinting && mc.player != null) {
            mc.player.setSprinting(true);
            mc.options.keySprint.setDown(true);
        }

        wasInventoryOpen = false;
        wasSprinting = false;
    }

    @Override
    public void onEnable() {
        // 启用时重置状态
        wasInventoryOpen = isInventoryOpen();
        wasSprinting = mc.player != null && mc.player.isSprinting();
    }
}