package com.heypixel.heypixelmod.mixin;

import com.heypixel.heypixelmod.obsoverlay.skin.SkinManager;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class MixinPlayerModel {
    
    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void onSetupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        // 这里可以添加手臂类型检测逻辑
        SkinManager skinManager = SkinManager.getInstance();
        String armType = skinManager.getArmType(player);
        
        if (armType != null) {
            // 根据手臂类型调整模型参数
            PlayerModel<?> model = (PlayerModel<?>) (Object) this;
            
            if (armType.equals("slim")) {
                // 细手臂设置
                model.leftArm.xScale = 0.375f;
                model.leftArm.yScale = 1.5f;
                model.leftArm.zScale = 0.375f;
                
                model.rightArm.xScale = 0.375f;
                model.rightArm.yScale = 1.5f;
                model.rightArm.zScale = 0.375f;
            } else if (armType.equals("classic")) {
                // 粗手臂设置
                model.leftArm.xScale = 0.5f;
                model.leftArm.yScale = 1.5f;
                model.leftArm.zScale = 0.5f;
                
                model.rightArm.xScale = 0.5f;
                model.rightArm.yScale = 1.5f;
                model.rightArm.zScale = 0.5f;
            }
        }
    }
}