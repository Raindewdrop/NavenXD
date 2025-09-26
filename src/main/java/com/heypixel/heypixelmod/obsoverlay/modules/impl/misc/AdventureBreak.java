package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "AdventureBreak",
        description = "Allow breaking blocks in adventure mode",
        category = Category.MISC
)
public class AdventureBreak extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    
    private final BooleanValue onlyWhenSneaking = ValueBuilder.create(this, "Only When Sneaking")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    
    private final BooleanValue requireCorrectTool = ValueBuilder.create(this, "Require Correct Tool")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private int sequenceNumber = 0;
    private BlockPos lastBlockPos = null;
    private long lastBreakTime = 0;

    @Override
    public void onEnable() {
        // 模块启用时的逻辑
        sequenceNumber = 0;
        lastBlockPos = null;
        lastBreakTime = 0;
    }

    @Override
    public void onDisable() {
        // 模块禁用时的逻辑
        // 如果正在破坏方块，发送停止破坏包
        if (lastBlockPos != null && mc.getConnection() != null) {
            sendStopBreakPacket(lastBlockPos);
            lastBlockPos = null;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;
        
        // 检查玩家是否在冒险模式
        if (mc.gameMode.getPlayerMode() != GameType.ADVENTURE) {
            if (lastBlockPos != null) {
                sendStopBreakPacket(lastBlockPos);
                lastBlockPos = null;
            }
            return;
        }
        
        // 检查是否满足条件（如果设置了仅潜行时生效）
        if (onlyWhenSneaking.getCurrentValue() && !mc.player.isShiftKeyDown()) {
            if (lastBlockPos != null) {
                sendStopBreakPacket(lastBlockPos);
                lastBlockPos = null;
            }
            return;
        }
        
        // 检查玩家是否按下了左键（破坏方块）
        if (mc.options.keyAttack.isDown()) {
            // 检查准星是否指向方块
            if (mc.hitResult instanceof BlockHitResult) {
                BlockHitResult hitResult = (BlockHitResult) mc.hitResult;
                BlockPos blockPos = hitResult.getBlockPos();
                
                // 检查方块是否可破坏
                if (canBreakBlock(blockPos)) {
                    // 如果是新的方块或者超过一定时间没有更新，重新开始破坏
                    if (!blockPos.equals(lastBlockPos) || System.currentTimeMillis() - lastBreakTime > 1000) {
                        if (lastBlockPos != null) {
                            sendStopBreakPacket(lastBlockPos);
                        }
                        sendStartBreakPacket(blockPos, hitResult.getDirection());
                        lastBlockPos = blockPos;
                        lastBreakTime = System.currentTimeMillis();
                    }
                    
                    // 持续发送破坏进度包
                    sendBreakProgressPacket(blockPos, hitResult.getDirection());
                } else if (lastBlockPos != null) {
                    // 如果不能破坏当前方块，停止之前的破坏
                    sendStopBreakPacket(lastBlockPos);
                    lastBlockPos = null;
                }
            } else if (lastBlockPos != null) {
                // 如果没有指向方块，停止破坏
                sendStopBreakPacket(lastBlockPos);
                lastBlockPos = null;
            }
        } else if (lastBlockPos != null) {
            // 如果松开了左键，停止破坏
            sendStopBreakPacket(lastBlockPos);
            lastBlockPos = null;
        }
    }
    
    /**
     * 检查是否可以破坏指定位置的方块
     */
    private boolean canBreakBlock(BlockPos pos) {
        BlockState blockState = mc.level.getBlockState(pos);
        
        // 检查是否要求正确工具
        if (requireCorrectTool.getCurrentValue()) {
            // 检查玩家手中的工具是否可以破坏这个方块
            return mc.player.hasCorrectToolForDrops(blockState);
        }
        
        // 如果不要求正确工具，则总是返回true
        return true;
    }
    
    /**
     * 发送开始破坏方块的数据包
     */
    private void sendStartBreakPacket(BlockPos pos, Direction direction) {
        if (mc.getConnection() == null) return;
        
        sequenceNumber++;
        
        // 发送开始破坏包
        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                direction,
                sequenceNumber
        ));
        
        // 模拟客户端破坏效果
        mc.particleEngine.destroy(pos, mc.level.getBlockState(pos));
    }
    
    /**
     * 发送破坏进度数据包
     */
    private void sendBreakProgressPacket(BlockPos pos, Direction direction) {
        if (mc.getConnection() == null) return;
        
        sequenceNumber++;
        
        // 发送破坏进度包
        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                pos,
                direction,
                sequenceNumber
        ));
    }
    
    /**
     * 发送停止破坏方块的数据包
     */
    private void sendStopBreakPacket(BlockPos pos) {
        if (mc.getConnection() == null) return;
        
        sequenceNumber++;
        
        // 发送停止破坏包
        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                Direction.UP, // 方向不重要
                sequenceNumber
        ));
    }
}