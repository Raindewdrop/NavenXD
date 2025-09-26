package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventKey;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(
    name = "SnakeGame",
    description = "Play snake game in Minecraft",
    category = Category.MISC
)
public class SnakeGame extends Module {
    
    private static final int GRID_WIDTH = 40;
    private static final int GRID_HEIGHT = 30;
    private static final int CELL_SIZE = 8;
    private static final int BORDER = 10;
    
    private List<Point> snake;
    private Point food;
    private Direction direction;
    private Direction nextDirection;
    private boolean gameOver;
    private int score;
    private long gameOverTime;
    private final TimeHelper gameTimer = new TimeHelper();
    private final Random random = new Random();
    
    public SnakeGame() {
    }
    
    @Override
    public void onEnable() {
        initGame();
        gameTimer.reset();
        // 强制停止所有移动
        forceStopMovement();
    }
    
    @Override
    public void onDisable() {
        snake = null;
        food = null;
        gameOver = false;
        score = 0;
    }
    
    private void forceStopMovement() {
        if (mc.player == null) return;
        
        // 强制停止所有移动
        mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
        mc.player.xxa = 0;
        mc.player.zza = 0;
        mc.player.setSprinting(false);
        
        // 阻止跳跃
        mc.player.setJumping(false);
    }
    
    private void initGame() {
        snake = new ArrayList<>();
        snake.add(new Point(GRID_WIDTH / 2, GRID_HEIGHT / 2));
        direction = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        gameOver = false;
        score = 0;
        spawnFood();
    }
    
    private void spawnFood() {
        while (true) {
            int x = random.nextInt(GRID_WIDTH);
            int y = random.nextInt(GRID_HEIGHT);
            food = new Point(x, y);
            
            boolean onSnake = false;
            for (Point segment : snake) {
                if (segment.x == x && segment.y == y) {
                    onSnake = true;
                    break;
                }
            }
            
            if (!onSnake) {
                break;
            }
        }
    }
    
    private void updateGame() {
        if (gameOver) {
            // 检查是否该重置游戏（游戏结束3秒后）
            if (System.currentTimeMillis() - gameOverTime >= 3000) {
                initGame();
            }
            return;
        }
        
        // 更新方向
        direction = nextDirection;
        
        // 移动蛇
        Point head = snake.get(0);
        Point newHead = new Point(head.x, head.y);
        
        switch (direction) {
            case UP -> newHead.y = (newHead.y - 1 + GRID_HEIGHT) % GRID_HEIGHT;
            case DOWN -> newHead.y = (newHead.y + 1) % GRID_HEIGHT;
            case LEFT -> newHead.x = (newHead.x - 1 + GRID_WIDTH) % GRID_WIDTH;
            case RIGHT -> newHead.x = (newHead.x + 1) % GRID_WIDTH;
        }
        
        // 检查自我碰撞
        for (int i = 1; i < snake.size(); i++) {
            Point segment = snake.get(i);
            if (segment.x == newHead.x && segment.y == newHead.y) {
                gameOver = true;
                gameOverTime = System.currentTimeMillis();
                return;
            }
        }
        
        // 检查食物碰撞
        if (newHead.x == food.x && newHead.y == food.y) {
            snake.add(0, newHead);
            score += 10;
            spawnFood();
        } else {
            snake.add(0, newHead);
            snake.remove(snake.size() - 1);
        }
    }
    
    @EventTarget
    public void onKey(EventKey e) {
        if (!isEnabled()) {
            return;
        }
        
        // 拦截所有WASD和方向键输入，防止玩家移动
        int key = e.getKey();
        if (key == 87 || key == 83 || key == 65 || key == 68 || // WASD
            key == 265 || key == 264 || key == 263 || key == 262) { // 方向键
            e.setCancelled(true);
            
            if (gameOver || !e.isState()) {
                return;
            }
            
            switch (key) {
                case 87, 265 -> { // W or UP
                    if (direction != Direction.DOWN) {
                        nextDirection = Direction.UP;
                    }
                }
                case 83, 264 -> { // S or DOWN
                    if (direction != Direction.UP) {
                        nextDirection = Direction.DOWN;
                    }
                }
                case 65, 263 -> { // A or LEFT
                    if (direction != Direction.RIGHT) {
                        nextDirection = Direction.LEFT;
                    }
                }
                case 68, 262 -> { // D or RIGHT
                    if (direction != Direction.LEFT) {
                        nextDirection = Direction.RIGHT;
                    }
                }
            }
        }
        
        // 拦截所有移动相关的按键，防止玩家在服务器上移动
        if (key == 32 || // 空格键 (跳跃)
            key == 340 || key == 344 || // Shift键
            key == 341 || key == 345 || // Ctrl键
            key == 348) { // Alt键
            e.setCancelled(true);
        }
    }
    
    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (!isEnabled()) {
            return;
        }
        
