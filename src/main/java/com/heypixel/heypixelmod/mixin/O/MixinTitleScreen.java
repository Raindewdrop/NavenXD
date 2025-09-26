package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Unique
    private static final ResourceLocation BACKGROUND_IMAGE = ResourceLocation.parse("navenxd:menu/bg.png");

    @Unique
    private List<String> menuButtonTexts;

    @Unique
    private List<Runnable> menuButtonActions;

    @Unique
    private int selectedButton = -1;

    @Unique
    private float buttonAlpha = 0.0f;

    @Unique
    private long animationStartTime = 0;

    @Unique
    private CustomTextRenderer fontRenderer;

    @Unique
    private CustomTextRenderer titleFontRenderer;

    @Unique
    private CustomTextRenderer timeFontRenderer;

    @Unique
    private static final int BUTTON_WIDTH = 100;
    @Unique
    private static final int BUTTON_HEIGHT = 20;
    @Unique
    private static final int BUTTON_SPACING = 4;
    @Unique
    private static final float TEXT_SIZE = 0.4f;

    private MixinTitleScreen() {
        super(null);
        throw new AssertionError();
    }

    @Inject(method = "m_7856_", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        menuButtonTexts = new ArrayList<>();
        menuButtonActions = new ArrayList<>();

        fontRenderer = Fonts.harmony;
        // 创建标题字体渲染器 (artenglish.ttf)
        titleFontRenderer = new CustomTextRenderer("artenglish", 48, 0, 255, 1024);
        // 创建时间字体渲染器 (math.otf)
        timeFontRenderer = new CustomTextRenderer("math", 32, 0, 255, 512);

        String languageCode = Minecraft.getInstance().getLanguageManager().getSelected();
        String[] buttonTexts;

        if (languageCode.startsWith("zh_tw") || languageCode.startsWith("zh_hk")) {
            buttonTexts = new String[]{"單人遊戲", "多人遊戲", "設定", "退出遊戲"};
        } else if (languageCode.startsWith("zh_cn") || languageCode.startsWith("zh")) {
            buttonTexts = new String[]{"单人游戏", "多人游戏", "设置", "退出游戏"};
        } else {
            buttonTexts = new String[]{"Singleplayer", "Multiplayer", "Options", "Quit Game"};
        }

        addMenuButton(buttonTexts[0], this::openSingleplayer);
        addMenuButton(buttonTexts[1], this::openMultiplayer);
        addMenuButton(buttonTexts[2], this::openSettings);
        addMenuButton(buttonTexts[3], this::quitGame);

        buttonAlpha = 0.0f;
        animationStartTime = System.currentTimeMillis();
        selectedButton = -1;

        this.clearWidgets();
        this.renderables.clear();
    }

    @Unique
    private void addMenuButton(String text, Runnable action) {
        menuButtonTexts.add(text);
        menuButtonActions.add(action);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderStart(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        renderBackground(guiGraphics);
        updateButtonAnimation();
        updateButtonHoverState(mouseX, mouseY);
        renderTitleAndTime(guiGraphics);
        renderMenuButtons(guiGraphics);
    }

    @Unique
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, new Color(20, 20, 30, 200).getRGB());
        guiGraphics.blit(BACKGROUND_IMAGE,
                0, 0,
                this.width, this.height,
                0, 0,
                1920, 1080,
                1920, 1080);
    }

    @Unique
    private void renderTitleAndTime(GuiGraphics guiGraphics) {
        int totalButtonHeight = menuButtonTexts.size() * (BUTTON_HEIGHT + BUTTON_SPACING) - BUTTON_SPACING;
        int centerY = this.height / 2;
        int startY = centerY - totalButtonHeight / 2 - 60;

        String titleText = "NavenXD";
        float titleSize = 1.0f;
        int titleColor = new Color(255, 255, 255).getRGB();
        float titleWidth = titleFontRenderer.getWidth(titleText, titleSize);
        float titleX = (this.width - titleWidth) / 2;
        float titleY = startY - 40;
        titleFontRenderer.render(guiGraphics.pose(), titleText, titleX, titleY, new Color(titleColor), true, titleSize);

        String timeText = getCurrentTime();
        float timeSize = 0.6f;
        int timeColor = new Color(220, 220, 220).getRGB();
        float timeWidth = timeFontRenderer.getWidth(timeText, timeSize);
        float timeX = (this.width - timeWidth) / 2;
        float timeY = titleY + (float) titleFontRenderer.getHeight(true, titleSize) + 10f;
        timeFontRenderer.render(guiGraphics.pose(), timeText, timeX, timeY, new Color(timeColor), true, timeSize);
    }

    @Unique
    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

    @Unique
    private void updateButtonAnimation() {
        long elapsed = System.currentTimeMillis() - animationStartTime;
        buttonAlpha = Math.min(1.0f, elapsed / 2000.0f);
    }

    @Unique
    private void updateButtonHoverState(int mouseX, int mouseY) {
        selectedButton = -1;
        int totalButtonHeight = menuButtonTexts.size() * (BUTTON_HEIGHT + BUTTON_SPACING) - BUTTON_SPACING;
        int centerY = this.height / 2;
        int startY = centerY - totalButtonHeight / 2;
        int startX = (this.width - BUTTON_WIDTH) / 2;

        for (int i = 0; i < menuButtonTexts.size(); i++) {
            int y = startY + i * (BUTTON_HEIGHT + BUTTON_SPACING);
            if (mouseX >= startX && mouseX <= startX + BUTTON_WIDTH &&
                mouseY >= y && mouseY <= y + BUTTON_HEIGHT) {
                selectedButton = i;
                break;
            }
        }
    }

    @Unique
    private void renderMenuButtons(GuiGraphics guiGraphics) {
        int totalButtonHeight = menuButtonTexts.size() * (BUTTON_HEIGHT + BUTTON_SPACING) - BUTTON_SPACING;
        int centerY = this.height / 2;
        int startY = centerY - totalButtonHeight / 2;
        int startX = (this.width - BUTTON_WIDTH) / 2;

        int baseAlpha = (int)(buttonAlpha * 255);
        int hoverAlpha = Math.min(255, baseAlpha + 100);

        for (int i = 0; i < menuButtonTexts.size(); i++) {
            String text = menuButtonTexts.get(i);
            int x = startX;
            int y = startY + i * (BUTTON_HEIGHT + BUTTON_SPACING);
            boolean isHovered = (i == selectedButton);
            renderCustomButton(guiGraphics, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, isHovered, baseAlpha);

            int textColor = isHovered
                    ? new Color(255, 255, 160, baseAlpha).getRGB()
                    : new Color(224, 224, 224, baseAlpha).getRGB();

            float textWidth = fontRenderer.getWidth(text, TEXT_SIZE);
            float textHeight = (float) fontRenderer.getHeight(true, TEXT_SIZE);
            float textX = x + (BUTTON_WIDTH - textWidth) / 2;
            float textY = y + (BUTTON_HEIGHT - textHeight) / 2;

            fontRenderer.render(guiGraphics.pose(), text,
                    textX + 0.5f, textY + 0.5f,
                    new Color(0, 0, 0, 150),
                    true, TEXT_SIZE);

            fontRenderer.render(guiGraphics.pose(), text,
                    textX, textY,
                    new Color(textColor),
                    true, TEXT_SIZE);
        }
    }

    @Unique
    private void renderCustomButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                    boolean isHovered, int alpha) {
        // 半透明黑色背景
        int bgColor = new Color(0, 0, 0, (int)(alpha * 0.7f)).getRGB();
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 悬停时的边框效果
        if (isHovered) {
            int borderColor = new Color(255, 255, 255, alpha).getRGB();
            // 上边框
            guiGraphics.fill(x, y, x + width, y + 1, borderColor);
            // 下边框
            guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
            // 左边框
            guiGraphics.fill(x, y, x + 1, y + height, borderColor);
            // 右边框
            guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selectedButton >= 0 && selectedButton < menuButtonActions.size()) {
            Runnable action = menuButtonActions.get(selectedButton);
            if (action != null) {
                action.run();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Unique
    private void openSingleplayer() {
        Minecraft.getInstance().setScreen(new SelectWorldScreen(this));
    }

    @Unique
    private void openMultiplayer() {
        Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this));
    }

    @Unique
    private void openSettings() {
        Options options = Minecraft.getInstance().options;
        Minecraft.getInstance().setScreen(new OptionsScreen(this, options));
    }

    @Unique
    private void quitGame() {
        Minecraft.getInstance().stop();
    }
}