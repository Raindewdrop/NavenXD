package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.KillAura;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.item.AxeItem;

@ModuleInfo(name = "Animations", description = "Customizes item animations and block animations", category = Category.RENDER)
public class Animations extends Module {
    public final ModeValue BlockMods = ValueBuilder.create(this, "Block Mods")
            .setModes("None", "1.7", "push", "XinXin")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();

    public final BooleanValue BlockOnlySword = ValueBuilder.create(this, "Block Only Sword")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue KillauraAutoBlock = ValueBuilder.create(this, "Killaura Auto Block")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue OverrideVanilla = ValueBuilder.create(this, "Override Vanilla")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue ShowHUDItem = ValueBuilder.create(this, "Show HUD Item")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue RenderOffhandShield = ValueBuilder.create(this, "Render Offhand Shield")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue EnableAxeBlockAnimation = ValueBuilder.create(this, "Enable Axe Block Animation")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue CustomSwingSpeed = ValueBuilder.create(this, "Custom Swing Speed")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final FloatValue SwingSpeed = ValueBuilder.create(this, "Swing Speed")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(3.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> CustomSwingSpeed.getCurrentValue())
            .build()
            .getFloatValue();

    private boolean flip;
    public static boolean isBlocking = false;
    private final Minecraft mc = Minecraft.getInstance();
    private float mainHandHeight = 0.0F;
    private float offHandHeight = 0.0F;
    private float oMainHandHeight = 0.0F;
    private float oOffHandHeight = 0.0F;
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (!this.isEnabled() || !OverrideVanilla.getCurrentValue() || BlockMods.getCurrentMode().equals("None"))
            return;

        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getItemStack().getItem() instanceof SwordItem || (event.getItemStack().getItem() instanceof AxeItem && EnableAxeBlockAnimation.getCurrentValue())))
            return;

        // 检查副手是否在使用物品（非盾牌）
        boolean isOffhandUsing = false;
        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = mc.player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            // 排除盾牌的使用（盾牌使用动画为BLOCK）
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }


        boolean isKillauraBlocking = KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null;
        
        if (isOffhandUsing && !isKillauraBlocking)
            return;

        if (!mc.options.keyUse.isDown() && !isKillauraBlocking)
            return;

        event.setCanceled(true);

        // 根据自定义挥手速度调整swingProgress
        float swingProgress = event.getSwingProgress();
        if (CustomSwingSpeed.getCurrentValue()) {
            swingProgress = Mth.clamp(swingProgress * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
        }

        renderArmWithItem(
                mc.player,
                event.getPartialTick(),
                event.getEquipProgress(),
                event.getHand(),
                swingProgress, // 使用调整后的挥手进度
                event.getItemStack(),
                event.getEquipProgress(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof ServerboundSwingPacket) {
            flip = !flip;
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE || mc.player == null)
            return;

        updateHandStates();
    }

    private void updateHandStates() {
        oMainHandHeight = mainHandHeight;
        oOffHandHeight = offHandHeight;

        LocalPlayer localplayer = mc.player;
        ItemStack itemstack = localplayer.getMainHandItem();
        ItemStack itemstack1 = localplayer.getOffhandItem();

        // 检查是否正在格挡
        boolean isBlocking = isBlocking();

        if (isBlocking) {
            // 格挡时，根据自定义挥手速度调整主手高度变化
            float targetHeight = 1.0F;
            if (CustomSwingSpeed.getCurrentValue()) {
                // 当启用自定义挥手速度时，根据挥动进度调整高度
                float swingProgress = localplayer.getAttackAnim(1.0F);
                float modifiedSwingProgress = Mth.clamp(swingProgress * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
                
                // 使用完整的挥动周期，确保动画能够完整执行
                // 当modifiedSwingProgress接近1.0时，平滑地返回到初始状态
                if (modifiedSwingProgress > 0.9F) {
                    // 在挥动结束时平滑返回，而不是直接重置
                    float returnProgress = (modifiedSwingProgress - 0.9F) * 10.0F; // 0.0到1.0的归一化
                    targetHeight = Mth.lerp(returnProgress, 0.0F, 1.0F);
                } else {
                    // 正常挥动阶段
                    targetHeight = 1.0F - Math.abs(Mth.sin(modifiedSwingProgress * (float) Math.PI));
                }
            } else {
                // 默认情况下保持完全抬起状态
                targetHeight = 1.0F;
            }
            
            // 平滑过渡到目标高度，使用更小的步长以获得更平滑的动画
            mainHandHeight += Mth.clamp(targetHeight - mainHandHeight, -0.1F, 0.1F);
            
            // 更新物品栈，避免重新装备动画
            if (ItemStack.matches(mainHandItem, itemstack)) {
                mainHandItem = itemstack;
            }
            if (ItemStack.matches(offHandItem, itemstack1)) {
                offHandItem = itemstack1;
            }
            return; // 跳过后续的攻击冷却计算
        }

        if (localplayer.isHandsBusy()) {
            mainHandHeight = Mth.clamp(mainHandHeight - 0.4F, 0.0F, 1.0F);
            offHandHeight = Mth.clamp(offHandHeight - 0.4F, 0.0F, 1.0F);
        } else {
            float f = localplayer.getAttackStrengthScale(1.0F);
            boolean flag = ForgeHooksClient.shouldCauseReequipAnimation(mainHandItem, itemstack,
                    localplayer.getInventory().selected);
            boolean flag1 = ForgeHooksClient.shouldCauseReequipAnimation(offHandItem, itemstack1, -1);

            if (!flag && mainHandItem != itemstack) {
                mainHandItem = itemstack;
            }

            if (!flag1 && offHandItem != itemstack1) {
                offHandItem = itemstack1;
            }

            // 修复攻击冷却高度计算，使其更平滑
            float targetMainHeight = !flag ? f * f * f : 0.0F;
            float targetOffHeight = !flag1 ? 1.0F : 0.0F;
            
            // 添加动画状态检查，避免新动画直接覆盖未完成的动画
            if (targetMainHeight > mainHandHeight || mainHandHeight < 0.01F) {
                // 只有当目标高度更高或当前高度接近0时才更新，避免动画中断
                mainHandHeight += Mth.clamp(targetMainHeight - mainHandHeight, -0.2F, 0.2F);
            }
            
            if (targetOffHeight > offHandHeight || offHandHeight < 0.01F) {
                offHandHeight += Mth.clamp(targetOffHeight - offHandHeight, -0.2F, 0.2F);
            }
        }

        if (mainHandHeight < 0.1F) {
            mainHandItem = itemstack;
        }

        if (offHandHeight < 0.1F) {
            offHandItem = itemstack1;
        }
    }

    // 添加一个辅助方法来判断是否正在格挡
    private boolean isBlocking() {
        if (!this.isEnabled() || BlockMods.getCurrentMode().equals("None"))
            return false;

        LocalPlayer player = mc.player;
        if (player == null)
            return false;

        ItemStack mainHandItem = player.getMainHandItem();
        if (BlockOnlySword.getCurrentValue() && !(mainHandItem.getItem() instanceof SwordItem || (mainHandItem.getItem() instanceof AxeItem && EnableAxeBlockAnimation.getCurrentValue())))
            return false;

        // 检查副手是否在使用物品（非盾牌）
        boolean isOffhandUsing = false;
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            // 排除盾牌的使用（盾牌使用动画为BLOCK）
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }

        // 如果是Killaura触发的自动格挡，则忽略副手使用状态
        boolean isKillauraBlocking = KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null && !isPvP19ModeEnabled();
        if (isKillauraBlocking) {
            return true;
        }

        // 如果副手正在使用物品（非盾牌），则不触发格挡
        if (isOffhandUsing) {
            return false;
        }

        return mc.options.keyUse.isDown();
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (mc.player == null || mc.level == null)
            return;

        if (ShowHUDItem.getCurrentValue()) {
            renderHUDItem(event);
        }
    }

    private void renderHUDItem(EventRender event) {
        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.isEmpty())
            return;

        PoseStack poseStack = new PoseStack();
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();
        float partialTicks = mc.getFrameTime();
        int packedLight = 15728880;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float itemX = screenWidth - 100;
        float itemY = screenHeight - 100;

        poseStack.translate(itemX, itemY, 0);

        float swingProgress = mc.player.getAttackAnim(partialTicks);
        if (swingProgress > 0) {
            float swingAngle = Mth.sin(swingProgress * swingProgress * (float) Math.PI) * 10.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(swingAngle));
        }

        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);

        renderItem(mc.player, mainHandItem, ItemDisplayContext.GUI, false, poseStack, bufferSource, packedLight);
    }

    private void renderArmWithItem(
            AbstractClientPlayer player,
            float partialTicks,
            float equipProgress,
            InteractionHand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equippedProg,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light) {
        if (!player.isScoping()) {
            // 根据自定义挥手速度调整swingProgress
            float modifiedSwingProgress = swingProgress;
            if (CustomSwingSpeed.getCurrentValue()) {
                modifiedSwingProgress = Mth.clamp(swingProgress * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
            }
            
            boolean flag = interactionHand == InteractionHand.MAIN_HAND;
            HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
            Animations animations = this;
            poseStack.pushPose();
            
            // 检查是否需要跳过副手盾牌渲染
            boolean skipOffhandShield = !flag && 
                                       player.getOffhandItem().getItem() instanceof ShieldItem && 
                                       !RenderOffhandShield.getCurrentValue();
            
            if (!skipOffhandShield) {
                if (itemStack.isEmpty()) {
                    if (flag && !player.isInvisible()) {
                        renderPlayerArm(poseStack, multiBufferSource, light, equippedProg, swingProgress, humanoidarm);
                    }
                } else if (itemStack.is(Items.FILLED_MAP)) {
                    if (flag && offHandItem.isEmpty()) {
                        renderTwoHandedMap(poseStack, multiBufferSource, light, equipProgress, equippedProg,
                                swingProgress);
                    } else {
                        renderOneHandedMap(poseStack, multiBufferSource, light, equippedProg, humanoidarm,
                                swingProgress, itemStack);
                    }
                } else {
                    boolean flag1 = itemStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack);
                    int i = humanoidarm == HumanoidArm.RIGHT ? 1 : -1;
                    if (itemStack.is(Items.CROSSBOW)) {
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4785682F), -0.094387F, 0.0573153F);
                            poseStack.mulPose(Axis.XP.rotation(-11.935F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.3F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                            float f6 = (float) itemStack.getUseDuration()
                                    - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                            float f10 = f6 / (float) CrossbowItem.getChargeDuration(itemStack);
                            f10 = Math.min(f10, 1.0F);
                            if (f10 > 0.1F) {
                                float f14 = Mth.sin((f6 - 0.1F) * 1.3F);
                                float f20 = f10 - 0.1F;
                                float f25 = f14 * f20;
                                poseStack.translate((double) (f25 * 0.0F), (double) (f25 * 0.004F),
                                        (double) (f25 * 0.0F));
                            }

                            poseStack.translate((double) (f10 * 0.0F), (double) (f10 * 0.0F), (double) (f10 * 0.04F));
                            poseStack.scale(1.0F, 1.0F, 1.0F + f10 * 0.2F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                        } else {
                            float f5 = -0.4F * Mth.sin(Mth.sqrt(modifiedSwingProgress) * (float) Math.PI);
                            float f9 = 0.2F * Mth.sin(Mth.sqrt(modifiedSwingProgress) * (float) (Math.PI * 2));
                            float f13 = -0.2F * Mth.sin(modifiedSwingProgress * (float) Math.PI);
                            poseStack.translate((double) ((float) i * f5), (double) f9, (double) f13);
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            applyItemArmAttackTransform(poseStack, humanoidarm, modifiedSwingProgress);
                            if (flag1 && modifiedSwingProgress < 0.001F && flag) {
                                poseStack.translate((double) ((float) i * -0.641864F), 0.0, 0.0);
                                poseStack.mulPose(Axis.YP.rotation((float) i * 10.0F * (float) Math.PI / 180.0F));
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                i == 1 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                i != 1,
                                poseStack,
                                multiBufferSource,
                                light);
                    } else {
                        boolean flag2 = humanoidarm == HumanoidArm.RIGHT;
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            switch (itemStack.getUseAnimation()) {
                                case NONE:
                                case BLOCK:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case EAT:
                                case DRINK:
                                    applyEatTransform(poseStack, partialTicks, humanoidarm, itemStack);
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case BOW:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.2785682F), 0.183444F, 0.1573153F);
                                    poseStack.mulPose(Axis.XP.rotation(-13.935F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f8 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f12 = f8 / 20.0F;
                                    f12 = (f12 * f12 + f12 * 2.0F) / 3.0F;
                                    f12 = Math.min(f12, 1.0F);
                                    if (f12 > 0.1F) {
                                        float f19 = Mth.sin((f8 - 0.1F) * 1.3F);
                                        float f24 = f12 - 0.1F;
                                        float f26 = f19 * f24;
                                        poseStack.translate((double) (f26 * 0.0F), (double) (f26 * 0.004F),
                                                (double) (f26 * 0.0F));
                                    }

                                    poseStack.translate((double) (f12 * 0.0F), (double) (f12 * 0.0F),
                                            (double) (f12 * 0.04F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    break;
                                case SPEAR:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.5F), 0.7F, 0.1F);
                                    poseStack.mulPose(Axis.XP.rotation(-55.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f7 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f11 = f7 / 10.0F;
                                    f11 = Math.min(f11, 1.0F);
                                    if (f11 > 0.1F) {
                                        float f18 = Mth.sin((f7 - 0.1F) * 1.3F);
                                        float f23 = f11 - 0.1F;
                                        float f4 = f18 * f23;
                                        poseStack.translate((double) (f4 * 0.0F), (double) (f4 * 0.004F),
                                                (double) (f4 * 0.0F));
                                    }

                                    poseStack.translate(0.0, 0.0, (double) (f11 * 0.2F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                            }
                        } else if ((player.isUsingItem()
                                || Minecraft.getInstance().options.keyUse.isDown()
                                || animations.KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null)
                                && (player.getMainHandItem().getItem() instanceof SwordItem || (player.getMainHandItem().getItem() instanceof AxeItem && animations.EnableAxeBlockAnimation.getCurrentValue()))
                                && animations.BlockOnlySword.getCurrentValue()
                                && !animations.BlockMods.getCurrentMode().equals("None")) {
                            String s = animations.BlockMods.getCurrentMode().toLowerCase();
                            switch (s) {
                                case "1.7":
                                    poseStack.translate((double) ((float) i * 0.56F),
                                            (double) (-0.52F), -0.72F); // 移除equippedProg的影响
                                    // 格挡动画使用modifiedSwingProgress以支持自定义挥手速度
                                    float f17 = Mth.sin(modifiedSwingProgress * modifiedSwingProgress * (float) Math.PI);
                                    float f22 = Mth.sin(Mth.sqrt(modifiedSwingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.YP
                                            .rotation((float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(
                                            Axis.ZP.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    poseStack.scale(0.9F, 0.9F, 0.9F);
                                    poseStack.translate(-0.2F, 0.126F, 0.2F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                    break;
                                case "push":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F); // 移除equippedProg的影响
                                        poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                        // 格挡动画使用modifiedSwingProgress以支持自定义挥手速度
                                        float f15 = Mth.sin(modifiedSwingProgress * modifiedSwingProgress * (float) Math.PI);
                                        float f3 = Mth.sin(Mth.sqrt(modifiedSwingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        break;
                                    case "xinxin":
                                        // 欣欣防砍动画 - 正常防砍位置但剑会旋转
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        // 基础防砍姿势
                                        // poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        // poseStack.mulPose(Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                        // poseStack.mulPose(Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                        // 添加旋转效果 - 剑会持续旋转
                                        float rotationTime = (float) (System.currentTimeMillis() % 800) / 800.0F * (float) Math.PI * 2;
                                        poseStack.mulPose(Axis.YP.rotation(rotationTime));
                                        // 添加一些摆动效果，使用modifiedSwingProgress以支持自定义挥手速度
                                        float wobble = Mth.sin(modifiedSwingProgress * (float) Math.PI * 4) * 0.1F;
                                        poseStack.mulPose(Axis.XP.rotation(wobble));
                                        poseStack.mulPose(Axis.ZP.rotation(wobble * (float) i));
                                        break;
                            }
                        } else if (player.isAutoSpinAttack()) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4F), 0.8F, 0.3F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.0F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -85.0F * (float) Math.PI / 180.0F));
                        } else {
                            // Match attack animation to digging animation
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            if ((itemStack.getItem() instanceof SwordItem || (itemStack.getItem() instanceof AxeItem && animations.EnableAxeBlockAnimation.getCurrentValue())) &&
                                    (mc.options.keyUse.isDown() || (animations.KillauraAutoBlock.getCurrentValue()
                                            && getAuraTarget() != null && getAuraTarget() instanceof LivingEntity
                                            && getTargetHudEnabled()))) {
                                // Use same transformations as blocking
                                String s = animations.BlockMods.getCurrentMode().toLowerCase();
                                switch (s) {
                                    case "1.7":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F); // 移除equippedProg的影响
                                        // 攻击动画使用modifiedSwingProgress保持自定义挥手速度效果
                                        float f17 = Mth.sin(modifiedSwingProgress * modifiedSwingProgress * (float) Math.PI);
                                        float f22 = Mth.sin(Mth.sqrt(modifiedSwingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.YP.rotation(
                                                (float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                        poseStack.scale(0.9F, 0.9F, 0.9F);
                                        poseStack.translate(-0.2F, 0.126F, 0.2F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                        break;
                                    case "push":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F); // 移除equippedProg的影响
                                        poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                        // 攻击动画使用modifiedSwingProgress保持自定义挥手速度效果
                                        float f15 = Mth.sin(modifiedSwingProgress * modifiedSwingProgress * (float) Math.PI);
                                        float f3 = Mth.sin(Mth.sqrt(modifiedSwingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        break;
                                    case "xinxin":
                                        // 欣欣防砍动画 - 正常防砍位置但剑会旋转
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        // 基础防砍姿势
                                        // poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        // poseStack.mulPose(Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                        // poseStack.mulPose(Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                        // 添加旋转效果 - 剑会持续旋转
                                        float rotationTime = (float) (System.currentTimeMillis() % 800) / 800.0F * (float) Math.PI * 2;
                                        poseStack.mulPose(Axis.YP.rotation(rotationTime));
                                        // 添加一些摆动效果，使用modifiedSwingProgress以支持自定义挥手速度
                                        float wobble = Mth.sin(modifiedSwingProgress * (float) Math.PI * 4) * 0.1F;
                                        poseStack.mulPose(Axis.XP.rotation(wobble));
                                        poseStack.mulPose(Axis.ZP.rotation(wobble * (float) i));
                                        break;
                                    default:
                                        applyItemArmAttackTransform(poseStack, humanoidarm, modifiedSwingProgress);
                                }
                            } else {
                                applyItemArmAttackTransform(poseStack, humanoidarm, modifiedSwingProgress);
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                flag2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                !flag2,
                                poseStack,
                                multiBufferSource,
                                light);
                    }
                }
            }

            poseStack.popPose();
        }
    }

    private LivingEntity getAuraTarget() {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            try {
                java.lang.reflect.Field targetField = KillAura.class.getDeclaredField("target");
                targetField.setAccessible(true);
                return (LivingEntity) targetField.get(null);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private boolean getTargetHudEnabled() {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            try {
                java.lang.reflect.Field targetHudField = KillAura.class.getDeclaredField("targetHud");
                targetHudField.setAccessible(true);
                Object targetHudValue = targetHudField.get(killAura);
                if (targetHudValue != null) {
                    // 调用getCurrentValue()方法
                    java.lang.reflect.Method getCurrentValueMethod = targetHudValue.getClass().getMethod("getCurrentValue");
                    return (Boolean) getCurrentValueMethod.invoke(targetHudValue);
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private boolean isPvP19ModeEnabled() {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            try {
                java.lang.reflect.Field pvp19Field = KillAura.class.getDeclaredField("pvp19");
                pvp19Field.setAccessible(true);
                Object pvp19Value = pvp19Field.get(killAura);
                if (pvp19Value != null) {
                    // 调用getCurrentValue()方法
                    java.lang.reflect.Method getCurrentValueMethod = pvp19Value.getClass().getMethod("getCurrentValue");
                    return (Boolean) getCurrentValueMethod.invoke(pvp19Value);
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
            float swingProgress, HumanoidArm arm) {
        boolean flag = arm == HumanoidArm.RIGHT;
        float f = flag ? 1.0F : -1.0F;
        
        // 根据自定义挥手速度调整swingProgress
        float modifiedSwingProgress = swingProgress;
        if (CustomSwingSpeed.getCurrentValue()) {
            modifiedSwingProgress = Mth.clamp(swingProgress * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
        }
        
        float f1 = Mth.sqrt(modifiedSwingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(modifiedSwingProgress * (float) Math.PI);
        poseStack.translate((double) (f * (0.644764F + f2)), (double) (0.644764F + f3), (double) (0.644764F + f4));
        poseStack.mulPose(Axis.XP.rotation(-0.3F * Mth.sin(f1 * (float) (Math.PI * 2))));
        poseStack.mulPose(Axis.YP.rotation(f * 0.4F * Mth.sin(f1 * (float) Math.PI)));
        poseStack.mulPose(Axis.ZP.rotation(f * -0.4F * Mth.sin(modifiedSwingProgress * (float) Math.PI)));
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, flag ? mainHandItem : offHandItem,
                flag ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !flag,
                poseStack, bufferSource, light);
    }

    private void renderTwoHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equipProgress,
            float equippedProg, float swingProgress) {
        // 根据自定义挥手速度调整swingProgress
        float modifiedSwingProgress = swingProgress;
        if (CustomSwingSpeed.getCurrentValue()) {
            modifiedSwingProgress = Mth.clamp(swingProgress * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
        }
        
        float f = Mth.sqrt(modifiedSwingProgress);
        float f1 = -0.2F * Mth.sin(modifiedSwingProgress * (float) Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
        poseStack.translate(0.0D, (double) (-f1 / 2.0F), (double) f2);
        float f3 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f4 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, mainHandItem, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, poseStack,
                bufferSource, light);
        this.renderItem(mc.player, offHandItem, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, true, poseStack,
                bufferSource, light);
    }

    private void renderOneHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
            HumanoidArm arm, float swingProgress, ItemStack item) {
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate((double) (f * 0.125F), 0.0D, 0.0D);
        
        // 根据自定义挥手速度调整swingProgress
        if (CustomSwingSpeed.getCurrentValue()) {
            swingProgress = Mth.clamp(swingProgress * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
        }

        // 这里应该直接渲染物品，而不是调用renderArmWithItem
        // 因为renderOneHandedMap是专门用于渲染单手地图的方法
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, item,
                arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                arm != HumanoidArm.RIGHT, poseStack, bufferSource, light);
    }

    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equippedProg) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f1 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F + f * -0.6F), -0.72F);
    }

    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        
        // 根据自定义挥手速度调整swingProgress
        float modifiedSwingProgress = swingProgress;
        if (CustomSwingSpeed.getCurrentValue()) {
            // 速度值越大，动画越快，所以我们需要调整swingProgress
            // 当速度为1.0时，保持原始速度
            // 当速度大于1.0时，动画加快
            // 当速度小于1.0时，动画减慢
            modifiedSwingProgress = Mth.clamp(swingProgress * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
        }
        
        float f = Mth.sin(modifiedSwingProgress * modifiedSwingProgress * (float) Math.PI);
        float f1 = Mth.sin(Mth.sqrt(modifiedSwingProgress) * (float) Math.PI);
        
        // 修复Y轴平移计算，移除不稳定的mainHandHeight影响
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F), -0.72F);
        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
        
        // 添加更平滑的动画过渡
        float swingFactor = Mth.clamp(modifiedSwingProgress, 0.0F, 1.0F);
        poseStack.mulPose(Axis.XP.rotation(f * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation(f1 * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation(f1 * -70.0F * swingFactor * (float) Math.PI / 180.0F));
    }

    private void applyEatTransform(PoseStack poseStack, float partialTicks, HumanoidArm arm, ItemStack item) {
        float f = (float) item.getUseDuration() - ((float) mc.player.getUseItemRemainingTicks() - partialTicks + 1.0F);
        float f1 = f / (float) item.getUseDuration();
        
        // Apply custom swing speed to eating animation
        if (CustomSwingSpeed.getCurrentValue()) {
            f1 = Mth.clamp(f1 * SwingSpeed.getCurrentValue(), 0.0F, 1.0F);
        }
        
        if (f1 < 0.8F) {
            float f2 = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            poseStack.translate(0.0D, (double) f2, 0.0D);
        }
        float f3 = 1.0F - (float) Math.pow((double) (1.0F - f1), 27.0D);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate((double) (f3 * 0.6F * (float) i), (double) (f3 * -0.5F), (double) (f3 * 0.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * f3 * 90.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.XP.rotation(f3 * 10.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * f3 * 30.0F * (float) Math.PI / 180.0F));
    }

    private void renderItem(LivingEntity entity, ItemStack stack,
            ItemDisplayContext transformType, boolean leftHand,
            PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (stack.isEmpty())
            return;
        ItemRenderer itemRenderer = mc.getItemRenderer();
        itemRenderer.renderStatic(entity, stack, transformType, leftHand, poseStack, buffer, entity.level(), light, 0,
                0);
    }
}