        // 完全阻止所有移动输入
        event.setForward(0.0F);
        event.setStrafe(0.0F);
        event.setJump(false);
        event.setSneak(false);
    }
    
    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (!isEnabled()) {
            return;
        }
        
        // 持续阻止移动
        forceStopMovement();
        
        GuiGraphics guiGraphics = e.getGuiGraphics();
        PoseStack poseStack = e.getStack();
        
        // 每100ms更新一次游戏逻辑
        if (gameTimer.delay(100)) {
            updateGame();
            gameTimer.reset();
        }
        
        // 计算位置
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int gameWidth = GRID_WIDTH * CELL_SIZE + BORDER * 2;
        int gameHeight = GRID_HEIGHT * CELL_SIZE + BORDER * 2;
        int x = (screenWidth - gameWidth) / 2;
        int y = (screenHeight - gameHeight) / 2;
        
        // 绘制背景
        guiGraphics.fill(x, y, x + gameWidth, y + gameHeight, 0x80000000);
        
        // 绘制边框
        guiGraphics.fill(x, y, x + gameWidth, y + BORDER, 0xFFFFFFFF);
        guiGraphics.fill(x, y + gameHeight - BORDER, x + gameWidth, y + gameHeight, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + BORDER, y + gameHeight, 0xFFFFFFFF);
        guiGraphics.fill(x + gameWidth - BORDER, y, x + gameWidth, y + gameHeight, 0xFFFFFFFF);
        
        // 绘制蛇
        for (Point segment : snake) {
            int segmentX = x + BORDER + segment.x * CELL_SIZE;
            int segmentY = y + BORDER + segment.y * CELL_SIZE;
            guiGraphics.fill(segmentX, segmentY, segmentX + CELL_SIZE, segmentY + CELL_SIZE, 0xFF00FF00);
        }
        
        // 绘制食物
        if (food != null) {
            int foodX = x + BORDER + food.x * CELL_SIZE;
            int foodY = y + BORDER + food.y * CELL_SIZE;
            guiGraphics.fill(foodX, foodY, foodX + CELL_SIZE, foodY + CELL_SIZE, 0xFFFFFF00);
        }
        
        // 绘制分数
        String scoreText = "Score: " + score;
        guiGraphics.drawString(mc.font, scoreText, x + BORDER, y - 15, 0xFFFFFFFF);
        
        // 在屏幕中央绘制游戏结束消息
        if (gameOver) {
            String gameOverText = "Game Over!";
            int textWidth = mc.font.width(gameOverText);
            int centerX = screenWidth / 2 - textWidth / 2;
            int centerY = screenHeight / 2 - 5;
            guiGraphics.drawString(mc.font, gameOverText, centerX, centerY, 0xFFFF0000);
            
            // 显示重启倒计时
            long timeSinceGameOver = System.currentTimeMillis() - gameOverTime;
            if (timeSinceGameOver >= 3000) {
                String restartText = "Restarting...";
                int restartWidth = mc.font.width(restartText);
                guiGraphics.drawString(mc.font, restartText, screenWidth / 2 - restartWidth / 2, centerY + 15, 0xFFFFFF00);
            } else {
                int timeLeft = 3 - (int)(timeSinceGameOver / 1000);
                String countdownText = "Restarting in " + timeLeft + "s";
                int countdownWidth = mc.font.width(countdownText);
                guiGraphics.drawString(mc.font, countdownText, screenWidth / 2 - countdownWidth / 2, centerY + 15, 0xFFFFFF00);
            }
        }
    }
    
    private static class Point {
        int x, y;
        
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}