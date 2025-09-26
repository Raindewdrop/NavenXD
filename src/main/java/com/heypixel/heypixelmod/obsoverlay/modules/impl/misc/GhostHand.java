package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "GhostHand",
        description = "Ignore the block and open the container.",
        category = Category.MISC
)
public class GhostHand extends Module {

    // 最大交互距离
    private static final double MAX_INTERACTION_DISTANCE = 6.0;

    private final Minecraft mc = Minecraft.getInstance();
    private boolean shouldOpenContainer = false;
    private BlockPos containerPosToOpen = null;

    @Override
    public void onEnable() {
        // 模块启用时的逻辑
    }

    @Override
    public void onDisable() {
        // 模块禁用时的逻辑
        shouldOpenContainer = false;
        containerPosToOpen = null;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;

        // 检查玩家是否按下了右键
        if (mc.options.keyUse.isDown()) {
            // 查找准星前的容器
            BlockPos containerPos = findContainerInSight();

            if (containerPos != null) {
                // 计算距离
                double distance = mc.player.getEyePosition().distanceTo(
                        Vec3.atCenterOf(containerPos)
                );

                // 检查是否在最大交互距离内
                if (distance <= MAX_INTERACTION_DISTANCE) {
                    // 标记需要打开容器
                    shouldOpenContainer = true;
                    containerPosToOpen = containerPos;

                    // 立即发送交互包
                    openContainer(containerPos);

                    // 重置标记，防止重复打开
                    shouldOpenContainer = false;
                }
            }
        } else {
            shouldOpenContainer = false;
            containerPosToOpen = null;
        }
    }

    /**
     * 查找玩家准星方向的容器
     */
    private BlockPos findContainerInSight() {
        if (mc.hitResult == null || !(mc.hitResult instanceof BlockHitResult)) {
            return null;
        }

        BlockHitResult hitResult = (BlockHitResult) mc.hitResult;
        BlockPos lookingAtPos = hitResult.getBlockPos();

        // 检查准星指向的方块是否是容器
        if (isContainer(lookingAtPos)) {
            return lookingAtPos;
        }

        // 如果没有直接指向容器，则沿着视线方向查找
        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 look = mc.player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(MAX_INTERACTION_DISTANCE));

        // 沿着视线方向步进查找
        double step = 0.1;
        double distance = 0;

        while (distance < MAX_INTERACTION_DISTANCE) {
            Vec3 currentPos = start.add(look.scale(distance));
            BlockPos blockPos = BlockPos.containing(
                    currentPos.x,
                    currentPos.y,
                    currentPos.z
            );

            // 跳过空气和玩家当前站立的位置
            if (!mc.level.isEmptyBlock(blockPos) &&
                    !blockPos.equals(BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ()))) {

                // 检查是否是容器
                if (isContainer(blockPos)) {
                    return blockPos;
                }
            }

            distance += step;
        }

        return null;
    }

    /**
     * 检查指定位置的方块是否是容器
     */
    private boolean isContainer(BlockPos pos) {
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);

        if (blockEntity == null) {
            return false;
        }

        // 检查是否是各种容器类型
        return blockEntity instanceof ChestBlockEntity ||
                blockEntity instanceof ShulkerBoxBlockEntity ||
                blockEntity instanceof BarrelBlockEntity ||
                blockEntity instanceof DispenserBlockEntity ||
                blockEntity instanceof DropperBlockEntity ||
                blockEntity instanceof HopperBlockEntity ||
                blockEntity instanceof FurnaceBlockEntity ||
                blockEntity instanceof BlastFurnaceBlockEntity ||
                blockEntity instanceof SmokerBlockEntity ||
                blockEntity instanceof BrewingStandBlockEntity;
    }

    /**
     * 发送交互包打开容器
     */
    private void openContainer(BlockPos pos) {
        if (mc.getConnection() == null) return;

        // 创建一个假的点击结果
        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(pos), // 点击容器的中心点
                Direction.UP, // 假设点击的是上面
                pos,
                false
        );

        // 发送交互包
        mc.getConnection().send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                hitResult,
                0 // 序列号
        ));
    }
}