package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.PrimedTnt;

@ModuleInfo(
        name = "TNTWarning",
        description = "Show tnts distance",
        category = Category.MISC
)
public class TNTWarning extends Module {
    public static BlockPos nearestTntPos = null;
    public static TNTWarning instance;

    public TNTWarning() {
        instance = this;
    }

    @EventTarget
    public void on2D(EventRender2D event) {
        this.onRender(event.getStack());
    }

    public void onRender(PoseStack poseStack) {
        if (mc.player != null && mc.level != null) {
            CustomTextRenderer font = Fonts.harmony;
            List<PrimedTnt> tnts = mc.level.getEntitiesOfClass(PrimedTnt.class, mc.player.getBoundingBox().inflate(10.0F));
            if (!tnts.isEmpty()) {
                double closestDist = Double.MAX_VALUE;
                PrimedTnt closestTnt = null;
                nearestTntPos = null;

                for(PrimedTnt tnt : tnts) {
                    double dist = mc.player.distanceTo(tnt);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestTnt = tnt;
                    }
                }

                if (closestTnt != null && closestDist <= 10.0F) {
                    nearestTntPos = closestTnt.blockPosition();
                    Color color = this.getGradientColor(closestDist);
                    String text = String.format("TNT Distance: %.1f", closestDist);
                    int screenWidth = mc.getWindow().getGuiScaledWidth();
                    int screenHeight = mc.getWindow().getGuiScaledHeight();
                    int progressY = screenHeight / 2 + 35;
                    float progressTextX = (float)screenWidth / 2.0F - (float)mc.font.width("TNT Distance: %.1f") / 2.0F;
                    float progressTextY = (float)(progressY + 6 + 6);
                    font.render(poseStack, text, (double)(progressTextX - 2.0F), (double)progressTextY, color, true, 0.4);
                }
            }
        }
    }

    private Color getGradientColor(double distance) {
        float ratio = (float)Mth.clamp(distance / 10.0F, 0.0F, 1.0F);
        int red = (int)(255.0F * (1.0F - ratio));
        int green = (int)(255.0F * ratio);
        return new Color(red, green, 0);
    }
}