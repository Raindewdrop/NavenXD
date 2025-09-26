package com.heypixel.heypixelmod.mixin.O.accessors;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PostChain.class})
public interface PostChainAccessor {
   @Accessor("passes")
   List<PostPass> getPasses();
}
